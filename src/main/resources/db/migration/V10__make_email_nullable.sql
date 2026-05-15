-- V10__make_email_nullable.sql
-- Allows walk-in customers to be registered without an email address.

ALTER TABLE users ALTER COLUMN email DROP NOT NULL;
