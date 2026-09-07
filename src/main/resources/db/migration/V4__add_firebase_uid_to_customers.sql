-- ========================================================
-- Flyway Database Migration: V4__add_firebase_uid_to_customers.sql
-- Add firebase_uid to customers table for Firebase Authentication
-- ========================================================

ALTER TABLE customers
    ADD COLUMN IF NOT EXISTS firebase_uid VARCHAR(128) UNIQUE AFTER customer_id;
