import os
import tempfile
import unittest
from pathlib import Path
from unittest.mock import patch
import joblib
import numpy as np
from app import create_app
from test_model import payload

class Predictor:
    classes_ = [0, 1]
    def __init__(self, probability): self.probability = probability
    def predict_proba(self, _): return np.array([[1-self.probability, self.probability]])

def artifact(probability):
    return {'model': Predictor(probability), 'metadata': {'schemaVersion': 2,
            'trainedAt': str(probability), 'metrics': {'rangos_train': {'coincidenciaHabilidades': [0, 100]},
            'categorias_train': {'modalidad': ['remoto']}}}}

class ReloadTests(unittest.TestCase):
    def test_reload_atomically_and_keep_previous_model_on_damage(self):
        with tempfile.TemporaryDirectory() as directory, patch.dict(os.environ, {'ML_ENABLE_EXPERIMENTAL': 'true'}):
            path = Path(directory) / 'model.joblib'
            joblib.dump(artifact(.4), path)
            with patch.dict(os.environ, {'MODEL_PATH': str(path)}):
                app = create_app()
            client = app.test_client()
            self.assertEqual(client.post('/predict', json=payload()).json['probabilidad'], 40)
            replacement = Path(directory) / 'replacement.joblib'
            joblib.dump(artifact(.8), replacement)
            replacement.replace(path)
            self.assertEqual(client.get('/health').json['trainedAt'], '0.8')
            self.assertEqual(client.post('/predict', json=payload()).json['probabilidad'], 80)
            path.write_bytes(b'corrupt')
            with self.assertLogs(app.logger, level='ERROR'):
                self.assertEqual(client.post('/predict', json=payload()).json['probabilidad'], 80)
            self.assertTrue(client.get('/health').json['modelReady'])

    def test_unseen_category_returns_estimate_with_limited_coverage(self):
        with patch.dict(os.environ, {'ML_ENABLE_EXPERIMENTAL': 'true'}):
            client = create_app(artifact(.4)).test_client()
            data = payload()
            data['modalidad'] = 'Presencial'
            result = client.post('/predict', json=data)
            self.assertEqual(result.status_code, 200)
            self.assertTrue(result.json['coberturaLimitada'])

if __name__ == '__main__': unittest.main()
