CREATE EXTENSION IF NOT EXISTS vector;

ALTER TABLE content ADD COLUMN IF NOT EXISTS embedding vector(8);
ALTER TABLE app_user ADD COLUMN IF NOT EXISTS embedding vector(8);

CREATE INDEX IF NOT EXISTS ix_content_embedding_hnsw
    ON content USING hnsw (embedding vector_cosine_ops)
    WHERE embedding IS NOT NULL;

CREATE TABLE IF NOT EXISTS rating_event_outbox (
    event_id UUID PRIMARY KEY,
    rating_id BIGINT NOT NULL REFERENCES rating(id) ON DELETE CASCADE,
    payload TEXT NOT NULL,
    occurred_at TIMESTAMPTZ NOT NULL,
    published_at TIMESTAMPTZ
);

