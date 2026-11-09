"""Explicit synthetic demo dataset, isolated from application collections.

Labels are sampled from a declared simulation, not hiring decisions. This can
exercise training/inference but cannot establish real predictive accuracy.
"""
import argparse
from collections import Counter
from datetime import date, datetime, timedelta, timezone
import hashlib
import json
import math
import os
from pathlib import Path
import random
import re
import uuid

import certifi
import joblib
from pymongo import MongoClient, ReplaceOne
from pymongo.errors import PyMongoError

from features import extract
from manage import load_environment
from train import fit

VERSION = 'hireconnect-demo-v1'
SEED = 20261107
DOMAINS = {
    'Tecnología': ['Java', 'Python', 'SQL', 'Spring Boot', 'Git', 'MongoDB'],
    'Finanzas': ['Excel', 'Contabilidad', 'Tesorería', 'Presupuestos', 'SAP', 'Análisis financiero'],
    'Salud': ['Enfermería', 'Primeros auxilios', 'Atención al paciente', 'Bioseguridad', 'Farmacología', 'Registros clínicos'],
    'Construcción': ['AutoCAD', 'Presupuestos de obra', 'Topografía', 'Seguridad industrial', 'Supervisión de obra', 'Revit'],
    'Administración': ['Excel', 'Servicio al cliente', 'Logística', 'Gestión documental', 'Comunicación', 'Ventas'],
}
MARKER = {'synthetic': True, 'dataOrigin': 'SIMULACION', 'datasetVersion': VERSION}


