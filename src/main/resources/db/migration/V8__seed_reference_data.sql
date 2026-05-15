-- V8__seed_reference_data.sql
-- Seeds the database with the initial Company, major Cities, branch Offices, and the Service Catalog.

-- 1. Seed the primary Company
INSERT INTO companies (id, version, created_at, updated_at, name, registration_number)
VALUES (1, 0, NOW(), NOW(), 'FastTrack Logistics Ltd.', 'BG123456789')
ON CONFLICT (id) DO NOTHING;

-- 2. Seed Major Cities in Bulgaria
INSERT INTO cities (id, version, created_at, updated_at, name, postcode)
VALUES
    (1, 0, NOW(), NOW(), 'Sofia', '1000'),
    (2, 0, NOW(), NOW(), 'Plovdiv', '4000'),
    (3, 0, NOW(), NOW(), 'Varna', '9000'),
    (4, 0, NOW(), NOW(), 'Burgas', '8000'),
    (5, 0, NOW(), NOW(), 'Ruse', '7000'),
    (6, 0, NOW(), NOW(), 'Stara Zagora', '6000')
ON CONFLICT (id) DO NOTHING;

-- 3. Seed Primary Offices
INSERT INTO offices (id, version, created_at, updated_at, company_id, city_id, street, district, building, latitude, longitude)
VALUES
    (1, 0, NOW(), NOW(), 1, 1, 'Tsarigradsko Shose 115', 'Mladost', 'G', 42.6610, 23.3855),
    (2, 0, NOW(), NOW(), 1, 2, 'Ruski Blvd 50', 'Center', null, 42.1449, 24.7451),
    (3, 0, NOW(), NOW(), 1, 3, 'Slivnitsa Blvd 2', 'Center', null, 43.2046, 27.9105)
ON CONFLICT (id) DO NOTHING;

-- 4. Seed Office Operating Hours
-- Clear existing hours for these offices to avoid duplication on re-runs
DELETE FROM office_operating_hours WHERE office_id IN (1, 2, 3);

INSERT INTO office_operating_hours (office_id, day_of_week, open_time, close_time, is_closed)
VALUES
    (1, 'MONDAY', '09:00:00', '18:00:00', false),
    (1, 'TUESDAY', '09:00:00', '18:00:00', false),
    (1, 'WEDNESDAY', '09:00:00', '18:00:00', false),
    (1, 'THURSDAY', '09:00:00', '18:00:00', false),
    (1, 'FRIDAY', '09:00:00', '18:00:00', false),

    (2, 'MONDAY', '09:00:00', '18:00:00', false),
    (2, 'TUESDAY', '09:00:00', '18:00:00', false),
    (2, 'WEDNESDAY', '09:00:00', '18:00:00', false),
    (2, 'THURSDAY', '09:00:00', '18:00:00', false),
    (2, 'FRIDAY', '09:00:00', '18:00:00', false),

    (3, 'MONDAY', '09:00:00', '18:00:00', false),
    (3, 'TUESDAY', '09:00:00', '18:00:00', false),
    (3, 'WEDNESDAY', '09:00:00', '18:00:00', false),
    (3, 'THURSDAY', '09:00:00', '18:00:00', false),
    (3, 'FRIDAY', '09:00:00', '18:00:00', false);

-- 5. Seed Service Catalog (Shipment Addons)
INSERT INTO services_catalog (id, version, created_at, updated_at, name, pricing_type, pricing_value)
VALUES
    (1, 0, NOW(), NOW(), 'Fragile', 'FIXED_AMOUNT', 5.00),
    (2, 0, NOW(), NOW(), 'SMS Notification', 'FIXED_AMOUNT', 0.20),
    (3, 0, NOW(), NOW(), 'Review Before Payment', 'FIXED_AMOUNT', 2.00),
    (4, 0, NOW(), NOW(), 'Express Next-Day', 'FIXED_AMOUNT', 10.00),
    (5, 0, NOW(), NOW(), 'Heavy Duty Oversize', 'PERCENTAGE_OF_BASE', 0.25)
ON CONFLICT (id) DO NOTHING;

-- 6. Seed Default Pricing Configuration
INSERT INTO pricing_configs (id, version, created_at, updated_at, base_price, price_per_kg, address_surcharge, active_from)
VALUES (1, 0, NOW(), NOW(), 5.00, 1.50, 3.50, NOW())
ON CONFLICT (id) DO NOTHING;

-- Reset Sequences to ensure next inserts from Hibernate do not conflict with the seeded IDs
SELECT setval('companies_id_seq', (SELECT MAX(id) FROM companies));
SELECT setval('cities_id_seq', (SELECT MAX(id) FROM cities));
SELECT setval('offices_id_seq', (SELECT MAX(id) FROM offices));
SELECT setval('services_catalog_id_seq', (SELECT MAX(id) FROM services_catalog));
SELECT setval('pricing_configs_id_seq', (SELECT MAX(id) FROM pricing_configs));
