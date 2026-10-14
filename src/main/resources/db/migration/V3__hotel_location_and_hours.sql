ALTER TABLE ${catalogSchema}.hotel
    ADD COLUMN map_url varchar(2048),
    ADD COLUMN opening_time time,
    ADD COLUMN closing_time time,
    ADD CONSTRAINT hotel_hours_pair CHECK ((opening_time IS NULL) = (closing_time IS NULL));
