"""Read-only Azure startup diagnostics. Never print app setting secrets."""
import json
from pathlib import Path
import re
import subprocess
import tempfile
from zipfile import ZipFile

APP = 'HireConnect-expo'


def az(*arguments, timeout=90):
    return subprocess.run(['az', *arguments, '--only-show-errors'], capture_output=True,
                          text=True, timeout=timeout)


def scrub(text, secrets=()):
    for secret in secrets:
        if len(secret) > 5:
            text = text.replace(secret, '[redacted]')
    text = re.sub(r'mongodb(?:\+srv)?://[^\s\"\x27]+', '[mongodb-uri-redacted]', text)
    text = re.sub(r'(?i)((?:password|client_secret|access_token)\s*[:=]\s*)[^\s,;]+', r'\1[redacted]', text)
    return text


def main():
    listed = az('webapp', 'list', '--query', f"[?name=='{APP}'].{{resourceGroup:resourceGroup,id:id}}", '-o', 'json')
    if listed.returncode:
        print('No se pudo consultar el App Service con esta identidad.'); return 1
    resources = json.loads(listed.stdout)
    if len(resources) != 1:
        print('No se encontro un unico servicio Linux con ese nombre.'); return 1
    group = resources[0]['resourceGroup']
    config = az('webapp', 'config', 'show', '-g', group, '-n', APP, '-o', 'json')
    if config.returncode == 0:
        data = json.loads(config.stdout)
        print(json.dumps({'app': APP, 'resourceGroup': group, 'linuxFxVersion': data.get('linuxFxVersion'),
                          'startup': data.get('appCommandLine'), 'alwaysOn': data.get('alwaysOn')}))
    settings = az('webapp', 'config', 'appsettings', 'list', '-g', group, '-n', APP, '-o', 'json')
    secret_values = []
    if settings.returncode == 0:
        data = {entry['name']: entry.get('value') or '' for entry in json.loads(settings.stdout)}
        secret_values = [value for key,value in data.items() if any(word in key.upper() for word in
                         ['MONGO', 'SECRET', 'PASSWORD', 'TOKEN', 'KEY'])]
        safe = {key: data.get(key) for key in ['ML_EMBEDDED','ML_REQUIRED','ML_PORT','PORT',
                         'SERVER_PORT','MODEL_PATH','ML_ENABLE_EXPERIMENTAL','WEBSITE_RUN_FROM_PACKAGE']}
        safe['MongoDBConfigured'] = bool(data.get('MONGODB_URI') or data.get('SPRING_DATA_MONGODB_URI'))
        print(json.dumps(safe))
    startup = az('webapp', 'log', 'startup', 'show', '-g', group, '-n', APP, '-o', 'json')
    if startup.returncode == 0 and startup.stdout.strip():
        try:
            text = json.loads(startup.stdout)
            if not isinstance(text, str): text = json.dumps(text, ensure_ascii=False)
        except ValueError:
            text = startup.stdout
        print(scrub('\n'.join(text.splitlines()[-180:]), secret_values))
        return 0
    print('Consultando archivos de arranque del servicio...')
    with tempfile.TemporaryDirectory() as directory:
        path = Path(directory) / 'logs.zip'
        result = az('webapp', 'log', 'download', '-g', group, '-n', APP, '--log-file', str(path), timeout=120)
        if result.returncode or not path.is_file():
            print('No se pudieron obtener logs de arranque con esta identidad.'); return 1
        with ZipFile(path) as archive:
            names = sorted(n for n in archive.namelist() if any(word in n.lower() for word in ['docker.log','startup','default_docker']))[-4:]
            for name in names:
                print('Log:',name)
                text = archive.read(name).decode('utf-8', errors='replace')
                print(scrub('\n'.join(text.splitlines()[-120:]), secret_values))
    return 0

if __name__ == '__main__':
    raise SystemExit(main())
