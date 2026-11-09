"""Decide whether training evidence supports displaying an acceptance probability.

A requirements overlap is a separate, deterministic measurement, never an
acceptance probability or a substitute training label.
"""
import math


def training_limits(metadata):
    reasons = []
    if metadata.get('synthetic'):
        reasons.append('datos_generados')
    sources = metadata.get('fuentes', {})
    if (metadata.get('trainingMode') != 'capturas_con_trazabilidad'
            or not sources.get('snapshot') or sources.get('perfil_actual') or sources.get('snapshot_sin_trazabilidad')):
        reasons.append('historial_sin_trazabilidad')
    ranges = metadata.get('metrics', {}).get('rangos_train', {})
    for key in ('coincidenciaHabilidades', 'experienciaAnos'):
        values = ranges.get(key)
        if not values or len(values) != 2 or values[0] >= values[1]:
            reasons.append(key + '_sin_variacion')
    return reasons


def coverage_limits(metadata, features):
    metrics = metadata.get('metrics', {})
    unsupported = [key for key, (low, high) in metrics.get('rangos_train', {}).items()
                   if not low <= features[key] <= high]
    unsupported += [key for key, values in metrics.get('categorias_train', {}).items()
                    if features[key] not in values]
    return unsupported


def overlap_result(features, reasons, unsupported):
    known = bool(features['habilidadesConocidas'])
    return {'disponible': known, 'tipoResultado': 'coincidencia_habilidades' if known else 'datos_insuficientes',
            'algorithm': 'skill_overlap', 'schemaVersion': 2,
            'coincidenciaHabilidades': round(features['coincidenciaHabilidades'], 1) if known else None,
            'habilidadesCoincidentes': features['habilidadesCoincidentes'],
            'habilidadesRequeridasTotal': features['habilidadesRequeridasTotal'],
            'motivos': reasons, 'coberturaLimitada': bool(unsupported),
            'probabilidadDisponible': False}


def valid_probability(value):
    return math.isfinite(value) and 0 <= value <= 1
