package com.zhongyan.uav.agent.infrastructure;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhongyan.uav.agent.port.RagDocument;
import com.zhongyan.uav.agent.port.VectorStorePort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

public class PgVectorStoreAdapter implements VectorStorePort {
    private final List<RagDocument> documents = new CopyOnWriteArrayList<>();
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public PgVectorStoreAdapter() {
        this(null, new ObjectMapper());
    }

    public PgVectorStoreAdapter(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public List<RagDocument> search(String query, int limit, Map<String, Object> filters) {
        if (jdbcTemplate != null) {
            return jdbcSearch(query, limit, filters, List.of());
        }
        int effectiveLimit = limit <= 0 ? 10 : limit;
        return documents.stream()
                .filter(document -> matchesFilters(document, filters))
                .filter(document -> isBlank(query) || score(document, query) > 0)
                .sorted((left, right) -> {
                    int byScore = Integer.compare(score(right, query), score(left, query));
                    return byScore != 0 ? byScore : left.documentId().compareTo(right.documentId());
                })
                .limit(effectiveLimit)
                .toList();
    }

    @Override
    public List<RagDocument> search(String query, int limit, Map<String, Object> filters,
                                    List<Double> queryEmbedding) {
        if (jdbcTemplate != null) {
            return jdbcSearch(query, limit, filters, queryEmbedding);
        }
        return search(query, limit, filters);
    }

    @Override
    public void upsert(RagDocument document) {
        if (jdbcTemplate != null) {
            jdbcUpsert(document, List.of());
            return;
        }
        documents.removeIf(existing -> existing.documentId().equals(document.documentId()));
        documents.add(document);
    }

    @Override
    public void upsert(RagDocument document, List<Double> embedding) {
        if (jdbcTemplate != null) {
            jdbcUpsert(document, embedding);
            return;
        }
        upsert(document);
    }

    private void jdbcUpsert(RagDocument document, List<Double> embedding) {
        try {
            Map<String, Object> metadata = new LinkedHashMap<>(document.metadata());
            metadata.putIfAbsent("snippet", document.snippet());
            jdbcTemplate.update("""
                            INSERT INTO agent_knowledge_document
                            (document_id, source_type, source_id, title, content, metadata, embedding, created_at, updated_at)
                            VALUES (?, ?, ?, ?, ?, ?::jsonb, ?::vector, now(), now())
                            ON CONFLICT (document_id) DO UPDATE SET
                              source_type = EXCLUDED.source_type,
                              source_id = EXCLUDED.source_id,
                              title = EXCLUDED.title,
                              content = EXCLUDED.content,
                              metadata = EXCLUDED.metadata,
                              embedding = EXCLUDED.embedding,
                              updated_at = now()
                            """,
                    document.documentId(),
                    document.sourceType(),
                    document.sourceId(),
                    document.title(),
                    document.content(),
                    objectMapper.writeValueAsString(metadata),
                    vectorLiteral(embedding));
        } catch (Exception exception) {
            throw new IllegalStateException("failed to upsert pgvector RAG document: " + document.documentId(),
                    exception);
        }
    }

    private List<RagDocument> jdbcSearch(String query, int limit, Map<String, Object> filters,
                                         List<Double> queryEmbedding) {
        int effectiveLimit = limit <= 0 ? 10 : limit;
        List<Object> params = new ArrayList<>();
        StringBuilder sql = new StringBuilder("""
                SELECT document_id, source_type, source_id, title, content, metadata
                FROM agent_knowledge_document
                WHERE 1 = 1
                """);
        appendFilters(sql, params, filters);
        if (!isBlank(query) && (queryEmbedding == null || queryEmbedding.isEmpty())) {
            sql.append(" AND (title ILIKE ? OR content ILIKE ?)");
            String pattern = "%" + query + "%";
            params.add(pattern);
            params.add(pattern);
        }
        if (queryEmbedding != null && !queryEmbedding.isEmpty()) {
            sql.append(" ORDER BY embedding <=> ?::vector, updated_at DESC");
            params.add(vectorLiteral(queryEmbedding));
        } else {
            sql.append(" ORDER BY updated_at DESC");
        }
        sql.append(" LIMIT ?");
        params.add(effectiveLimit);
        return jdbcTemplate.query(sql.toString(), ragMapper(), params.toArray());
    }

    private void appendFilters(StringBuilder sql, List<Object> params, Map<String, Object> filters) {
        if (filters == null || filters.isEmpty()) {
            return;
        }
        filters.forEach((key, expected) -> {
            if (expected == null || expected instanceof String text && text.isBlank()) {
                return;
            }
            switch (key) {
                case "documentId" -> {
                    sql.append(" AND document_id = ?");
                    params.add(String.valueOf(expected));
                }
                case "sourceType" -> {
                    sql.append(" AND source_type = ?");
                    params.add(String.valueOf(expected));
                }
                case "sourceId" -> {
                    sql.append(" AND source_id = ?");
                    params.add(String.valueOf(expected));
                }
                default -> {
                    if (key.matches("[A-Za-z0-9_.-]{1,64}")) {
                        sql.append(" AND metadata ->> CAST(? AS text) = ?");
                        params.add(key);
                        params.add(String.valueOf(expected));
                    }
                }
            }
        });
    }

    private RowMapper<RagDocument> ragMapper() {
        return (rs, rowNum) -> {
            Map<String, Object> metadata = Optional.ofNullable(rs.getString("metadata"))
                    .filter(value -> !value.isBlank())
                    .map(this::readMetadata)
                    .orElse(Map.of());
            Object snippet = metadata.get("snippet");
            return new RagDocument(rs.getString("document_id"),
                    rs.getString("source_type"),
                    rs.getString("source_id"),
                    rs.getString("title"),
                    rs.getString("content"),
                    snippet == null ? rs.getString("content") : String.valueOf(snippet),
                    metadata);
        };
    }

    private Map<String, Object> readMetadata(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<>() {
            });
        } catch (Exception exception) {
            return Map.of();
        }
    }

    private String vectorLiteral(List<Double> values) {
        if (values == null || values.isEmpty()) {
            return null;
        }
        return values.stream()
                .map(value -> Double.toString(value == null ? 0.0D : value))
                .collect(java.util.stream.Collectors.joining(",", "[", "]"));
    }

    private static boolean matchesFilters(RagDocument document, Map<String, Object> filters) {
        if (filters == null || filters.isEmpty()) {
            return true;
        }
        for (Map.Entry<String, Object> entry : filters.entrySet()) {
            String key = entry.getKey();
            Object expected = entry.getValue();
            if (expected == null || expected instanceof String text && text.isBlank()) {
                continue;
            }
            Object actual = switch (key) {
                case "documentId" -> document.documentId();
                case "sourceType" -> document.sourceType();
                case "sourceId" -> document.sourceId();
                default -> document.metadata().get(key);
            };
            if (actual == null || !String.valueOf(actual).equalsIgnoreCase(String.valueOf(expected))) {
                return false;
            }
        }
        return true;
    }

    private static int score(RagDocument document, String query) {
        if (isBlank(query)) {
            return 1;
        }
        List<String> terms = Arrays.stream(query.toLowerCase(Locale.ROOT).split("\\s+"))
                .filter(term -> !term.isBlank())
                .toList();
        String title = lower(document.title());
        String snippet = lower(document.snippet());
        String content = lower(document.content());
        int score = 0;
        for (String term : terms) {
            if (title.contains(term)) {
                score += 8;
            }
            if (snippet.contains(term)) {
                score += 4;
            }
            if (content.contains(term)) {
                score += 1;
            }
        }
        return score;
    }

    private static String lower(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
