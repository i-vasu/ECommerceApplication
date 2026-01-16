-- Enable ParadeDB search extension
CREATE EXTENSION IF NOT EXISTS pg_search;

-- Create BM25 search index for products
CALL paradedb.create_bm25(
    index_name => 'products_search_idx',
    table_name => 'products',
    key_field => 'id',
    text_fields => paradedb.field('name', tokenizer => paradedb.tokenizer('en_stem')) ||
                   paradedb.field('description', tokenizer => paradedb.tokenizer('en_stem')) ||
                   paradedb.field('category'),
    numeric_fields => paradedb.field('price') ||
                      paradedb.field('stock_quantity')
);

-- Index will auto-update on INSERT/UPDATE/DELETE
-- No manual sync needed!

COMMENT ON EXTENSION pg_search IS 'ParadeDB full-text search with BM25 ranking';
