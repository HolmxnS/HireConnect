"""Read-only, aggregate audit of model behaviour. No identifiers or DB writes.

Run from the repository: python ml/diagnose.py [--database]
Counterfactual requests are diagnostic fixtures, never training observations.
"""
import argparse
from collections import Counter
from datetime import date
import json
import os
from pathlib import Path
import re

import certifi
import joblib
import pandas as pd
from pymongo import MongoClient
from pymongo.errors import PyMongoError

from app import create_app
from features import extract
from manage import load_environment
from train import audit_database, load_rows


def diagnose(path):
    bundle = joblib.load(path)
    client = create_app(bundle).test_client()
    scenarios, raw_levels, shown = [], Counter(), Counter()
    for mode in ['Presencial', 'Remoto', 'Híbrido']:
        for skills in [[], ['Java'], ['Java', 'Python']]:
            payload = {'schemaVersion': 2, 'fechaReferencia': date.today().isoformat(),
                       'habilidades': skills, 'habilidadesRequeridas': 'Java, Python',
                       'experiencia': [], 'educacionRegistrada': False,
                       'experienciaRequerida': 'DE_2_A_3_ANIOS',
                       'modalidad': mode, 'tipoContrato': 'INDEFINIDO'}
            raw = float(bundle['model'].predict_proba(pd.DataFrame([extract(payload)]))[
                0, list(bundle['model'].classes_).index(1)])
            response = client.post('/predict', json=payload)
            result = response.json
            raw_levels['Alta' if raw >= .7 else 'Media' if raw >= .45 else 'Baja'] += 1
            shown[result.get('tipoResultado', 'error')] += 1
            scenarios.append({'modalidad': mode, 'habilidadesCoincidentes': len(skills),
                              'probabilidad_cruda_no_validada': round(raw * 100, 1),
                              'status': response.status_code, 'respuesta': result})
    return {'modelVersion': bundle['metadata'].get('modelVersion'), 'synthetic': True,
            'usedForTraining': False, 'raw_levels': dict(raw_levels),
            'displayed_results': dict(shown), 'scenarios': scenarios}


def database_audit():
    uri = os.environ.get('MONGODB_URI')
    if not uri:
        raise ValueError('Falta MONGODB_URI')
    tls = ((uri.startswith('mongodb+srv://') and not re.search(r'[?&](?:tls|ssl)=false', uri, re.I))
           or re.search(r'[?&](?:tls|ssl)=true', uri, re.I))
    with MongoClient(uri, serverSelectionTimeoutMS=10000,
                     **({'tlsCAFile': os.environ.get('MONGODB_TLS_CA_FILE', certifi.where())} if tls else {})) as client:
        db = client[os.environ.get('MONGODB_DATABASE', 'hireconnect')]
        rows, _ = load_rows(db, allow_current=True)
        return {'captured': audit_database(db), 'legacy': audit_database(db, allow_current=True),
                'outcomes_by_modality': {mode: dict(Counter(
                    'ACEPTADA' if r['y'] else 'RECHAZADA' for r in rows if r['features']['modalidad'] == mode))
                    for mode in sorted({r['features']['modalidad'] for r in rows})}}


def main():
    load_environment()
    parser = argparse.ArgumentParser()
    parser.add_argument('--model', default=os.environ.get('MODEL_PATH', str(Path(__file__).parent / 'artifacts/model.joblib')))
    parser.add_argument('--database', action='store_true', help='Consultar solo estadísticas agregadas, sin modificar registros')
    args = parser.parse_args()
    report = diagnose(args.model)
    if args.database:
        report['database'] = database_audit()
    print(json.dumps(report, ensure_ascii=False, indent=2))

if __name__ == '__main__':
    try:
        main()
    except PyMongoError as error:
        raise SystemExit('No se pudo consultar MongoDB: ' + type(error).__name__)
