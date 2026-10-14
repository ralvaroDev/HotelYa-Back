-- Optional local example. Only inserts children when the hotel itself is new.
WITH new_hotel AS (
    INSERT INTO hotel (code, name, description, address, location, phone,
                       logo_path, facade_path, video_path, published)
    VALUES ('lumis-hotel', 'Lumi''s Hotel',
            'Te damos la bienvenida a Lumi''s Hotel, ubicado en Av. Lizardo Montero 161, en San Juan de Miraflores, Lima. Un punto de partida para una visita al sur de la ciudad o una pausa en tu día.',
            'Av. Lizardo Montero 161, San Juan de Miraflores, Lima, Perú.',
            'San Juan de Miraflores, Lima', '+5117557324',
            'assets/images/lumis-hotel-logo.jpg', 'assets/images/lumis-hotel-fachada.jpg',
            'assets/videos/lumis-promocional.mp4', true)
    ON CONFLICT (code) DO NOTHING
    RETURNING id
), new_rooms AS (
    INSERT INTO room_type (hotel_id, code, name, description, display_order)
    SELECT h.id, v.code, v.name, v.description, v.position
    FROM new_hotel h CROSS JOIN (VALUES
        ('tematica', 'Temática', 'Un ambiente especial para celebrar en pareja, con detalles románticos. Consulta las opciones de decoración para tu ocasión.', 0),
        ('matrimonial', 'Matrimonial', 'Un espacio acogedor con cama matrimonial, iluminación cálida y televisión para disfrutar de una estadía en pareja.', 1),
        ('premium', 'Premium', 'Disfruta de una habitación con jacuzzi y un rincón para relajarte, ideal para una escapada especial.', 2)
    ) v(code, name, description, position)
    RETURNING id, code, name
), new_cards AS (
    INSERT INTO stay_card (hotel_id, title, description, display_order)
    SELECT h.id, v.title, v.description, v.position
    FROM new_hotel h CROSS JOIN (VALUES
        ('Una escapada en pareja', 'Haz espacio para compartir y descansar a tu ritmo.', 0),
        ('Una visita a la ciudad', 'Consulta las condiciones y servicios para tu estadía.', 1),
        ('Un descanso en el camino', 'Organiza tu llegada directamente con el hotel.', 2)
    ) v(title, description, position)
), new_photos AS (
    INSERT INTO room_photo (room_type_id, path, alt_text, display_order)
    SELECT id, 'assets/images/lumis-cuarto-' || CASE WHEN code = 'tematica' THEN 'tematico' ELSE code END || '.jpg', 'Tipo de habitación ' || name, 0
    FROM new_rooms
), new_details AS (
    INSERT INTO room_detail (room_type_id, kind, text, display_order)
    SELECT id, 'RECOMMENDATION', 'Consulta las modalidades y servicios disponibles directamente con el hotel.', 0
    FROM new_rooms
)
INSERT INTO rate (room_type_id, mode, amount, currency, package_hours, additional_hour_amount, simulated)
SELECT r.id, v.mode, v.amount, 'PEN', v.hours, v.extra, true
FROM new_rooms r JOIN (VALUES
    ('tematica', 'HOUR', 25, NULL::integer, NULL::numeric),
    ('tematica', 'PACKAGE', 60, 3, 20),
    ('tematica', 'NIGHT', 120, NULL, NULL),
    ('matrimonial', 'HOUR', 20, NULL, NULL),
    ('matrimonial', 'PACKAGE', 50, 3, 15),
    ('matrimonial', 'NIGHT', 100, NULL, NULL),
    ('premium', 'HOUR', 35, NULL, NULL),
    ('premium', 'PACKAGE', 90, 3, 25),
    ('premium', 'NIGHT', 180, NULL, NULL)
) v(code, mode, amount, hours, extra) ON v.code = r.code;
