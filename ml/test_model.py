import os
import unittest
from datetime import date, timedelta
from unittest.mock import patch

from features import extract, experience_years


def payload():
    return {'schemaVersion': 2, 'fechaReferencia': '2026-01-01',
            'habilidades': ['JÁVA', 'SQL'], 'habilidadesRequeridas': 'java, Python',
            'experiencia': [], 'educacionRegistrada': False,
            'experienciaRequerida': 'DE_1_A_2_ANIOS', 'modalidad': 'REMOTO', 'tipoContrato': 'INDEFINIDO'}


class FeatureTests(unittest.TestCase):
    def test_skill_overlap_not_constant(self):
        data = payload()
        self.assertEqual(extract(data)['coincidenciaHabilidades'], 50)
        data['habilidades'] = ['Python', 'java']
        self.assertEqual(extract(data)['coincidenciaHabilidades'], 100)
        data['habilidadesRequeridas'] = ''
        self.assertEqual(extract(data)['habilidadesConocidas'], 0)

    def test_overlapping_jobs_and_future_dates(self):
        years = experience_years([{'fechaInicio': '2020-01-01', 'fechaFin': '2022-01-01'},
                                  {'fechaInicio': '2021-01-01', 'fechaFin': '2023-01-01'},
                                  {'fechaInicio': '2027-01-01'}, {'fechaFin': '2024-01-01'}], date(2026, 1, 1))
        self.assertAlmostEqual(years, 3, places=2)

    def test_schema_and_dates_required(self):
        with self.assertRaises(ValueError):
            extract({'edad': 22, 'coincidenciaHabilidades': 80})
        data = payload()
        data['fechaReferencia'] = 'invalida'
        with self.assertRaises(ValueError):
            extract(data)

    def test_unknown_requirements_not_same_as_no_experience(self):
        data = payload()
        data['experienciaRequerida'] = None
        self.assertEqual(extract(data)['experienciaRequeridaConocida'], 0)
        data['experienciaRequerida'] = 'SIN_EXPERIENCIA'
        self.assertEqual(extract(data)['experienciaRequeridaConocida'], 1)

    def test_pipeline_and_api(self):
        from train import fit, temporal_split
        from app import create_app
        # Artificial fixtures exercise implementation only; never used as the real artifact.
        rows = []
        for i in range(100):
            data = payload()
            data['habilidades'] = ['Java', 'Python'] if i % 2 else []
            data['experiencia'] = [] if i % 3 else [{'fechaInicio': '2025-01-01'}]
            rows.append({'features': extract(data), 'y': i % 2,
                         'date': date(2024, 1, 1) + timedelta(days=i), 'group': str(i)})
        training, testing = temporal_split(rows)
        self.assertFalse({r['group'] for r in training} & {r['group'] for r in testing})
        model, report = fit(rows)
        self.assertIn('brier', report)
        bundle = {'model': model, 'metadata': {'schemaVersion': 2, 'metrics': report,
                  'trainingMode': 'capturas_con_trazabilidad', 'fuentes': {'snapshot': len(rows)}}}
        client = create_app(bundle).test_client()
        with patch.dict(os.environ, {'ML_ENABLE_EXPERIMENTAL': 'false'}):
            self.assertEqual(client.post('/predict', json=payload()).status_code, 503)
        with patch.dict(os.environ, {'ML_ENABLE_EXPERIMENTAL': 'true'}):
            self.assertEqual(client.post('/predict', json={}).status_code, 400)
            low = payload(); low['habilidades'] = []
            high = payload(); high['habilidades'] = ['Java', 'Python']
            a = client.post('/predict', json=low).get_json()
            b = client.post('/predict', json=high).get_json()
            self.assertLess(a['probabilidad'], b['probabilidad'])
            self.assertEqual(a['algorithm'], 'logistic_regression')
            high['educacionRegistrada'] = True
            outside = client.post('/predict', json=high)
            self.assertEqual(outside.status_code, 200)
            self.assertTrue(outside.get_json()['coberturaLimitada'])

    def test_calibration_uses_later_disjoint_examples(self):
        from train import fit_estimator
        from features import NUMERIC
        rows = []
        for i in range(160):
            data = payload()
            data['habilidades'] = ['Java', 'Python'] if i % 2 else []
            data['experiencia'] = [] if i % 3 else [{'fechaInicio': '2025-01-01'}]
            rows.append({'features': extract(data), 'y': i % 2,
                         'date': date(2024, 1, 1)+timedelta(days=i), 'group': str(i)})
        rows[10]['group'] = rows[140]['group']
        model, learning, calibration = fit_estimator(rows, NUMERIC, 1, calibrated=True)
        self.assertTrue(calibration)
        self.assertFalse({r['group'] for r in learning} & {r['group'] for r in calibration})
        self.assertLess(max(r['date'] for r in learning), min(r['date'] for r in calibration))
        self.assertEqual(model.__class__.__name__, 'CalibratedClassifierCV')

    def test_skill_separators_and_invalid_types(self):
        data = payload()
        data['habilidades'] = ['JÁVA; Python']
        self.assertEqual(extract(data)['coincidenciaHabilidades'], 100)
        data['educacionRegistrada'] = 'false'
        with self.assertRaises(ValueError):
            extract(data)

    def test_experience_shortfall_and_surplus(self):
        data = payload()
        data['experienciaRequerida'] = 'DE_2_A_3_ANIOS'
        features = extract(data)
        self.assertEqual(features['experienciaFaltanteAnos'], 2)
        self.assertEqual(features['experienciaExcedenteAnos'], 0)
        data['experiencia'] = [{'fechaInicio': '2020-01-01', 'fechaFin': '2023-01-01'}]
        features = extract(data)
        self.assertAlmostEqual(features['experienciaExcedenteAnos'], 1, places=2)
        self.assertEqual(features['experienciaFaltanteAnos'], 0)

    def test_forward_validation_keeps_candidates_and_dates_separate(self):
        from train import validation_folds, select_configuration
        rows = [{'features': extract(payload()), 'y': i % 2,
                 'date': date(2024, 1, 1) + timedelta(days=i), 'group': str(i)} for i in range(100)]
        rows[10]['group'] = rows[60]['group']
        folds = validation_folds(rows)
        self.assertEqual(len(folds), 2)
        for training, validation in folds:
            self.assertFalse({r['group'] for r in training} & {r['group'] for r in validation})
            self.assertLess(max(r['date'] for r in training), min(r['date'] for r in validation))
        self.assertEqual(len(select_configuration(rows)[2]), 16)

    def test_refuses_insufficient_training(self):
        from train import fit
        with self.assertRaises(ValueError):
            fit([])

if __name__ == '__main__':
    unittest.main()
