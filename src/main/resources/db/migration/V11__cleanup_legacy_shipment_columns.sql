-- V11__cleanup_legacy_shipment_columns.sql
-- Renames the legacy city_id column to delivery_city_id to correctly align with the embedded AddressDetails pattern.
-- This resolves the null constraint violation where Hibernate was trying to write to delivery_city_id while PostgreSQL expected city_id.

ALTER TABLE shipments RENAME COLUMN city_id TO delivery_city_id;
-- We must drop the NOT NULL constraint on delivery_city_id because shipments delivered to an office will NOT have a deliveryAddressSnapshot, and therefore delivery_city_id will be null.
ALTER TABLE shipments ALTER COLUMN delivery_city_id DROP NOT NULL;
-- The same logic applies to origin_city_id which was added in V7
ALTER TABLE shipments ALTER COLUMN origin_city_id DROP NOT NULL;
