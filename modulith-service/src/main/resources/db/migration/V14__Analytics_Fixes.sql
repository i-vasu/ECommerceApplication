-- V14 Fixes for Analytics Dashboard
-- 1. Create missing operational_audit_logs table
CREATE TABLE IF NOT EXISTS operational_audit_logs (
    id BIGSERIAL PRIMARY KEY,
    type VARCHAR(255),
    category VARCHAR(255),
    entity_id VARCHAR(255),
    detail TEXT,
    success BOOLEAN,
    result VARCHAR(255),
    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_audit_timestamp ON operational_audit_logs(timestamp);
CREATE INDEX IF NOT EXISTS idx_audit_category ON operational_audit_logs(category);

-- 2. Fix user_traits table foreign key
-- It was incorrectly referencing 'users' table which was renamed/refactored to 'app_users'
ALTER TABLE user_traits DROP CONSTRAINT IF EXISTS user_traits_email_fkey;
ALTER TABLE user_traits ADD CONSTRAINT fk_user_traits_user FOREIGN KEY (email) REFERENCES app_users(email) ON DELETE CASCADE;
