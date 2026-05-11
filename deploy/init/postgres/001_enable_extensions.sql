CREATE EXTENSION IF NOT EXISTS postgis;
CREATE EXTENSION IF NOT EXISTS vector;

COMMENT ON EXTENSION postgis IS 'PostGIS spatial types and indexes for UAV geo data';
COMMENT ON EXTENSION vector IS 'pgvector embeddings for agent RAG retrieval';
