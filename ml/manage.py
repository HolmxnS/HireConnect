"""Local launcher: loads project configuration and trains only when requested/missing."""
import argparse
import os
from pathlib import Path
import subprocess
import sys

BASE = Path(__file__).resolve().parent


def load_environment():
    # Read values as text, never execute shell expressions from .env.
    for path in [BASE / '.env', BASE.parent / '.env']:
        if not path.is_file():
            continue
        for line in path.read_text().splitlines():
            line = line.strip()
            if not line or line.startswith('#') or '=' not in line:
                continue
            name, value = line.split('=', 1)
            name = name.strip()
            if name in {'MONGODB_URI', 'MONGODB_DATABASE', 'MONGODB_TLS_CA_FILE', 'MODEL_PATH', 'PORT', 'ML_ENABLE_EXPERIMENTAL'}:
                os.environ.setdefault(name, value.strip().strip('\"\x27'))


def main():
    load_environment()
    parser = argparse.ArgumentParser()
    parser.add_argument('command', choices=['serve', 'train', 'audit', 'verify'])
    parser.add_argument('--port', type=int, default=int(os.environ.get('PORT', '5050')))
    parser.add_argument('--captured-only', action='store_true',
                        help='Entrenar solamente con perfiles capturados y decisiones fechadas')
    args = parser.parse_args()
    if args.command == 'verify':
        subprocess.run([sys.executable, str(BASE / 'robustness.py')], check=True)
        return
    if args.command == 'audit':
        subprocess.run([sys.executable, str(BASE / 'train.py'), '--audit-only'], check=True)
        return
    artifact = Path(os.environ.get('MODEL_PATH', str(BASE / 'artifacts/model.joblib'))).resolve()
    os.environ['MODEL_PATH'] = str(artifact)
    if args.command == 'train' or not artifact.is_file():
        # Current DB is explicitly authorized for the functional experimental model.
        command = [sys.executable, str(BASE / 'train.py'), '--output', str(artifact.parent)]
        if not args.captured_only:
            command.append('--allow-current-profiles')
        subprocess.run(command, check=True)
        if artifact.name != 'model.joblib':
            (artifact.parent / 'model.joblib').replace(artifact)
    if args.command == 'serve':
        from app import create_app
        from werkzeug.serving import run_simple
        run_simple('127.0.0.1', args.port, create_app(), threaded=True, use_reloader=False)

if __name__ == '__main__':
    try:
        main()
    except subprocess.CalledProcessError as error:
        raise SystemExit(error.returncode)
