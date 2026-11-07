"""Build a self-contained Linux ZIP. Never package the repository or its .env."""
import argparse
import hashlib
import json
from pathlib import Path
import shutil
import tempfile
from zipfile import ZipFile, ZIP_DEFLATED, is_zipfile


def package(root, runtime, jar, output):
    root, runtime, jar, output = map(lambda p: Path(p).resolve(), (root, runtime, jar, output))
    if not (runtime / 'bin/python3').is_file():
        raise ValueError('El runtime debe incluir bin/python3')
    if not jar.is_file() or not is_zipfile(jar):
        raise ValueError('Se requiere un JAR valido')
    for path in runtime.rglob('*'):
        if path.is_symlink() and not path.resolve().is_relative_to(runtime):
            raise ValueError('El runtime tiene enlaces fuera de su directorio')
    output.parent.mkdir(parents=True, exist_ok=True)
    with tempfile.TemporaryDirectory() as temporary:
        stage = Path(temporary)
        shutil.copy2(jar, stage / 'app.jar')
        (stage / 'deploy').mkdir()
        for name in ['start.py', 'azure-start.sh']:
            shutil.copy2(root / 'deploy' / name, stage / 'deploy' / name)
        (stage / 'ml').mkdir()
        for source in (root / 'ml').iterdir():
            if source.is_file() and (source.suffix == '.py' or source.name in {'requirements.txt', 'requirements.lock.txt'}):
                shutil.copy2(source, stage / 'ml' / source.name)
        shutil.copytree(runtime, stage / '.python', symlinks=False,
                        ignore=shutil.ignore_patterns('__pycache__', '*.pyc', '.env', '.env.*'))
        with ZipFile(output, 'w', ZIP_DEFLATED, compresslevel=3, strict_timestamps=False) as archive:
            for path in sorted(stage.rglob('*')):
                if path.is_file():
                    archive.write(path, path.relative_to(stage).as_posix())
        verify_package(output)
    with output.open('rb') as stream:
        digest = hashlib.file_digest(stream, 'sha256').hexdigest()
    return {'file': str(output), 'bytes': output.stat().st_size, 'sha256': digest}


def verify_package(path):
    with ZipFile(path) as archive:
        names = set(archive.namelist())
        required = {'app.jar', 'deploy/start.py', 'deploy/azure-start.sh',
                    'ml/app.py', 'ml/train.py', 'ml/requirements.lock.txt', '.python/bin/python3'}
        if not required.issubset(names):
            raise ValueError('El ZIP no incluye Java, Python y su arranque completo')
        for name in names:
            parts = Path(name).parts
            if name.startswith('/') or '..' in parts or any(
                    p in {'.git', '.venv', '__pycache__'} or p == '.env' or p.startswith('.env.') for p in parts):
                raise ValueError('El paquete contiene archivos excluidos o una ruta no segura')
        if archive.testzip() is not None:
            raise ValueError('El ZIP esta danado')


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--runtime', required=True)
    parser.add_argument('--jar')
    parser.add_argument('--output', default='target/azure-package.zip')
    args = parser.parse_args()
    root = Path(__file__).resolve().parents[1]
    jars = list((root / 'target').glob('*.jar'))
    if args.jar is None and len(jars) != 1:
        parser.error('Indica --jar o genera un unico JAR con Maven')
    result = package(root, args.runtime, args.jar or jars[0], args.output)
    print(json.dumps(result))

if __name__ == '__main__':
    main()
