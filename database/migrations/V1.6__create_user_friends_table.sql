-- Migration: Create user_friends table for social features
-- Version: 1.6
-- Date: 2026-02-14

CREATE TABLE IF NOT EXISTS user_friends (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    friend_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    accepted_at TIMESTAMP,
    
    -- Foreign key constraints
    CONSTRAINT fk_user_friends_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    CONSTRAINT fk_user_friends_friend FOREIGN KEY (friend_id) REFERENCES users(user_id) ON DELETE CASCADE,
    
    -- Unique constraint to prevent duplicate friendships
    CONSTRAINT uk_user_friend_pair UNIQUE (user_id, friend_id),
    
    -- Check constraint to prevent self-friending
    CONSTRAINT chk_no_self_friend CHECK (user_id != friend_id)
);

-- Indexes for efficient friend lookups
CREATE INDEX idx_user_friends_user ON user_friends(user_id) WHERE status = 'ACCEPTED';
CREATE INDEX idx_user_friends_friend ON user_friends(friend_id) WHERE status = 'ACCEPTED';
CREATE INDEX idx_user_friends_status ON user_friends(status);

-- Comments for documentation
COMMENT ON TABLE user_friends IS 'Stores user friendship relationships for social features';
COMMENT ON COLUMN user_friends.status IS 'Friendship status: PENDING, ACCEPTED, or BLOCKED';
COMMENT ON COLUMN user_friends.accepted_at IS 'Timestamp when friendship was accepted';
