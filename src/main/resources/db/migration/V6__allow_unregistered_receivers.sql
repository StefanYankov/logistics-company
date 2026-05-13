ALTER TABLE shipments
    ADD receiver_email VARCHAR(255);

ALTER TABLE shipments
    ADD receiver_name VARCHAR(255);

ALTER TABLE shipments
    ADD receiver_phone VARCHAR(16);

ALTER TABLE shipments
    ALTER COLUMN receiver_id DROP NOT NULL;