#!/usr/bin/env bash
set -euo pipefail
APP_ROOT="$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)"
ML_PYTHON="$APP_ROOT/.python/bin/python3"
if [[ ! -f "$ML_PYTHON" ]]; then
    echo "Falta el runtime Python. Este servicio requiere azure-package.zip, no solo app.jar." >&2
    exit 1
fi
if [[ ! -x "$ML_PYTHON" ]]; then chmod u+x "$ML_PYTHON"; fi
# Azure's application package may be read-only. Keep trained data in /home.
ML_DATA_DIR="${ML_DATA_DIR:-/home/data/hireconnect-ml}"
export ML_MODEL_MODE="${ML_MODEL_MODE:-$("$ML_PYTHON" "$APP_ROOT/ml/runtime_mode.py")}"
if [[ "$ML_MODEL_MODE" == "demo" ]]; then ML_DATA_DIR="$ML_DATA_DIR/demo"; fi
mkdir -p "$ML_DATA_DIR"
export MODEL_PATH="${MODEL_PATH:-$ML_DATA_DIR/model.joblib}"
export PORT="${SERVER_PORT:-${PORT:-8080}}"
export ML_EMBEDDED=true
export PYTHONUNBUFFERED=1
echo "Iniciando HireConnect con Java + Python (paquete Linux)"
cd "$APP_ROOT"
exec "$ML_PYTHON" "$APP_ROOT/deploy/start.py"