def generate(people_count=1200, vacancy_count=120, seed=SEED):
    if people_count < 200 or vacancy_count < 20:
        raise ValueError('Se requieren al menos 200 postulantes y 20 vacantes para la demostración')
    rng = random.Random(seed)
    people, vacancies, applications, rows = [], [], [], []
    domains = list(DOMAINS)
    for i in range(vacancy_count):
        domain = domains[i % len(domains)]
        required = rng.sample(DOMAINS[domain], rng.randint(2, 5))
        vacancies.append(dict(MARKER, _id=f'{VERSION}-v-{i}', titulo=f'Vacante simulada de {domain} {i+1}',
            sector=domain, habilidadesRequeridas=', '.join(required),
            experienciaRequerida=rng.choice(['SIN_EXPERIENCIA', 'DE_1_A_2_ANIOS', 'DE_2_A_3_ANIOS', 'MAS_DE_3_ANIOS']),
            modalidad=rng.choice(['Presencial', 'Remoto', 'Híbrido']),
            tipoContrato=rng.choice(['INDEFINIDO', 'TIEMPO_COMPLETO', 'TIEMPO_PARCIAL']), estado='DEMO'))
    for i in range(people_count):
        domain = domains[i % len(domains)]
        reference = date(2024, 1, 1) + timedelta(days=i * 600 // people_count)
        years = rng.choice([0, 0, .5, 1, 2, 3, 5, 8, 12, 20, 30, 40])
        education = rng.random() < .65
        skills = rng.sample(DOMAINS[domain], rng.randint(0, len(DOMAINS[domain])))
        experience = [] if not years else [{'fechaInicio': (reference-timedelta(days=round(years*365.25))).isoformat()}]
        person = dict(MARKER, _id=f'{VERSION}-p-{i}', nombre='Candidato simulado', apellido=str(i+1),
            email=f'candidato-{i+1}@example.invalid', loginDisabled=True, habilidades=skills,
            educacion=[{'titulo': f'Estudios simulados en {domain}'}] if education else [], experiencia=experience)
        people.append(person)
        same = [v for v in vacancies if v['sector'] == domain]
        selected = rng.sample(same, 2)
        selected.append(rng.choice([v for v in vacancies if v not in selected]))
        for j, vacancy in enumerate(selected):
            # No candidate appears on both sides of a temporal split except boundary
            # dates, which the shared trainer already excludes by candidate group.
            payload = {'schemaVersion': 2, 'fechaReferencia': reference.isoformat(),
                'habilidades': skills, 'experiencia': experience, 'educacionRegistrada': education,
                **{k: vacancy[k] for k in ['habilidadesRequeridas', 'experienciaRequerida', 'modalidad', 'tipoContrato']}}
            features = extract(payload)
            minimum = features['experienciaRequeridaAnos']
            fit_experience = min(years / minimum, 1) if minimum else 1
            shortfall = max(0, minimum-years)
            # Declared generative scenario. Modality and contract do not influence
            # acceptance, so hybrid vacancies cannot become automatic acceptances.
            logit = -2.0 + 3.8*features['coincidenciaHabilidades']/100 + .45*education + .7*fit_experience - .65*shortfall
            probability = 1 / (1 + math.exp(-logit))
            accepted = int(rng.random() < probability)
            captured = datetime.combine(reference, datetime.min.time(), tzinfo=timezone.utc)
            application = dict(MARKER, _id=f'{VERSION}-a-{i}-{j}', postulanteId=person['_id'],
                vacanteId=vacancy['_id'], estado='ACEPTADA' if accepted else 'RECHAZADA',
                prediccionOrigen='SIMULACION', prediccionPerfil=payload, fechaPostulacion=captured,
                prediccionCapturadaEn=captured, resultadoActualizadoEn=captured+timedelta(days=1))
            applications.append(application)
            rows.append({'features': features, 'y': accepted, 'group': person['_id'],
                         'date': reference, 'resolved_date': reference+timedelta(days=1), 'source': 'simulado'})
    return people, vacancies, applications, rows


def demo_database_name(base):
    if not re.fullmatch(r'[A-Za-z0-9_-]+', base):
        raise ValueError('Nombre de base inválido')
    return base + '_ml_demo'


def persist(db, people, vacancies, applications):
    if not db.name.endswith('_ml_demo'):
        raise ValueError('Los datos simulados solo pueden escribirse en una base _ml_demo')
    for name, docs in [('postulantes', people), ('vacantes', vacancies), ('postulaciones', applications)]:
        collection = db[name]
        # Repeated runs are idempotent. Never update documents owned by another dataset.
        collection.bulk_write([ReplaceOne({'_id': d['_id'], 'datasetVersion': VERSION, 'synthetic': True}, d, upsert=True)
                               for d in docs], ordered=False)
    return {name: db[name].count_documents({'datasetVersion': VERSION, 'synthetic': True})
            for name in ['postulantes', 'vacantes', 'postulaciones']}


def train_demo(rows, output):
    output = Path(output)
    if output.resolve() == (Path(__file__).parent / 'artifacts').resolve():
        raise ValueError('El modelo demo requiere un directorio distinto del modelo real')
    model, metrics = fit(rows)
    metadata = {'schemaVersion': 2, 'algorithm': 'logistic_regression', 'experimental': True,
        'synthetic': True, 'trainingMode': 'simulacion', 'modelVersion': 'demo-' + uuid.uuid4().hex[:12],
        'trainedAt': datetime.now(timezone.utc).isoformat(), 'fuentes': {'simulado': len(rows)},
        'datasetVersion': VERSION, 'seed': SEED, 'metrics': metrics,
        'dataFingerprint': hashlib.sha256(json.dumps(rows, sort_keys=True, default=str).encode()).hexdigest(),
        'limitations': ['Etiquetas simuladas; las métricas no demuestran precisión en contrataciones reales.']}
    output.mkdir(parents=True, exist_ok=True)
    temp = output / 'model.tmp'
    joblib.dump({'model': model, 'metadata': metadata}, temp)
    temp.replace(output / 'model.joblib')
    (output / 'report.json').write_text(json.dumps(metadata, indent=2, ensure_ascii=False))
    return metadata


def main():
    load_environment()
    parser = argparse.ArgumentParser()
    parser.add_argument('--write-db', action='store_true')
    parser.add_argument('--output', default=str(Path(__file__).parent / 'artifacts/demo'))
    args = parser.parse_args()
    people, vacancies, applications, rows = generate()
    counts = {'postulantes': len(people), 'vacantes': len(vacancies), 'postulaciones': len(applications)}
    if args.write_db:
        uri = os.environ.get('MONGODB_URI')
        if not uri:
            raise ValueError('Falta MONGODB_URI')
        tls = ((uri.startswith('mongodb+srv://') and not re.search(r'[?&](?:tls|ssl)=false', uri, re.I))
               or re.search(r'[?&](?:tls|ssl)=true', uri, re.I))
        with MongoClient(uri, serverSelectionTimeoutMS=30000,
                         **({'tlsCAFile': certifi.where()} if tls else {})) as client:
            db = client[demo_database_name(os.environ.get('MONGODB_DATABASE', 'hireconnect'))]
            counts = persist(db, people, vacancies, applications)
            # Train on the snapshots actually read back from the demo database.
            saved = list(db.postulaciones.find({'datasetVersion': VERSION, 'synthetic': True}))
            rows = sorted([{'features': extract(a['prediccionPerfil']), 'y': int(a['estado']=='ACEPTADA'),
                'group': a['postulanteId'], 'date': a['fechaPostulacion'].date(),
                'resolved_date': a['resultadoActualizadoEn'].date(), 'source': 'simulado'} for a in saved], key=lambda r: r['date'])
    metadata = train_demo(rows, args.output)
    print(json.dumps({'synthetic': True, 'counts': counts, 'outcomes': dict(Counter(r['y'] for r in rows)),
        'modelVersion': metadata['modelVersion'], 'output': str(Path(args.output)),
        'metrics': {k: metadata['metrics'][k] for k in ['train', 'test', 'roc_auc', 'brier', 'baseline_brier']}}, ensure_ascii=False))

if __name__ == '__main__':
    try:
        main()
    except PyMongoError as error:
        raise SystemExit('No se pudo preparar la base demo: ' + type(error).__name__)
