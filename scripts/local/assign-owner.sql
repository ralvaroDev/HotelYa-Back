-- The caller supplies hotelya.seed.owner_email as a transaction-local setting.
DO $$
DECLARE
    account_id bigint;
    hotel_id bigint;
    current_owner bigint;
BEGIN
    SELECT id INTO account_id FROM owner_account
    WHERE email = lower(btrim(current_setting('hotelya.seed.owner_email')));
    IF account_id IS NULL THEN
        RAISE EXCEPTION 'Primero registra esta cuenta mediante la aplicación.';
    END IF;
    SELECT id, owner_id INTO hotel_id, current_owner FROM hotel
    WHERE code = 'lumis-hotel' FOR UPDATE;
    IF hotel_id IS NULL THEN
        RAISE EXCEPTION 'Primero carga el catálogo local de ejemplo.';
    END IF;
    IF current_owner IS NOT NULL AND current_owner <> account_id THEN
        RAISE EXCEPTION 'El hotel ya pertenece a otra cuenta; no se cambia su propietario.';
    END IF;
    IF EXISTS (SELECT 1 FROM hotel WHERE owner_id = account_id AND id <> hotel_id) THEN
        RAISE EXCEPTION 'La cuenta ya tiene otro hotel asociado.';
    END IF;
    UPDATE hotel SET owner_id = account_id WHERE id = hotel_id AND owner_id IS NULL;
END $$;
