-- V33: Add User Roles
-- Purpose: Support Role-Based Access Control (RBAC)

ALTER TABLE users
    ADD COLUMN role VARCHAR(20) NOT NULL DEFAULT 'USER';

-- Optional: Seed an admin user if needed, but for now we just want the structure.
-- We can manually upgrade a user to ADMIN via SQL for testing.
