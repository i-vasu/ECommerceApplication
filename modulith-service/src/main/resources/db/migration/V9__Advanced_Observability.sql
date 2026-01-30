-- Advanced Observability: Error Indexing and Alerting

CREATE TABLE indexed_logs (
    log_id BIGSERIAL PRIMARY KEY,
    timestamp TIMESTAMP NOT NULL,
    level VARCHAR(10) NOT NULL,
    logger VARCHAR(255),
    message TEXT,
    trace_id VARCHAR(64),
    span_id VARCHAR(64),
    metadata JSONB DEFAULT '{}'::jsonb,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_logs_trace ON indexed_logs(trace_id);
CREATE INDEX idx_logs_level ON indexed_logs(level);
CREATE INDEX idx_logs_timestamp ON indexed_logs(timestamp);

CREATE TABLE monitoring_alert_rules (
    rule_id SERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    metric_name VARCHAR(255) NOT NULL, -- e.g., 'system.cpu.usage'
    threshold DOUBLE PRECISION NOT NULL,
    comparison_operator VARCHAR(10) NOT NULL, -- '>', '<', '>='
    duration_minutes INT DEFAULT 5,
    is_active BOOLEAN DEFAULT TRUE,
    last_triggered_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE monitoring_alert_history (
    history_id BIGSERIAL PRIMARY KEY,
    rule_id INT REFERENCES monitoring_alert_rules(rule_id),
    actual_value DOUBLE PRECISION,
    message TEXT,
    triggered_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
