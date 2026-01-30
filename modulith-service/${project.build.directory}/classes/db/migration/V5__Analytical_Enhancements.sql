CREATE TABLE activity_logs (
    activity_id BIGSERIAL PRIMARY KEY,
    user_email VARCHAR(255),
    activity_type VARCHAR(50) NOT NULL,
    metadata JSONB, -- stores details like keyword, product_id, price
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_activity_type_time ON activity_logs(activity_type, created_at);

-- Enhanced Analytics Views
CREATE OR REPLACE VIEW view_search_performance AS
SELECT 
    metadata->>'keyword' as keyword,
    COUNT(*) as search_count,
    (metadata->>'result_count')::int as last_result_count,
    MAX(created_at) as last_searched
FROM activity_logs
WHERE activity_type = 'SEARCH'
GROUP BY metadata->>'keyword', (metadata->>'result_count')::int
ORDER BY search_count DESC;

CREATE OR REPLACE VIEW view_user_growth AS
SELECT 
    created_at::date as reg_date,
    COUNT(*) as new_users
FROM users
GROUP BY created_at::date
ORDER BY reg_date DESC;
