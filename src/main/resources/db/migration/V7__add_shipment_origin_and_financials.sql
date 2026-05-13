-- 1. Origin Location Fields
ALTER TABLE shipments ADD origin_office_id BIGINT;
ALTER TABLE shipments ADD origin_city_id BIGINT;
ALTER TABLE shipments ADD origin_street VARCHAR(255);
ALTER TABLE shipments ADD origin_district VARCHAR(100);
ALTER TABLE shipments ADD origin_building VARCHAR(50);
ALTER TABLE shipments ADD origin_entrance VARCHAR(50);
ALTER TABLE shipments ADD origin_floor VARCHAR(50);
ALTER TABLE shipments ADD origin_apartment VARCHAR(50);
ALTER TABLE shipments ADD origin_latitude DOUBLE PRECISION;
ALTER TABLE shipments ADD origin_longitude DOUBLE PRECISION;

-- 2. Current Location Fields
ALTER TABLE shipments ADD current_office_id BIGINT;
ALTER TABLE shipments ADD current_courier_id UUID;

-- 3. Financial Fields
-- (total_price is already there, but we added paidBy and isPaid)
ALTER TABLE shipments ADD paid_by VARCHAR(50);
ALTER TABLE shipments ADD is_paid BOOLEAN DEFAULT FALSE;

-- Set default for existing records before applying NOT NULL constraint
UPDATE shipments SET is_paid = FALSE WHERE is_paid IS NULL;
UPDATE shipments SET paid_by = 'SENDER' WHERE paid_by IS NULL;

ALTER TABLE shipments ALTER COLUMN paid_by SET NOT NULL;
ALTER TABLE shipments ALTER COLUMN is_paid SET NOT NULL;

-- 4. Delivery Address Latitude/Longitude (Missing from previous script)
ALTER TABLE shipments ADD delivery_latitude DOUBLE PRECISION;
ALTER TABLE shipments ADD delivery_longitude DOUBLE PRECISION;

-- 5. Foreign Key Constraints
ALTER TABLE shipments ADD CONSTRAINT FK_SHIPMENTS_ON_ORIGIN_OFFICE FOREIGN KEY (origin_office_id) REFERENCES offices (id);
ALTER TABLE shipments ADD CONSTRAINT FK_SHIPMENTS_ON_ORIGIN_CITY FOREIGN KEY (origin_city_id) REFERENCES cities (id);
ALTER TABLE shipments ADD CONSTRAINT FK_SHIPMENTS_ON_CURRENT_OFFICE FOREIGN KEY (current_office_id) REFERENCES offices (id);
ALTER TABLE shipments ADD CONSTRAINT FK_SHIPMENTS_ON_CURRENT_COURIER FOREIGN KEY (current_courier_id) REFERENCES couriers (id);
    