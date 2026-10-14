#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
if [[ ! -f .env ]]; then echo 'Falta HotelYa-Back/.env para las pruebas aisladas.' >&2; exit 1; fi
set -a
source .env
set +a
export HOTELYA_DB_SCHEMA='hotelya_test'
export SERVER_PORT='8081'
export HOTELYA_CORS_ORIGINS='http://127.0.0.1:5174'
export HOTELYA_UPLOAD_DIR='/private/tmp/hotelya-browser-test-uploads'
export HOTELYA_LUMIS_OWNER_EMAIL=''
export HOTELYA_LUMIS_OWNER_PASSWORD=''
export SPRING_PROFILES_ACTIVE='default'
./gradlew bootJar --console=plain
exec java -jar build/libs/hotelya-back-0.0.1-SNAPSHOT.jar --server.address=127.0.0.1 --catalog.seed.enabled=false
