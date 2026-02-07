-- V28: Timestamp Timezone Upgrade
-- Purpose: Upgrade timestamp columns to timestamptz for proper timezone handling

-- ====================================================================================
-- 1. UPGRADE TIMESTAMP COLUMNS TO TIMESTAMPTZ
-- ====================================================================================
-- Convert all timestamp columns to timestamptz (storing in UTC)
ALTER TABLE accounts
    ALTER COLUMN created_at TYPE TIMESTAMPTZ USING created_at AT TIME ZONE 'UTC',
    ALTER COLUMN updated_at TYPE TIMESTAMPTZ USING updated_at AT TIME ZONE 'UTC',
    ALTER COLUMN deleted_at TYPE TIMESTAMPTZ USING deleted_at AT TIME ZONE 'UTC';

-- Update default values to use timezone-aware NOW()
ALTER TABLE accounts
    ALTER COLUMN created_at SET DEFAULT NOW(),
    ALTER COLUMN updated_at SET DEFAULT NOW();

COMMENT ON COLUMN accounts.created_at IS
'Timestamp when account was created (stored in UTC, displayed in user timezone)';
COMMENT ON COLUMN accounts.updated_at IS
'Timestamp when account was last updated (stored in UTC, auto-updated by trigger)';
COMMENT ON COLUMN accounts.deleted_at IS
'Timestamp when account was soft-deleted (stored in UTC)';

-- ====================================================================================
-- 2. CREATE TRIGGER FOR AUTO-UPDATE TIMESTAMP
-- ====================================================================================
-- Create function to auto-update updated_at
CREATE OR REPLACE FUNCTION update_accounts_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Create trigger
CREATE TRIGGER trg_accounts_updated_at
    BEFORE UPDATE ON accounts
    FOR EACH ROW
    EXECUTE FUNCTION update_accounts_updated_at();

COMMENT ON FUNCTION update_accounts_updated_at() IS
'Automatically updates the updated_at column to current timestamp on every UPDATE';

-- ====================================================================================
-- 3. UPGRADE LOOKUP TABLE TIMESTAMPS
-- ====================================================================================
-- Upgrade account_types table
ALTER TABLE account_types
    ALTER COLUMN created_at TYPE TIMESTAMPTZ USING created_at AT TIME ZONE 'UTC',
    ALTER COLUMN updated_at TYPE TIMESTAMPTZ USING updated_at AT TIME ZONE 'UTC';

ALTER TABLE account_types
    ALTER COLUMN created_at SET DEFAULT NOW(),
    ALTER COLUMN updated_at SET DEFAULT NOW();

-- Create trigger for account_types
CREATE TRIGGER trg_account_types_updated_at
    BEFORE UPDATE ON account_types
    FOR EACH ROW
    EXECUTE FUNCTION update_accounts_updated_at();

-- Upgrade currencies table
ALTER TABLE currencies
    ALTER COLUMN created_at TYPE TIMESTAMPTZ USING created_at AT TIME ZONE 'UTC';

ALTER TABLE currencies
    ALTER COLUMN created_at SET DEFAULT NOW();
