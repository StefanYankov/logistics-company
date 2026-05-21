-- V12__add_address_details_to_companies_table.sql

-- Step 1: Alter the schema layout to add the new embedded address columns as nullable
ALTER TABLE companies ADD COLUMN city_id BIGINT;
ALTER TABLE companies ADD COLUMN street VARCHAR(100);
ALTER TABLE companies ADD COLUMN district VARCHAR(100);
ALTER TABLE companies ADD COLUMN building VARCHAR(10);
ALTER TABLE companies ADD COLUMN entrance VARCHAR(10);
ALTER TABLE companies ADD COLUMN floor VARCHAR(10);
ALTER TABLE companies ADD COLUMN apartment VARCHAR(10);
ALTER TABLE companies ADD COLUMN latitude DOUBLE PRECISION;
ALTER TABLE companies ADD COLUMN longitude DOUBLE PRECISION;

-- Step 2: Update the pre-existing company record (ID 1) with its headquarters data
UPDATE companies
SET
    city_id = 1, -- References 'Sofia' seeded in V8
    street = 'Tsarigradsko Shose 115',
    district = 'Mladost',
    building = 'G',
    latitude = 42.6610,
    longitude = 23.3855
WHERE id = 1;

-- Step 3: Now that data is present, safely enforce NOT NULL boundaries
ALTER TABLE companies ALTER COLUMN city_id SET NOT NULL;
ALTER TABLE companies ALTER COLUMN street SET NOT NULL;

-- Step 4: Add foreign key and structural constraint guards for the company relation
ALTER TABLE companies
    ADD CONSTRAINT fk_companies_on_city FOREIGN KEY (city_id) REFERENCES cities (id);

-- Step 5: Clean up old structural modifications requested by the generator tool
ALTER TABLE shipments DROP COLUMN IF EXISTS latitude;
ALTER TABLE shipments DROP COLUMN IF EXISTS longitude;

ALTER TABLE shipments ALTER COLUMN origin_apartment TYPE VARCHAR(10) USING (origin_apartment::VARCHAR(10));
ALTER TABLE shipments ALTER COLUMN origin_building TYPE VARCHAR(10) USING (origin_building::VARCHAR(10));
ALTER TABLE shipments ALTER COLUMN origin_entrance TYPE VARCHAR(10) USING (origin_entrance::VARCHAR(10));
ALTER TABLE shipments ALTER COLUMN origin_floor TYPE VARCHAR(10) USING (origin_floor::VARCHAR(10));
ALTER TABLE shipments ALTER COLUMN origin_street TYPE VARCHAR(100) USING (origin_street::VARCHAR(100));
ALTER TABLE shipments ALTER COLUMN paid_by TYPE VARCHAR(255) USING (paid_by::VARCHAR(255));