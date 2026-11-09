"""Regression tests for the real failure: hybrid + no skills returned 99%."""
import numpy as np
import unittest
from unittest.mock import Mock
from app import create_app
from test_model import payload


def bundle():
    model = Mock()
    model.classes_ = [0, 1]
    model.predict_proba.return_value = np.array([[.01, .99]])
    return {'model': model, 'metadata': {'schemaVersion': 2, 'experimental': False,
        'trainingMode': 'capturas_con_trazabilidad', 'fuentes': {'snapshot': 100},
        'metrics': {'rangos_train': {'coincidenciaHabilidades': [0, 100], 'experienciaAnos': [0, 25]},
                    'categorias_train': {'modalidad': ['hibrido', 'remoto']}}}}


class ReliabilityTests(unittest.TestCase):
    def test_legacy_dataset_does_not_display_spurious_99_percent(self):
        b = bundle()
        b['metadata'].update(trainingMode='experimental_con_historicos', fuentes={'perfil_actual': 439})
        b['metadata']['metrics']['rangos_train']['coincidenciaHabilidades'] = [100, 100]
        client = create_app(b).test_client()
        for mode in ['HIBRIDO', 'REMOTO', 'PRESENCIAL']:
            for skills, overlap in [([], 0), (['Java'], 50), (['Java', 'Python'], 100)]:
                data = payload(); data.update(habilidades=skills, modalidad=mode)
                result = client.post('/predict', json=data).json
                self.assertNotIn('probabilidad', result)
                self.assertEqual(result['tipoResultado'], 'coincidencia_habilidades')
                self.assertEqual(result['coincidenciaHabilidades'], overlap)
                self.assertIn('historial_sin_trazabilidad', result['motivos'])
        b['model'].predict_proba.assert_not_called()

    def test_supported_model_keeps_real_medium_probability_without_rescaling(self):
        b = bundle(); b['model'].predict_proba.return_value = np.array([[.45, .55]])
        result = create_app(b).test_client().post('/predict', json=payload()).json
        self.assertEqual(result['probabilidad'], 55)
        self.assertEqual(result['compatibilidad'], 'Media')
        self.assertTrue(result['probabilidadDisponible'])

    def test_thresholds_are_applied_to_model_output(self):
        for probability, level in [(.449, 'Baja'), (.45, 'Media'), (.699, 'Media'), (.7, 'Alta')]:
            b = bundle(); b['model'].predict_proba.return_value = np.array([[1-probability, probability]])
            result = create_app(b).test_client().post('/predict', json=payload()).json
            self.assertEqual(result['compatibilidad'], level)

    def test_unsupported_profile_is_not_sent_to_model(self):
        b = bundle(); data = payload(); data['modalidad'] = 'NUEVA'
        result = create_app(b).test_client().post('/predict', json=data).json
        self.assertNotIn('probabilidad', result)
        self.assertTrue(result['coberturaLimitada'])
        b['model'].predict_proba.assert_not_called()

    def test_missing_requirements_do_not_turn_into_zero_probability(self):
        b = bundle(); b['metadata']['trainingMode'] = 'experimental_con_historicos'
        data = payload(); data['habilidadesRequeridas'] = ''
        result = create_app(b).test_client().post('/predict', json=data).json
        self.assertEqual(result['tipoResultado'], 'datos_insuficientes')
        self.assertFalse(result['disponible'])
        self.assertIsNone(result['coincidenciaHabilidades'])
        self.assertNotIn('probabilidad', result)

    def test_invalid_model_probability_is_rejected(self):
        for p in [float('nan'), float('inf'), -.1, 1.1]:
            b = bundle(); b['model'].predict_proba.return_value = np.array([[1-p, p]])
            self.assertEqual(create_app(b).test_client().post('/predict', json=payload()).status_code, 503)

    def test_health_distinguishes_operational_service_from_probability_readiness(self):
        b = bundle(); b['metadata']['fuentes'] = {'perfil_actual': 439}
        result = create_app(b).test_client().get('/health').json
        self.assertTrue(result['modelReady'])
        self.assertTrue(result['predictionReady'])
        self.assertFalse(result['probabilityReady'])

if __name__ == '__main__': unittest.main()
