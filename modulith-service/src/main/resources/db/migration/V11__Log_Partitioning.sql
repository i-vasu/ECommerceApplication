-- Log Partitioning: Extreme Performance & Scalability (Zero RAM Overhead)

-- We drop and recreate to establish a clean partitioned structure
DROP TABLE IF EXISTS indexed_logs CASCADE;

CREATE TABLE indexed_logs (
    log_id BIGSERIAL,
    timestamp TIMESTAMP NOT NULL,
    level VARCHAR(10) NOT NULL,
    logger VARCHAR(255),
    message TEXT,
    trace_id VARCHAR(64),
    span_id VARCHAR(64),
    metadata JSONB DEFAULT '{}'::jsonb,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (log_id, timestamp) -- Partition key must be part of PK
) PARTITION BY RANGE (timestamp);

-- 1. Create Default Partition (Insurance against missed ranges)
CREATE TABLE indexed_logs_default PARTITION OF indexed_logs DEFAULT;

-- 2. Create Initial Range Partitions for 2026
-- This keeps indexes small and queries super fast even with millions of logs
CREATE TABLE indexed_logs_y2026_m01 PARTITION OF indexed_logs
    FOR VALUES FROM ('2026-01-01') TO ('2026-02-01');
CREATE TABLE indexed_logs_y2026_m02 PARTITION OF indexed_logs
    FOR VALUES FROM ('2026-02-01') TO ('2026-03-01');
CREATE TABLE indexed_logs_y2026_m03 PARTITION OF indexed_logs
    FOR VALUES FROM ('2026-03-01') TO ('2026-04-01');

-- 3. Global Indices on the Root Table (Automatically inherited by partitions)
CREATE INDEX idx_logs_trace ON indexed_logs(trace_id);
CREATE INDEX idx_logs_level ON indexed_logs(level);

-- 4. Re-apply ParadeDB's BM25 Search Engine
-- pg_search handles partitioned tables seamlessly
CALL search.create_bm25(
  index_name => 'logs_search_idx',
  table_name => 'indexed_logs',
  text_fields => '{message: {}}',
  keyword_fields => '{level: {}, logger: {}, trace_id: {}}'
);
