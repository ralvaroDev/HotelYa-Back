#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
docker info >/dev/null
ready=true
for service in db pooler studio pg_meta rest kong; do
  state="$(docker inspect --format '{{if .State.Running}}{{if .State.Health}}{{.State.Health.Status}}{{else}}running{{end}}{{else}}stopped{{end}}' "supabase_${service}_hotelya" 2>/dev/null || true)"
  if [[ "$state" != healthy && "$state" != running ]]; then ready=false; fi
done
if [[ "$ready" == true ]]; then
  echo 'Supabase de HotelYa ya está activo; se omite su arranque.'
  exit 0
fi
if [[ "$(docker inspect --format '{{.State.Running}}' supabase_db_hotelya 2>/dev/null || true)" != "true" ]]; then
  python3 - <<'CHECK'
import socket
ports = [55320,55321,55322,55323,55324,55325,55327,55328,55329]
occupied = []
for port in ports:
    with socket.socket() as listener:
        try:
            listener.bind(('0.0.0.0', port))
        except OSError:
            occupied.append(port)
if occupied:
    raise SystemExit('Puertos ocupados: ' + ', '.join(map(str, occupied)) + '. No se detuvo ningún proyecto. Revisa lsof y docker ps.')
CHECK
fi
: "${SUPABASE_INTERNAL_IMAGE_REGISTRY:=docker.io}"
export SUPABASE_INTERNAL_IMAGE_REGISTRY
exec supabase start
