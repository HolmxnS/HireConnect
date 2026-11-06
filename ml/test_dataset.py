import copy
from datetime import date, datetime, timedelta, timezone
import unittest
from unittest.mock import Mock
from bson import DBRef, ObjectId
from train import load_rows, audit_database, temporal_split, validation_folds
from test_model import payload


def application(index=1, accepted=True):
    data = payload()
    data['habilidades'] = ['Java', 'Python'] if accepted else []
    data['educacionRegistrada'] = accepted
    return {'postulanteId': DBRef('postulantes', ObjectId(f'{index:024x}')),
            'vacanteId': DBRef('vacantes', ObjectId('f' * 24)),
            'estado': 'ACEPTADA' if accepted else 'RECHAZADA',
            'prediccionPerfil': data, 'prediccionOrigen': 'FLUJO_APLICACION',
            'prediccionCapturadaEn': datetime(2026, 1, 1, 15, tzinfo=timezone.utc),
            'resultadoActualizadoEn': datetime(2026, 1, 2, 15, tzinfo=timezone.utc),
            'fechaPostulacion': datetime(2026, 1, 1)}


def database(applications):
    db = Mock()
    db.postulaciones.find.return_value = applications
    db.postulaciones.count_documents.return_value = len(applications)
    db.postulantes.find.return_value = []
    db.vacantes.find.return_value = []
    return db


class DatasetTests(unittest.TestCase):
    def test_uses_captured_profiles_and_reports_actual_variation(self):
        db = database([application(1), application(2, False)])
        report = audit_database(db)
        self.assertEqual(report['utilizables'], 2)
        self.assertEqual(report['aceptadas'], 1)
        self.assertEqual(report['rechazadas'], 1)
        self.assertTrue(report['habilidades_con_variacion'])
        self.assertTrue(report['educacion_con_variacion'])
        self.assertFalse(report['permite_evaluacion_separada'])
        self.assertEqual(db.postulaciones.find.call_args.args[0], {'estado': {'$in': ['ACEPTADA', 'RECHAZADA']}})

    def test_legacy_capture_requires_explicit_experimental_mode(self):
        old = application()
        del old['prediccionOrigen']
        db = database([old])
        rows, counts = load_rows(db)
        self.assertEqual(rows, [])
        self.assertEqual(counts['sin_origen_captura'], 1)
        rows, _ = load_rows(db, allow_current=True)
        self.assertEqual(rows[0]['source'], 'snapshot_sin_trazabilidad')
        self.assertIsNone(rows[0]['resolved_date'])

    def test_never_uses_profile_recorded_after_decision(self):
        old = application()
        old['resultadoActualizadoEn'] = datetime(2025, 12, 31)
        for exploratory in (False, True):
            rows, counts = load_rows(database([old]), allow_current=exploratory)
            self.assertEqual(rows, [])
            self.assertEqual(counts['decision_anterior_al_perfil'], 1)

    def test_invalid_legacy_row_cannot_hide_a_valid_capture(self):
        good = application()
        old = copy.deepcopy(good)
        del old['resultadoActualizadoEn']
        rows, counts = load_rows(database([old, good]))
        self.assertEqual(len(rows), 1)
        self.assertEqual(counts['sin_fechas_captura_decision'], 1)

    def test_excludes_decisions_not_known_at_training_cutoff(self):
        rows = [{'date': date(2024, 1, 1) + timedelta(days=i),
                 'resolved_date': date(2024, 1, 1) + timedelta(days=i),
                 'y': i % 2, 'group': str(i)} for i in range(100)]
        rows[10]['resolved_date'] = date(2025, 1, 1)
        train, _ = temporal_split(rows)
        self.assertNotIn('10', {r['group'] for r in train})
        for train, _ in validation_folds(rows):
            self.assertNotIn('10', {r['group'] for r in train})

    def test_malformed_capture_is_counted_instead_of_crashing_audit(self):
        bad = application()
        bad['prediccionPerfil'] = 'invalid'
        rows, counts = load_rows(database([bad]))
        self.assertEqual(rows, [])
        self.assertEqual(counts['datos_invalidos'], 1)

if __name__ == '__main__':
    unittest.main()
