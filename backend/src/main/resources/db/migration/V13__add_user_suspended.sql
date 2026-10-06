-- =============================================
-- V13: Add suspended fields to users table
-- =============================================

ALTER TABLE users
    ADD COLUMN suspended BOOLEAN NOT NULL DEFAULT FALSE AFTER status,
    ADD COLUMN suspend_reason VARCHAR(500) AFTER suspended;
