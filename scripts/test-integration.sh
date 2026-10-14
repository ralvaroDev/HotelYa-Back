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
exec ./gradlew integrationTest "$@"
