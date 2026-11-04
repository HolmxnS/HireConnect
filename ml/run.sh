#!/bin/sh
set -eu
ML_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
ML_ENV="${ML_VENV:-$ML_DIR/.venv}"
if [ ! -x "$ML_ENV/bin/python" ]; then
    ML_PYTHON=""
    for candidate in python3.14 python3.13 python3.12 python3; do
        if command -v "$candidate" >/dev/null 2>&1; then
            ML_PYTHON="$candidate"
            break
        fi
    done
    if [ -z "$ML_PYTHON" ]; then
        echo "Se requiere Python 3.12 a 3.14." >&2
        exit 1
    fi
    if ! "$ML_PYTHON" -c 'import sys; sys.exit(0 if (3, 12) <= sys.version_info[:2] <= (3, 14) else 1)'; then
        echo "Usa Python 3.12, 3.13 o 3.14 para instalar las dependencias del modelo." >&2
        exit 1
    fi
    "$ML_PYTHON" -m venv "$ML_ENV"
fi
if ! "$ML_ENV/bin/python" -c 'import flask, sklearn, pandas, pymongo, certifi' >/dev/null 2>&1; then
    "$ML_ENV/bin/python" -m pip install -r "$ML_DIR/requirements.lock.txt"
fi
if [ "$#" -eq 0 ]; then set -- serve; fi
exec "$ML_ENV/bin/python" "$ML_DIR/manage.py" "$@"
