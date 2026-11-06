"""Read-only MongoDB training. Pending results are never treated as rejections."""
import argparse
import re
import certifi
from collections import Counter
from datetime import date, datetime, timezone
import json
import os
import uuid
import hashlib
import platform
from importlib.metadata import version as package_version
from zoneinfo import ZoneInfo
from pathlib import Path

import joblib
import pandas as pd
from pymongo import MongoClient
from pymongo.errors import PyMongoError
from bson import DBRef, ObjectId
from sklearn.compose import ColumnTransformer
from sklearn.impute import SimpleImputer
from sklearn.linear_model import LogisticRegression
from sklearn.calibration import CalibratedClassifierCV
from sklearn.frozen import FrozenEstimator
from sklearn.metrics import brier_score_loss, log_loss, roc_auc_score, precision_score, recall_score
from sklearn.pipeline import Pipeline
from sklearn.preprocessing import OneHotEncoder, StandardScaler

from features import SCHEMA_VERSION, NUMERIC, EXPERIENCE_FIT, CATEGORICAL, extract, from_documents, parse_date


def reference_id(value):
    if isinstance(value, DBRef):
        return value.id
    if isinstance(value, dict):
        return value.get('$id')
    if isinstance(value, str) and ObjectId.is_valid(value):
        return ObjectId(value)
    return value


def instant(value):
    if isinstance(value, datetime):
        return value.replace(tzinfo=timezone.utc) if value.tzinfo is None else value.astimezone(timezone.utc)
    if isinstance(value, str) and 'T' in value:
        try:
            parsed = datetime.fromisoformat(value.replace('Z', '+00:00'))
            return parsed.replace(tzinfo=timezone.utc) if parsed.tzinfo is None else parsed.astimezone(timezone.utc)
        except ValueError:
            pass
    return None


def capture_status(application):
    if application.get('prediccionOrigen') != 'FLUJO_APLICACION':
        return 'sin_origen_captura'
    captured = instant(application.get('prediccionCapturadaEn'))
    decided = instant(application.get('resultadoActualizadoEn'))
    if captured is None or decided is None:
        return 'sin_fechas_captura_decision'
    if captured > decided:
        return 'decision_anterior_al_perfil'
    reference = parse_date((application.get('prediccionPerfil') or {}).get('fechaReferencia'))
    capture_day = captured.astimezone(ZoneInfo('America/Bogota')).date()
    if reference is None or not 0 <= (capture_day - reference).days <= 1:
        return 'fecha_perfil_inconsistente'
    return None


def load_rows(db, allow_current=False):
    counts = Counter()
    rows = []
    seen = set()
    applications = list(db.postulaciones.find({'estado': {'$in': ['ACEPTADA', 'RECHAZADA']}},
                                    {'postulanteId': 1, 'vacanteId': 1, 'prediccionPerfil': 1,
                                     'estado': 1, 'fechaPostulacion': 1, 'prediccionOrigen': 1,
                                     'prediccionCapturadaEn': 1, 'resultadoActualizadoEn': 1}))
    people, vacancies = {}, {}
    if allow_current:
        person_ids = list({reference_id(a.get('postulanteId')) for a in applications} - {None})
        vacancy_ids = list({reference_id(a.get('vacanteId')) for a in applications} - {None})
        people = {str(p['_id']): p for p in db.postulantes.find({'_id': {'$in': person_ids}},
                  {'habilidades': 1, 'experiencia': 1, 'educacion': 1})}
        vacancies = {str(v['_id']): v for v in db.vacantes.find({'_id': {'$in': vacancy_ids}},
                     {'habilidadesRequeridas': 1, 'experienciaRequerida': 1, 'modalidad': 1, 'tipoContrato': 1})}
    for app in applications:
        person_id, vacancy_id = reference_id(app.get('postulanteId')), reference_id(app.get('vacanteId'))
        if not person_id or not vacancy_id:
            counts['sin_referencias'] += 1
            continue
        pair = (str(person_id), str(vacancy_id))
        if pair in seen:
            counts['duplicadas'] += 1
            continue
        payload = app.get('prediccionPerfil')
        if payload is not None and not isinstance(payload, dict):
            counts['datos_invalidos'] += 1
            continue
        source = 'snapshot'
        capture = capture_status(app) if payload else None
        if payload and capture:
            if not allow_current or capture in {'decision_anterior_al_perfil', 'fecha_perfil_inconsistente'}:
                counts[capture] += 1
                continue
            source = 'snapshot_sin_trazabilidad'
        if not payload:
            if not allow_current:
                counts['sin_snapshot'] += 1
                continue
            person, vacancy = people.get(str(person_id)), vacancies.get(str(vacancy_id))
            if not person or not vacancy:
                counts['referencia_eliminada'] += 1
                continue
            payload = from_documents(person, vacancy, datetime.now(ZoneInfo("America/Bogota")).date())
            source = 'perfil_actual'
        when = (instant(app.get('prediccionCapturadaEn')).astimezone(ZoneInfo('America/Bogota')).date()
                if source == 'snapshot' else parse_date(app.get('fechaPostulacion')))
        decision = (instant(app.get('resultadoActualizadoEn')).astimezone(ZoneInfo('America/Bogota')).date()
                    if source == 'snapshot' else None)
        try:
            features = extract(payload)
        except (ValueError, TypeError, AttributeError):
            counts['datos_invalidos'] += 1
            continue
        if not when:
            counts['sin_fecha'] += 1
            continue
        seen.add(pair)
        rows.append({'features': features, 'y': int(app['estado'] == 'ACEPTADA'),
                     'group': str(person_id), 'date': when, 'resolved_date': decision, 'source': source})
        counts[source] += 1
    return sorted(rows, key=lambda r: r['date']), dict(counts)


