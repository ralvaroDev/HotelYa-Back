#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
python3 ./scripts/backend-process.py stop
./scripts/stop-supabase.sh
echo 'Backend y Supabase de HotelYa detenidos. Los datos se conservan.'
