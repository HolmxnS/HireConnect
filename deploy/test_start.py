import os
import io
from pathlib import Path
import socket
import sys
import tempfile
import unittest
from unittest.mock import Mock, patch

from start import configuration, prepare_model, start_services, stop, wait_for_predictor

class ConfigurationTests(unittest.TestCase):
    def test_render_uses_internal_model_and_same_database_as_java(self):
        _, env, embedded, port = configuration({
            'PORT': '10000', 'SPRING_DATA_MONGODB_URI': 'mongodb://database/hireconnect',
            'PYTHON_ML_URL': 'https://obsolete.example/predict'}, '/app')
        self.assertTrue(embedded)
        self.assertEqual(port, 5050)
        self.assertEqual(env['PORT'], '10000')
        self.assertEqual(env['MONGODB_URI'], 'mongodb://database/hireconnect')
        self.assertEqual(env['PYTHON_ML_URL'], 'http://127.0.0.1:5050/predict')

    def test_external_service_is_supported_when_requested(self):
        _, env, embedded, _ = configuration({'ML_EMBEDDED': 'false',
                       'PYTHON_ML_URL': 'https://predictor.example/predict'}, '/app')
        self.assertFalse(embedded)
        self.assertEqual(env['PYTHON_ML_URL'], 'https://predictor.example/predict')

    def test_model_cannot_take_render_public_port(self):
        with self.assertRaisesRegex(ValueError, 'distinto'):
            configuration({'PORT': '10000', 'ML_PORT': '10000'})

    def test_missing_database_fails_explicitly_before_starting_java(self):
        with tempfile.TemporaryDirectory() as directory:
            root, env, _, _ = configuration({}, directory)
            with self.assertRaisesRegex(RuntimeError, 'MONGODB_URI'):
                prepare_model(root, env)

    def test_existing_model_is_not_retrained_on_every_start(self):
        with tempfile.TemporaryDirectory() as directory:
            model = Path(directory) / 'model.joblib'
            model.touch()
            with patch('start.subprocess.run') as run:
                prepare_model(Path(directory), {'MODEL_PATH': str(model)})
                run.assert_not_called()

    def test_failed_predictor_cannot_be_reported_as_ready(self):
        process = Mock()
        process.poll.return_value = 1
        with self.assertRaisesRegex(RuntimeError, 'termino'):
            wait_for_predictor(process, 5050)

    def test_empty_local_database_can_start_without_a_trained_model(self):
        process = Mock()
        process.poll.return_value = None
        response = io.BytesIO(b'{"modelReady":false,"schemaVersion":2}')
        with patch('start.urlopen', return_value=response):
            wait_for_predictor(process, 5050, require_model=False)

    @unittest.skipUnless(os.environ.get('RUN_DEPLOY_SMOKE') == '1', 'Requires local TCP binding')
    def test_internal_service_readiness_and_clean_shutdown(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            (root / 'ml').mkdir()
            (root / 'ml/app.py').write_text('''from flask import Flask, jsonify

def create_app():
    app = Flask(__name__)
    @app.get('/health')
    def health(): return jsonify(modelReady=True, schemaVersion=2)
    return app
''')
            with socket.socket() as listener:
                listener.bind(('127.0.0.1', 0))
                port = listener.getsockname()[1]
            env = dict(os.environ)
            processes = start_services(root, env, port,
                        [sys.executable, '-c', 'import time; time.sleep(60)'])
            try:
                self.assertEqual(len(processes), 2)
                self.assertTrue(all(p.poll() is None for p in processes))
            finally:
                stop(processes)
            self.assertTrue(all(p.poll() is not None for p in processes))

if __name__ == '__main__':
    unittest.main()