def temporal_split(rows):
    if len(rows) < 50:
        raise ValueError('Se requieren al menos 50 ejemplos utilizables; no se generan ejemplos artificiales.')
    cutoff = rows[int(len(rows) * .8)]['date']
    test = [r for r in rows if r['date'] >= cutoff]
    test_people = {r['group'] for r in test}
    train = [r for r in rows if r['date'] < cutoff and r['group'] not in test_people
             and (r.get('resolved_date') is None or r['resolved_date'] < cutoff)]
    for name, part in [('entrenamiento', train), ('evaluacion', test)]:
        counts = Counter(r['y'] for r in part)
        if min(counts.get(0, 0), counts.get(1, 0)) < 5:
            raise ValueError(f'Datos insuficientes en {name}: {dict(counts)}. Se requieren ambas clases y fechas diversas.')
    return train, test


def validation_folds(rows):
    """Forward validation inside training, keeping days and candidates separate."""
    folds = []
    for fraction in (.5, .75):
        start = rows[int(len(rows) * fraction)]['date']
        end_index = min(len(rows) - 1, int(len(rows) * (fraction + .25)))
        end = rows[end_index]['date']
        validation = [r for r in rows if start <= r['date'] and
                      (fraction == .75 or r['date'] < end)]
        people = {r['group'] for r in validation}
        training = [r for r in rows if r['date'] < start and r['group'] not in people
                    and (r.get('resolved_date') is None or r['resolved_date'] < start)]
        if all(min(Counter(r['y'] for r in part).get(0, 0),
                   Counter(r['y'] for r in part).get(1, 0)) >= 3 for part in (training, validation)):
            folds.append((training, validation))
    return folds


def estimator(numeric, regularization):
    prep = ColumnTransformer([
        ('numeric', Pipeline([('missing', SimpleImputer(strategy='median')),
                              ('scale', StandardScaler())]), numeric),
        ('categorical', OneHotEncoder(handle_unknown='ignore'), CATEGORICAL)])
    return Pipeline([('features', prep), ('classifier', LogisticRegression(max_iter=2000, C=regularization))])


def fit_estimator(rows, numeric, strength, calibrated=False):
    learning, calibration = temporal_split(rows) if calibrated else (rows, [])
    model = estimator(numeric, strength)
    model.fit(pd.DataFrame([r['features'] for r in learning]), [r['y'] for r in learning])
    if calibrated:
        # Frozen base estimator: all calibration examples are later and disjoint.
        model = CalibratedClassifierCV(FrozenEstimator(model), method='sigmoid')
        model.fit(pd.DataFrame([r['features'] for r in calibration]), [r['y'] for r in calibration])
    return model, learning, calibration


