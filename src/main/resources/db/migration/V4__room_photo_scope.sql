ALTER TABLE ${catalogSchema}.media_file ADD COLUMN room_scope varchar(80);

-- Preserve existing room uploads without moving shared hotel identity files.
UPDATE ${catalogSchema}.media_file f
SET room_scope = r.code
FROM ${catalogSchema}.room_type r
WHERE r.hotel_id = f.hotel_id
  AND EXISTS (SELECT 1 FROM ${catalogSchema}.room_photo p WHERE p.room_type_id = r.id AND p.path = '/api/media/' || f.id)
  AND (SELECT count(DISTINCT p.room_type_id) FROM ${catalogSchema}.room_photo p WHERE p.path = '/api/media/' || f.id) = 1
  AND NOT EXISTS (SELECT 1 FROM ${catalogSchema}.hotel h WHERE h.id = f.hotel_id
                  AND '/api/media/' || f.id IN (h.logo_path, h.facade_path, h.video_path));

ALTER TABLE ${catalogSchema}.rate DROP CONSTRAINT rate_check;
ALTER TABLE ${catalogSchema}.rate ADD CONSTRAINT rate_package_fields CHECK (
    (mode = 'PACKAGE' AND package_hours > 0 AND package_hours IS NOT NULL
     AND (additional_hour_amount IS NULL OR additional_hour_amount >= 0))
    OR (mode <> 'PACKAGE' AND package_hours IS NULL AND additional_hour_amount IS NULL)
);
