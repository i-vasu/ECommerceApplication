-- 1. Create User Profile Table
CREATE TABLE IF NOT EXISTS user_profiles (
    profile_id BIGSERIAL PRIMARY KEY,
    user_id BIGINT UNIQUE,
    first_name VARCHAR(255),
    last_name VARCHAR(255),
    mobile_number VARCHAR(20) UNIQUE,
    avatar_url VARCHAR(255),
    date_of_birth DATE,
    gender VARCHAR(50),
    CONSTRAINT fk_profile_user FOREIGN KEY (user_id) REFERENCES app_users(user_id)
);

-- 2. Create User Loyalty Table
CREATE TABLE IF NOT EXISTS user_loyalty (
    loyalty_id BIGSERIAL PRIMARY KEY,
    user_id BIGINT UNIQUE,
    reward_points INTEGER DEFAULT 0,
    customer_group VARCHAR(50) DEFAULT 'RETAIL',
    CONSTRAINT fk_loyalty_user FOREIGN KEY (user_id) REFERENCES app_users(user_id)
);

-- 3. Create join tables for Profile <-> Address (if not exists, migrating form User <-> Address logic)
CREATE TABLE IF NOT EXISTS user_profile_address (
    profile_id BIGINT,
    address_id BIGINT,
    PRIMARY KEY (profile_id, address_id),
    CONSTRAINT fk_upa_profile FOREIGN KEY (profile_id) REFERENCES user_profiles(profile_id),
    CONSTRAINT fk_upa_address FOREIGN KEY (address_id) REFERENCES addresses(address_id)
);

-- 4. Migrate Data
-- Migrate Profile Data
INSERT INTO user_profiles (user_id, first_name, last_name, mobile_number, avatar_url, date_of_birth, gender)
SELECT user_id, first_name, last_name, mobile_number, avatar_url, date_of_birth, gender 
FROM app_users
ON CONFLICT (user_id) DO NOTHING;

-- Migrate Loyalty Data
INSERT INTO user_loyalty (user_id, reward_points, customer_group)
SELECT user_id, COALESCE(reward_points, 0), COALESCE(customer_group, 'RETAIL')
FROM app_users
ON CONFLICT (user_id) DO NOTHING;

-- Migrate Addresses (This is tricky as link was user_id -> address_id, now profile_id -> address_id)
-- We need to link profile_id via user_id
INSERT INTO user_profile_address (profile_id, address_id)
SELECT p.profile_id, ua.address_id
FROM user_address ua
JOIN user_profiles p ON p.user_id = ua.user_id
ON CONFLICT DO NOTHING;

-- 5. Cleanup app_users (Optional/Commented out for safety initially, but required for clean entity mapping)
-- ALTER TABLE app_users DROP COLUMN first_name;
-- ALTER TABLE app_users DROP COLUMN last_name;
-- ALTER TABLE app_users DROP COLUMN mobile_number;
-- ALTER TABLE app_users DROP COLUMN avatar_url;
-- ALTER TABLE app_users DROP COLUMN date_of_birth;
-- ALTER TABLE app_users DROP COLUMN gender;
-- ALTER TABLE app_users DROP COLUMN reward_points;
-- ALTER TABLE app_users DROP COLUMN customer_group;
-- DROP TABLE user_address; 
