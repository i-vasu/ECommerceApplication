-- Metric Snapshotting: For Trend Analysis (Grafana Plot Replacement)

CREATE TABLE monitoring_metrics_history (
    snapshot_id BIGSERIAL PRIMARY KEY,
    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    metric_name VARCHAR(255) NOT NULL,
    metric_value DOUBLE PRECISION NOT NULL,
    metadata JSONB DEFAULT '{}'::jsonb
) PARTITION BY RANGE (timestamp);

-- Create Partitions for 2026 (6 months)
CREATE TABLE monitoring_metrics_y2026_m01 PARTITION OF monitoring_metrics_history
    FOR VALUES FROM ('2026-01-01') TO ('2026-02-01');
CREATE TABLE monitoring_metrics_y2026_m02 PARTITION OF monitoring_metrics_history
    FOR VALUES FROM ('2026-02-01') TO ('2026-03-01');
CREATE TABLE monitoring_metrics_y2026_m03 PARTITION OF monitoring_metrics_history
    FOR VALUES FROM ('2026-03-01') TO ('2026-04-01');
CREATE TABLE monitoring_metrics_y2026_m04 PARTITION OF monitoring_metrics_history
    FOR VALUES FROM ('2026-04-01') TO ('2026-05-01');
CREATE TABLE monitoring_metrics_y2026_m05 PARTITION OF monitoring_metrics_history
    FOR VALUES FROM ('2026-05-01') TO ('2026-06-01');
CREATE TABLE monitoring_metrics_y2026_m06 PARTITION OF monitoring_metrics_history
    FOR VALUES FROM ('2026-06-01') TO ('2026-07-01');

CREATE TABLE monitoring_metrics_default PARTITION OF monitoring_metrics_history DEFAULT;

CREATE INDEX idx_metrics_name_time ON monitoring_metrics_history(metric_name, timestamp DESC);
