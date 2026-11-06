"""Shared feature extraction for training and inference. No personal identifiers."""
import re
import unicodedata
from datetime import date, datetime

SCHEMA_VERSION = 2
NUMERIC = ['coincidenciaHabilidades', 'habilidadesConocidas', 'experienciaAnos',
           'experienciaRequeridaAnos', 'experienciaRequeridaConocida', 'educacionRegistrada']
EXPERIENCE_FIT = ['experienciaFaltanteAnos', 'experienciaExcedenteAnos']
CATEGORICAL = ['modalidad', 'tipoContrato']

def normalize(value):
    return ''.join(c for c in unicodedata.normalize('NFD', str(value or '').strip().lower())
                   if unicodedata.category(c) != 'Mn')

def parse_date(value):
    if isinstance(value, datetime):
        return value.date()
    if isinstance(value, date):
        return value
    try:
        return date.fromisoformat(str(value)[:10])
    except (TypeError, ValueError):
        return None

def experience_years(items, reference):
    # Merge overlapping jobs, exclude future time and incomplete/invalid start dates.
    intervals = []
    for item in items or []:
        start = parse_date(item.get('fechaInicio'))
        raw_end = item.get('fechaFin')
        end = parse_date(raw_end) if raw_end else reference
        if start and end and start <= min(end, reference):
            intervals.append((start, min(end, reference)))
    merged = []
    for start, end in sorted(intervals):
        if merged and start <= merged[-1][1]:
            merged[-1] = (merged[-1][0], max(end, merged[-1][1]))
        else:
            merged.append((start, end))
    return sum((end-start).days for start, end in merged) / 365.25

def extract(payload):
    if payload.get('schemaVersion') != SCHEMA_VERSION:
        raise ValueError('Se requiere schemaVersion=2')
    reference = parse_date(payload.get('fechaReferencia'))
    if reference is None:
        raise ValueError('fechaReferencia debe ser una fecha ISO')
    skills = payload.get('habilidades')
    experience = payload.get('experiencia')
    required = payload.get('habilidadesRequeridas')
    if not isinstance(skills, list) or not all(isinstance(s, str) for s in skills):
        raise ValueError('habilidades debe ser una lista de textos')
    if not isinstance(experience, list) or not all(isinstance(e, dict) for e in experience):
        raise ValueError('experiencia debe ser una lista de fechas')
    if required is not None and not isinstance(required, str):
        raise ValueError('habilidadesRequeridas debe ser texto')
    if not isinstance(payload.get('educacionRegistrada'), bool):
        raise ValueError('educacionRegistrada debe ser verdadero o falso')
    for key in CATEGORICAL + ['experienciaRequerida']:
        if payload.get(key) is not None and not isinstance(payload[key], str):
            raise ValueError(key + ' debe ser texto')
    wanted = {normalize(s) for s in re.split(r'[,;\n]', required or '') if normalize(s)}
    owned = {normalize(part) for skill in skills for part in re.split(r'[,;\n]', skill) if normalize(part)}
    minimums = {'sin_experiencia': 0, 'sin experiencia': 0,
                'de_1_a_2_anios': 1, '1-2 anos': 1,
                'de_2_a_3_anios': 2, '2-3 anos': 2,
                'mas_de_3_anios': 3, '+3 anos': 3}
    minimum = minimums.get(normalize(payload.get('experienciaRequerida')))
    years = experience_years(experience, reference)
    return {
        'coincidenciaHabilidades': len(owned & wanted) / len(wanted) * 100 if wanted else 0,
        'habilidadesConocidas': int(bool(wanted)),
        'experienciaAnos': years,
        'experienciaFaltanteAnos': max(0, minimum - years) if minimum is not None else 0,
        'experienciaExcedenteAnos': max(0, years - minimum) if minimum is not None else 0,
        'experienciaRequeridaAnos': minimum if minimum is not None else 0,
        'experienciaRequeridaConocida': int(minimum is not None),
        'educacionRegistrada': int(bool(payload.get('educacionRegistrada'))),
        'modalidad': normalize(payload.get('modalidad')).replace('_', ' ') or 'desconocida',
        'tipoContrato': normalize(payload.get('tipoContrato')).replace('_', ' ') or 'desconocido',
    }

def from_documents(person, vacancy, reference):
    return {'schemaVersion': SCHEMA_VERSION, 'fechaReferencia': reference.isoformat(),
            'habilidades': person.get('habilidades') or [],
            'habilidadesRequeridas': vacancy.get('habilidadesRequeridas'),
            'experiencia': [{'fechaInicio': str(parse_date(e.get('fecha_inicio')) or ''),
                             'fechaFin': str(parse_date(e.get('fecha_fin')) or '')}
                            for e in person.get('experiencia') or []],
            'educacionRegistrada': bool(person.get('educacion')),
            'experienciaRequerida': vacancy.get('experienciaRequerida'),
            'modalidad': vacancy.get('modalidad'), 'tipoContrato': vacancy.get('tipoContrato')}
