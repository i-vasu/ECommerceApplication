-- Attribution and Preference Center
ALTER TABLE users ADD COLUMN IF NOT EXISTS unsubscribed BOOLEAN DEFAULT FALSE;

CREATE TABLE campaign_links (
    link_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    original_url TEXT NOT NULL,
    campaign_name VARCHAR(100) NOT NULL,
    user_email VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE campaign_interactions (
    interaction_id BIGSERIAL PRIMARY KEY,
    link_id UUID REFERENCES campaign_links(link_id),
    interaction_type VARCHAR(20), -- 'OPEN', 'CLICK'
    ip_address VARCHAR(45),
    user_agent TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Update orders to track attribution
ALTER TABLE orders ADD COLUMN IF NOT EXISTS attributed_campaign VARCHAR(100);

-- Campaign Rules/Settings table
CREATE TABLE marketing_campaign_settings (
    campaign_id SERIAL PRIMARY KEY,
    campaign_name VARCHAR(100) UNIQUE NOT NULL,
    is_active BOOLEAN DEFAULT TRUE,
    subject_line VARCHAR(255),
    subject_line_b VARCHAR(255),
    frequency_cap_days INT DEFAULT 3,
    min_cart_value DECIMAL(10,2) DEFAULT 0,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO marketing_campaign_settings (campaign_name, subject_line, frequency_cap_days)
VALUES ('ABANDONED_CART_RECOVERY', 'You left items in your cart!', 3);