def select_configuration(training):
    folds = validation_folds(training)
    candidates = []
    for name, numeric in [('base', NUMERIC), ('ajuste_experiencia', NUMERIC + EXPERIENCE_FIT)]:
        for strength in (1.0, .1, .01, 10.0):
            for calibrated in (False, True):
                scores, reason = [], None
                for learn, validate in folds:
                    try:
                        model, _, _ = fit_estimator(learn, numeric, strength, calibrated)
                    except ValueError as error:
                        reason = str(error)
                        break
                    probabilities = model.predict_proba(pd.DataFrame([r['features'] for r in validate]))[:, 1]
                    scores.append(float(brier_score_loss([r['y'] for r in validate], probabilities)))
                if calibrated and scores and reason is None:
                    try:
                        fit_estimator(training, numeric, strength, calibrated)
                    except ValueError as error:
                        reason = str(error)
                candidates.append({'variables': name, 'C': strength,
                                   'calibracion': 'sigmoid' if calibrated else 'sin_calibracion_adicional',
                                   'folds': len(scores), 'exclusion': reason,
                                   'brier_cv': sum(scores) / len(scores) if scores and reason is None
                                               and len(scores) == len(folds) else None})
    usable = [c for c in candidates if c['brier_cv'] is not None]
    selected = min(usable, key=lambda c: c['brier_cv']) if usable else candidates[0]
    numeric = NUMERIC + EXPERIENCE_FIT if selected['variables'] == 'ajuste_experiencia' else NUMERIC
    return numeric, selected, candidates


def fit(rows):
    train, test = temporal_split(rows)
    x_train = pd.DataFrame([r['features'] for r in train])
    x_test = pd.DataFrame([r['features'] for r in test])
    y_train, y_test = [r['y'] for r in train], [r['y'] for r in test]
    if all(x_train[c].nunique() <= 1 for c in x_train):
        raise ValueError('Las variables son constantes; no hay informacion suficiente para aprender.')
    numeric, selected, comparison = select_configuration(train)
    model, learned, calibration_rows = fit_estimator(train, numeric, selected['C'], selected['calibracion'] == 'sigmoid')
    support = pd.DataFrame([r['features'] for r in learned])
    p = model.predict_proba(x_test)[:, list(model.classes_).index(1)]
    baseline = [sum(y_train) / len(y_train)] * len(y_test)
    previous = estimator(NUMERIC, 1.0)
    previous.fit(x_train, y_train)
    previous_p = previous.predict_proba(x_test)[:, 1]
    bins = []
    for low in (0, .2, .4, .6, .8):
        mask = (p >= low) & ((p <= 1) if low == .8 else (p < low + .2))
        if mask.any():
            bins.append({'intervalo': [low, round(low+.2, 1)], 'cantidad': int(mask.sum()),
                         'prediccion_media': float(p[mask].mean()),
                         'aceptacion_observada': float(pd.Series(y_test)[mask].mean())})
    report = {'rangos_train': {c: [float(support[c].min()), float(support[c].max())] for c in numeric},
              'categorias_train': {c: sorted(support[c].unique().tolist()) for c in CATEGORICAL},
              'seleccion': selected, 'comparacion_cv': comparison,
              'brier_modelo_anterior': float(brier_score_loss(y_test, previous_p)),
              'log_loss_modelo_anterior': float(log_loss(y_test, previous_p)),
              'variables_constantes': [c for c in numeric + CATEGORICAL if x_train[c].nunique() <= 1],
              'variables_distintas_train': {c: int(x_train[c].nunique()) for c in x_train},
              'fecha_corte': min(r['date'] for r in test).isoformat(),
              'train': len(train), 'test': len(test),
              'entrenamiento_clasificador': len(learned), 'calibracion_muestras': len(calibration_rows), 'train_aceptadas': sum(y_train),
              'test_aceptadas': sum(y_test), 'roc_auc': float(roc_auc_score(y_test, p)),
              'brier': float(brier_score_loss(y_test, p)),
              'baseline_brier': float(brier_score_loss(y_test, baseline)),
              'log_loss': float(log_loss(y_test, p)),
              'precision': float(precision_score(y_test, p >= .5, zero_division=0)),
              'recall': float(recall_score(y_test, p >= .5, zero_division=0)),
              'calibracion': bins,
              'split': 'temporal; candidatos de evaluacion excluidos del entrenamiento'}
    return model, report


