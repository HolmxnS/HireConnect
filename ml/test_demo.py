import unittest
from collections import Counter
from datetime import date
from pathlib import Path
from tempfile import TemporaryDirectory
from unittest.mock import Mock, patch

from app import create_app
from demo import generate, persist, train_demo, demo_database_name
from runtime_mode import model_mode


class DemoTests(unittest.TestCase):
    def test_varied_data_is_reproducible_identified_and_pairs_are_unique(self):
        people, vacancies, apps, rows = generate(200, 20)
        self.assertEqual((len(people), len(vacancies), len(apps)), (200, 20, 600))
        self.assertEqual(generate(200, 20)[2], apps)
        self.assertEqual(len({(a['postulanteId'], a['vacanteId']) for a in apps}), len(apps))
        self.assertTrue(all(d['synthetic'] is True for d in people + vacancies + apps))
        self.assertTrue(all(p['email'].endswith('@example.invalid') and p['loginDisabled'] for p in people))
        self.assertTrue(all(a['prediccionOrigen'] == 'SIMULACION' for a in apps))
        for feature in ['coincidenciaHabilidades', 'educacionRegistrada', 'experienciaAnos']:
            self.assertGreater(len({r['features'][feature] for r in rows}), 1)
        for mode in ['hibrido', 'presencial', 'remoto']:
            self.assertEqual(set(r['y'] for r in rows if r['features']['modalidad'] == mode), {0, 1})

    def test_database_is_isolated_and_real_collections_cannot_be_written(self):
        self.assertEqual(demo_database_name('hireconnect'), 'hireconnect_ml_demo')
        db = Mock(); db.name = 'hireconnect'
        with self.assertRaises(ValueError): persist(db, [], [], [])
        db.__getitem__.assert_not_called() if hasattr(db, '__getitem__') else None

    def test_test_stage_mode_can_be_explicitly_disabled(self):
        self.assertEqual(model_mode({}), 'demo')
        self.assertEqual(model_mode({'ML_MODEL_MODE': 'real'}), 'real')
        with self.assertRaises(ValueError): model_mode({'ML_MODEL_MODE': 'typo'})

    def test_demo_training_and_api_are_labelled_and_require_demo_mode(self):
        _, _, _, rows = generate(300, 30)
        with TemporaryDirectory() as directory:
            metadata = train_demo(rows, directory)
            import joblib
            bundle = joblib.load(Path(directory) / 'model.joblib')
        self.assertTrue(metadata['synthetic'])
        self.assertEqual(metadata['trainingMode'], 'simulacion')
        # A profile drawn from supported ranges, with a partial skills match.
        data = {'schemaVersion': 2, 'fechaReferencia': '2026-11-07', 'habilidades': ['Java'],
            'habilidadesRequeridas': 'Java, Python', 'experiencia': [], 'educacionRegistrada': False,
            'experienciaRequerida': 'DE_1_A_2_ANIOS', 'modalidad': 'Híbrido', 'tipoContrato': 'INDEFINIDO'}
        with patch.dict('os.environ', {'ML_MODEL_MODE': 'demo'}):
            client = create_app(bundle).test_client()
            result = client.post('/predict', json=data).json
            self.assertEqual(result['tipoResultado'], 'simulacion')
            self.assertTrue(result['synthetic'])
            self.assertGreaterEqual(result['probabilidad'], 0)
            self.assertLessEqual(result['probabilidad'], 100)
            self.assertTrue(client.get('/health').json['simulationReady'])
            self.assertFalse(client.get('/health').json['probabilityReady'])
        with patch.dict('os.environ', {'ML_MODEL_MODE': 'real'}):
            result = create_app(bundle).test_client().post('/predict', json=data).json
            self.assertEqual(result['tipoResultado'], 'coincidencia_habilidades')
            self.assertNotIn('probabilidad', result)

if __name__ == '__main__': unittest.main()
