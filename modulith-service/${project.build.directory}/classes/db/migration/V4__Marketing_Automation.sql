CREATE TABLE marketing_logs (
    log_id BIGSERIAL PRIMARY KEY,
    user_email VARCHAR(255) NOT NULL,
    campaign_name VARCHAR(100) NOT NULL,
    sent_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    reference_id VARCHAR(100), -- e.g. cart_id or product_id
    status VARCHAR(50) DEFAULT 'SENT'
);

CREATE INDEX idx_marketing_user_campaign ON marketing_logs(user_email, campaign_name);
