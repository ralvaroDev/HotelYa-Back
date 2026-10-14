#!/usr/bin/env bash
# Explicit preparation of HotelYa's optional local example. Never runs at startup.
set -euo pipefail
cd "$(dirname "$0")/.."
seed_schema=hotelya
seed_owner=''
while [[ $# -gt 0 ]]; do
  case "$1" in
    --schema) seed_schema="${2:?Falta el esquema}"; shift 2 ;;
    --owner-email) seed_owner="${2:?Falta el correo}"; shift 2 ;;
    --help) echo 'Uso: ./scripts/seed-local.sh [--owner-email correo] [--schema hotelya|hotelya_test]'; exit 0 ;;
    *) echo "Argumento desconocido: $1" >&2; exit 1 ;;
  esac
done
case "$seed_schema" in
  hotelya|hotelya_test) ;;
  *) echo 'Solo se permiten los esquemas locales hotelya y hotelya_test.' >&2; exit 1 ;;
esac
# This exact container belongs to HotelYa local. No .env or remote connection is used.
{
  echo 'SET LOCAL search_path TO :"catalog_schema";'
  cat scripts/local/seed-catalog.sql
  if [[ -n "$seed_owner" ]]; then
    echo "SELECT set_config('hotelya.seed.owner_email', :'owner_email', true);"
    cat scripts/local/assign-owner.sql
  fi
} | docker exec -i supabase_db_hotelya psql -X -U postgres -d postgres \
    --single-transaction -v ON_ERROR_STOP=1 -v catalog_schema="$seed_schema" -v owner_email="$seed_owner" -f -
echo 'Preparación local terminada. Los datos existentes y las contraseñas se conservan.'
