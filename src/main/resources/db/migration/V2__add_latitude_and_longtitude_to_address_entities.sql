ALTER TABLE client_addresses
    ADD latitude DOUBLE PRECISION;

ALTER TABLE client_addresses
    ADD longitude DOUBLE PRECISION;

ALTER TABLE offices
    ADD latitude DOUBLE PRECISION;

ALTER TABLE offices
    ADD longitude DOUBLE PRECISION;

ALTER TABLE shipments
    ADD latitude DOUBLE PRECISION;

ALTER TABLE shipments
    ADD longitude DOUBLE PRECISION;