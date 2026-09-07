-- Enables vector-first candidate matching (see AiService.findCandidatesForMatching).
-- Requires the pgvector extension to be installed on the Postgres server
-- (the official `pgvector/pgvector:pg16` Docker image ships it out of the box).
CREATE EXTENSION IF NOT EXISTS vector;

-- text-embedding-3-small produces 1536-dimensional vectors (see application.yml
-- spring.ai.openai.embedding.options.model). These columns are intentionally NOT
-- mapped on the JPA entities: Hibernate has no built-in understanding of the
-- `vector` type, so all reads/writes go through EmbeddingService via JdbcTemplate
-- to avoid fighting hibernate.hbm2ddl on every startup.
ALTER TABLE resumes ADD COLUMN embedding vector(1536);
ALTER TABLE job_postings ADD COLUMN embedding vector(1536);

-- IVFFlat index for fast approximate cosine-distance search once there's enough
-- data to make an index worthwhile; harmless (just unused) on small tables.
CREATE INDEX IF NOT EXISTS idx_resumes_embedding ON resumes USING ivfflat (embedding vector_cosine_ops) WITH (lists = 100);
CREATE INDEX IF NOT EXISTS idx_job_postings_embedding ON job_postings USING ivfflat (embedding vector_cosine_ops) WITH (lists = 100);
