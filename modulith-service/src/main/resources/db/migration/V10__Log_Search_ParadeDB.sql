-- ParadeDB Integration: Turning Logs into Searchable Intelligence

CREATE EXTENSION IF NOT EXISTS pg_search CASCADE;

-- Create the BM25 search index on our existing indexed_logs table.
-- This allows sub-second full-text search across millions of log entries.
-- We index the message for full-text search, and level/logger as keywords for filtering.
CALL search.create_bm25(
  index_name => 'logs_search_idx',
  table_name => 'indexed_logs',
  text_fields => '{message: {}}',
  keyword_fields => '{level: {}, logger: {}, trace_id: {}}'
);
