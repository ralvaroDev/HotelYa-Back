#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
if [[ ! -f .env ]]; then
  echo 'Falta HotelYa-Back/.env. Crea el archivo privado siguiendo el README.' >&2
  exit 1
fi
set -a
source .env
set +a
export SPRING_PROFILES_ACTIVE=local
./scripts/start-supabase.sh
if python3 ./scripts/backend-process.py status; then
  echo 'El backend de HotelYa ya está activo; se omite su arranque.'
  exit 0
fi
exec ./gradlew bootRun "$@"
