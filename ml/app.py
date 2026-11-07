"""Inference only: no database connection and no training during HTTP requests."""
import os
import threading
from pathlib import Path
import joblib
import pandas as pd
from flask import Flask, request, jsonify
from features import SCHEMA_VERSION, extract


def create_app(bundle=None):
    app = Flask(__name__)
    app.config['MAX_CONTENT_LENGTH'] = 128 * 1024
    path = None if bundle is not None else Path(os.environ.get(
        'MODEL_PATH', str(Path(__file__).resolve().parent / 'artifacts/model.joblib')))
    signature = None
    lock = threading.Lock()

    def refresh():
        nonlocal bundle, signature
        if path is None or not path.is_file():
            return
        with lock:
            stat = path.stat()
            current = (stat.st_mtime_ns, stat.st_size)
            if current == signature:
                return
            try:
                replacement = joblib.load(path)
                if replacement['metadata'].get('schemaVersion') != SCHEMA_VERSION:
                    raise ValueError('Version incompatible')
                if not hasattr(replacement['model'], 'predict_proba'):
                    raise ValueError('Artefacto sin predictor')
                bundle = replacement
            except Exception:
                # Keep serving the previous valid model if a replacement is damaged.
                app.logger.exception('No se pudo cargar el artefacto del modelo')
            signature = current

    refresh()

    @app.get('/health')
    def health():
        refresh()
        current = bundle
        return jsonify({'modelReady': current is not None, 'schemaVersion': SCHEMA_VERSION,
                        'trainedAt': current['metadata'].get('trainedAt') if current else None,
                        'modelVersion': current['metadata'].get('modelVersion', 'legacy') if current else None,
                        'experimental': current['metadata'].get('experimental', True) if current else None,
                        'predictionReady': current is not None and (not current['metadata'].get('experimental', True)
                            or os.environ.get('ML_ENABLE_EXPERIMENTAL', 'true').lower() == 'true')})

    @app.post('/predict')
    def predict():
        refresh()
        current = bundle
        if current is None:
            return jsonify({'error': 'Modelo pendiente de entrenamiento', 'schemaVersion': SCHEMA_VERSION}), 503
        if current['metadata'].get('schemaVersion') != SCHEMA_VERSION:
            return jsonify({'error': 'Modelo incompatible'}), 503
        if current['metadata'].get('experimental', True) and os.environ.get('ML_ENABLE_EXPERIMENTAL', 'true').lower() != 'true':
            return jsonify({'error': 'Modelo experimental deshabilitado'}), 503
        data = request.get_json(silent=True)
        if not isinstance(data, dict):
            return jsonify({'error': 'Se requiere un objeto JSON'}), 400
        try:
            features = extract(data)
        except (ValueError, TypeError, AttributeError) as error:
            return jsonify({'error': str(error)}), 400
        ranges = current['metadata'].get('metrics', {}).get('rangos_train')
        if not ranges:
            return jsonify({'error': 'Modelo sin informe de cobertura'}), 503
        unsupported = [key for key, (low, high) in ranges.items() if not low <= features[key] <= high]
        categories = current['metadata'].get('metrics', {}).get('categorias_train', {})
        unsupported += [key for key, values in categories.items() if features[key] not in values]
        model = current['model']
        probability = float(model.predict_proba(pd.DataFrame([features]))[0, list(model.classes_).index(1)])
        # Display categories are provisional UI thresholds, not a separately trained target.
        return jsonify({'probabilidad': round(probability * 100, 1),
                        'compatibilidad': 'Alta' if probability >= .7 else 'Media' if probability >= .45 else 'Baja',
                        'schemaVersion': SCHEMA_VERSION, 'experimental': current['metadata'].get('experimental', True),
                        'modelVersion': current['metadata'].get('modelVersion', 'legacy'),
                        'coberturaLimitada': bool(unsupported),
                        'algorithm': 'logistic_regression'})
    return app

if __name__ == '__main__':
    create_app().run(host='127.0.0.1', port=int(os.environ.get('PORT', 5050)), debug=False)