def audit_database(db, allow_current=False):
    rows, counts = load_rows(db, allow_current)
    labels = Counter(r['y'] for r in rows)
    skills = Counter(str(round(r['features']['coincidenciaHabilidades'], 2)) for r in rows)
    education = Counter(str(r['features']['educacionRegistrada']) for r in rows)
    try:
        train, test = temporal_split(rows)
        can_split, reason = True, None
        sizes = {'entrenamiento': len(train), 'evaluacion': len(test)}
    except ValueError as error:
        can_split, reason, sizes = False, str(error), None
    return {
        'modo': 'experimental_con_historicos' if allow_current else 'capturas_con_trazabilidad',
        'capturas_totales': db.postulaciones.count_documents({'prediccionPerfil': {'$exists': True, '$ne': None}}),
        'resueltas_con_captura': db.postulaciones.count_documents({'prediccionPerfil': {'$exists': True, '$ne': None},
                                                     'estado': {'$in': ['ACEPTADA', 'RECHAZADA']}}),
        'capturas_pendientes': db.postulaciones.count_documents({'prediccionPerfil': {'$exists': True, '$ne': None},
                                                     'estado': {'$nin': ['ACEPTADA', 'RECHAZADA']}}),
        'utilizables': len(rows), 'aceptadas': labels.get(1, 0), 'rechazadas': labels.get(0, 0),
        'conteos': counts, 'coincidencia_habilidades': dict(skills), 'educacion_registrada': dict(education),
        'habilidades_con_variacion': len(skills) > 1,
        'educacion_con_variacion': len(education) > 1,
        'permite_evaluacion_separada': can_split, 'motivo': reason, 'particiones': sizes,
        'nota': 'La trazabilidad del flujo no certifica la autenticidad de las decisiones ni su representatividad.'}


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--allow-current-profiles', action='store_true',
                        help='Solo exploracion: reconstruye con perfiles actuales, no historicos.')
    parser.add_argument('--audit-only', action='store_true')
    parser.add_argument('--output', default=str(Path(__file__).resolve().parent / 'artifacts'))
    args = parser.parse_args()
    uri = os.environ.get('MONGODB_URI')
    if not uri:
        parser.error('Define MONGODB_URI en el entorno; no se incluyen credenciales en codigo.')
    use_tls = (uri.startswith('mongodb+srv://') and not re.search(r'[?&](?:tls|ssl)=false', uri, re.I)) or re.search(r'[?&](?:tls|ssl)=true', uri, re.I)
    tls_options = {'tlsCAFile': os.environ.get('MONGODB_TLS_CA_FILE', certifi.where())} if use_tls else {}
    with MongoClient(uri, serverSelectionTimeoutMS=10000, **tls_options) as client:
        db = client[os.environ.get('MONGODB_DATABASE', 'hireconnect')]
        if args.audit_only:
            print(json.dumps(audit_database(db, args.allow_current_profiles), indent=2, ensure_ascii=False))
            return
        rows, counts = load_rows(db, args.allow_current_profiles)
    print(json.dumps({'utilizables': len(rows), 'conteos': counts}, ensure_ascii=False))
    model, metrics = fit(rows)
    metadata = {'schemaVersion': SCHEMA_VERSION, 'algorithm': 'logistic_regression',
                'modelVersion': uuid.uuid4().hex[:12],
                'trainingMode': 'experimental_con_historicos' if args.allow_current_profiles else 'capturas_con_trazabilidad',
                'experimental': True, 'featureVersion': 2, 'trainedAt': datetime.now(timezone.utc).isoformat(),
                'fuentes': counts, 'metrics': metrics,
                'dataFingerprint': hashlib.sha256(json.dumps(rows, sort_keys=True, default=str).encode()).hexdigest(),
                'runtime': {'python': platform.python_version(), 'scikit-learn': package_version('scikit-learn')},
                'limitations': ['Resultados historicos no implican causalidad ni garantizan aceptacion.',
                                'Verificar procedencia de los datos y calibracion antes de publicar.',
                                'Perfiles actuales pueden diferir del momento de postulacion.']}
    output = Path(args.output)
    output.mkdir(parents=True, exist_ok=True)
    # Only load locally generated, trusted joblib files.
    temporary = output / ('model.' + metadata['modelVersion'] + '.tmp')
    joblib.dump({'model': model, 'metadata': metadata}, temporary)
    temporary.replace(output / 'model.joblib')
    for filename, document in [
        ('report.json', metadata),
        ('manifest.json', {'modelVersion': metadata['modelVersion'], 'experimental': metadata['experimental'],
                           'sha256': hashlib.sha256((output / 'model.joblib').read_bytes()).hexdigest(),
                           'schemaVersion': SCHEMA_VERSION, 'runtime': metadata['runtime']})]:
        staged = output / (filename + '.' + metadata['modelVersion'] + '.tmp')
        staged.write_text(json.dumps(document, indent=2, ensure_ascii=False))
        staged.replace(output / filename)
    print(json.dumps(metadata, indent=2, ensure_ascii=False))

if __name__ == '__main__':
    try:
        main()
    except ValueError as error:
        raise SystemExit(str(error))
    except PyMongoError as error:
        raise SystemExit('No se pudo consultar MongoDB: ' + type(error).__name__)
