#!/usr/bin/env bash
# Deletes only isolated test records and their generated browser-test files.
set -euo pipefail
ids=$(docker exec supabase_db_hotelya psql -U postgres -d postgres -Atc 'SELECT id::text FROM hotelya_test.media_file')
while IFS= read -r id; do
  if [[ "$id" =~ ^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$ ]]; then
    rm -f "/private/tmp/hotelya-browser-test-uploads/$id"
  fi
done <<< "$ids"
docker exec supabase_db_hotelya psql -U postgres -d postgres -v ON_ERROR_STOP=1 -c 'TRUNCATE hotelya_test.stay_card, hotelya_test.media_file, hotelya_test.rate, hotelya_test.room_detail, hotelya_test.room_photo, hotelya_test.room_type, hotelya_test.hotel, hotelya_test.owner_session, hotelya_test.owner_account;'
