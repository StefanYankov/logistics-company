ALTER TABLE cities
    ADD CONSTRAINT uk_city_postcode UNIQUE (postcode);

ALTER TABLE cities
    DROP CONSTRAINT uk_city_name_postcode;