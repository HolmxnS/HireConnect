"""Run the internal predictor and Java web server in the same container."""
import os
from pathlib import Path
import signal
import subprocess
import sys
import time
import json
from urllib.error import URLError
from urllib.request import urlopen


def configuration(environment=None, root=None):
    env = dict(os.environ if environment is None else environment)
    root = Path(root or Path(__file__).resolve().parents[1])
    embedded = env.get('ML_EMBEDDED', 'true').lower() == 'true'
    port = int(env.get('ML_PORT', '5050'))
    if not 1 <= port <= 65535:
        raise ValueError('ML_PORT debe estar entre 1 y 65535')
    if embedded and port == int(env.get('PORT', '8080')):
        raise ValueError('ML_PORT debe ser distinto del puerto publico PORT')
    env['MONGODB_URI'] = env.get('MONGODB_URI') or env.get('SPRING_DATA_MONGODB_URI', '')
    env['MONGODB_DATABASE'] = env.get('MONGODB_DATABASE') or env.get('SPRING_DATA_MONGODB_DATABASE') or 'hireconnect'
    env.setdefault('MODEL_PATH', str(root / 'ml/artifacts/model.joblib'))
    env.setdefault('ML_ENABLE_EXPERIMENTAL', 'true')
    if embedded:
        # Embedded mode always uses the API included in this deployment, even if
        # an obsolete URL for the previous rules-based service remains in Render.
        env['PYTHON_ML_URL'] = f'http://127.0.0.1:{port}/predict'
    return root, env, embedded, port


def prepare_model(root, env):
    if Path(env['MODEL_PATH']).is_file():
        return
    if not env.get('MONGODB_URI'):
        raise RuntimeError('Falta MONGODB_URI o SPRING_DATA_MONGODB_URI para preparar el modelo')
    print('Preparando regresion logistica con las postulaciones de MongoDB...', flush=True)
    subprocess.run([sys.executable, str(root / 'ml/manage.py'), 'train'],
                   env=env, check=True, timeout=180)


def wait_for_predictor(process, port, timeout=45, require_model=True):
    deadline = time.monotonic() + timeout
    while time.monotonic() < deadline:
        if process.poll() is not None:
            raise RuntimeError('El servicio predictivo termino durante el arranque')
        try:
            with urlopen(f'http://127.0.0.1:{port}/health', timeout=2) as response:
                status = json.load(response)
            if status.get('schemaVersion') == 2 and (status.get('modelReady') or not require_model):
                return
        except (URLError, OSError, ValueError):
            pass
        time.sleep(.25)
    raise RuntimeError('El modelo no quedo listo dentro del tiempo de arranque')


def stop(processes):
    for process in reversed(processes):
        if process.poll() is None:
            try:
                os.killpg(process.pid, signal.SIGTERM)
            except ProcessLookupError:
                pass
    deadline = time.monotonic() + 10
    for process in processes:
        try:
            process.wait(timeout=max(.1, deadline - time.monotonic()))
        except subprocess.TimeoutExpired:
            try:
                os.killpg(process.pid, signal.SIGKILL)
            except ProcessLookupError:
                pass
            process.wait()


def start_services(root, env, port, java_command):
    processes = []
    try:
        predictor = subprocess.Popen([
            sys.executable, '-m', 'gunicorn', '--chdir', str(root / 'ml'),
            '--bind', f'127.0.0.1:{port}', '--workers', '1', '--threads', '2',
            '--timeout', '30', '--access-logfile', '-', 'app:create_app()'],
            env=env, start_new_session=True)
        processes.append(predictor)
        wait_for_predictor(predictor, port, require_model=env.get('ML_REQUIRED', 'true').lower() == 'true')
        print(f'Modelo listo. Java usara la API interna en el puerto {port}.', flush=True)
        processes.append(subprocess.Popen(java_command, env=env, start_new_session=True))
        return processes
    except BaseException:
        stop(processes)
        raise


def main():
    root, env, embedded, port = configuration()
    java = ['java', '-jar', str(root / 'app.jar')]
    if not embedded:
        os.execvpe(java[0], java, env)
    try:
        prepare_model(root, env)
    except (RuntimeError, subprocess.SubprocessError) as error:
        if env.get('ML_REQUIRED', 'true').lower() == 'true':
            raise
        print(f'Modelo aun no preparado; iniciando para recolectar datos: {type(error).__name__}', flush=True)
    processes = []
    def interrupted(_signum, _frame):
        raise SystemExit(0)
    signal.signal(signal.SIGTERM, interrupted)
    signal.signal(signal.SIGINT, interrupted)
    try:
        processes = start_services(root, env, port, java)
        while all(process.poll() is None for process in processes):
            time.sleep(.5)
        # Any child unexpectedly stopping must restart the container as a whole.
        raise SystemExit(next((p.returncode or 1) for p in processes if p.poll() is not None))
    finally:
        stop(processes)

if __name__ == '__main__':
    try:
        main()
    except (RuntimeError, ValueError, subprocess.SubprocessError) as error:
        print(f'No se pudo iniciar HireConnect: {error}', file=sys.stderr, flush=True)
        raise SystemExit(1)
