"""Unlabelled synthetic requests test operation only, never predictive accuracy."""
import argparse
from collections import Counter
import json
import math
import os
from pathlib import Path
import random
import statistics
import time

from app import create_app


def requests(count=512, seed=42):
    rng = random.Random(seed)
    vocabulary = ['Java', 'Python', 'SQL', 'Excel', 'Comunicación', 'Trabajo en equipo']
    for i in range(count):
        skills = rng.sample(vocabulary, i % 7)
        wanted = ', '.join(rng.sample(vocabulary, (i // 7) % 7))
        experience = [] if i % 5 == 0 else [{'fechaInicio': f'{2010 + i % 15}-01-01'}]
        if i % 9 == 0:
            experience += [{'fechaInicio': '2021-01-01', 'fechaFin': '2023-01-01'},
                           {'fechaInicio': '2022-01-01', 'fechaFin': '2024-01-01'}]
        yield {'schemaVersion': 2, 'fechaReferencia': '2026-11-05',
               'habilidades': skills, 'habilidadesRequeridas': wanted,
               'experiencia': experience, 'educacionRegistrada': bool(i % 2),
               'experienciaRequerida': [None, 'SIN_EXPERIENCIA', 'DE_1_A_2_ANIOS',
                                        'DE_2_A_3_ANIOS', 'MAS_DE_3_ANIOS'][i % 5],
               'modalidad': ['Presencial', 'Remoto', 'Híbrido', 'No especificada'][i % 4],
               'tipoContrato': ['TIEMPO_COMPLETO', 'TIEMPO_PARCIAL', 'INDEFINIDO', None][i % 4]}


def verify(count=512):
    if count < 10:
        raise ValueError('Se requieren al menos 10 casos de prueba')
    client = create_app().test_client()
    health = client.get('/health').json
    if not health.get('modelReady'):
        raise ValueError('No hay un modelo cargado para verificar')
    latencies, failures, limited, predictions = [], [], 0, []
    result_types = Counter()
    for i, payload in enumerate(requests(count)):
        started = time.perf_counter()
        response = client.post('/predict', json=payload)
        latencies.append((time.perf_counter()-started) * 1000)
        result = response.get_json() or {}
        probability = (result.get('coincidenciaHabilidades') if result.get('tipoResultado') == 'coincidencia_habilidades' else result.get('probabilidad'))
        insufficient = result.get('tipoResultado') == 'datos_insuficientes'
        result_types[result.get('tipoResultado', 'sin_tipo')] += 1
        if result.get('tipoResultado') in {'coincidencia_habilidades', 'datos_insuficientes'} and 'probabilidad' in result:
            failures.append({'caso': i, 'error': 'Coincidencia presentada como probabilidad'})
        if insufficient and response.status_code == 200 and 'probabilidad' not in result:
            continue
        if (response.status_code != 200 or not isinstance(probability, (float, int))
                or not math.isfinite(probability) or not 0 <= probability <= 100):
            failures.append({'caso': i, 'status': response.status_code})
        else:
            predictions.append(probability)
            limited += int(bool(result.get('coberturaLimitada')))
    invalid = [{}, {'schemaVersion': 999}, {'schemaVersion': 2, 'fechaReferencia': 'no es fecha'},
               dict(next(requests()), habilidades='Java'), dict(next(requests()), experiencia='incorrecta'),
               dict(next(requests()), educacionRegistrada='false'), dict(next(requests()), modalidad=['Remoto'])]
    for i, payload in enumerate(invalid):
        if client.post('/predict', json=payload).status_code != 400:
            failures.append({'entrada_invalida': i})
    return {'synthetic': True, 'usedForTraining': False, 'modelVersion': health.get('modelVersion'),
            'scope': 'Funcionamiento y validacion de entradas; no mide precision predictiva.',
            'casos_validos': count, 'entradas_invalidas': len(invalid), 'fallos': failures,
            'casos_cobertura_limitada': limited,
            'porcentajes_distintos': len(set(predictions)),
            'tipos_resultado': dict(result_types),
            'latencia_mediana_ms': round(statistics.median(latencies), 3),
            'latencia_p95_ms': round(sorted(latencies)[math.ceil(.95 * len(latencies))-1], 3)}


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--cases', type=int, default=512)
    parser.add_argument('--output', default=str(Path(__file__).resolve().parent/'artifacts/robustness-report.json'))
    args = parser.parse_args()
    report = verify(args.cases)
    path = Path(args.output)
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(report, indent=2, ensure_ascii=False))
    print(json.dumps(report, indent=2, ensure_ascii=False))
    if report['fallos']:
        raise SystemExit(1)

if __name__ == '__main__':
    try:
        main()
    except ValueError as error:
        raise SystemExit(str(error))
