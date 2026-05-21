package com.zhongyan.uav.agent.port;

import java.util.List;
import java.util.Map;

/**
 * Boundary for vector retrieval and document indexing. Implementations may use pgvector or in-memory fixtures.
 */
public interface VectorStorePort {
    List<RagDocument> search(String query, int limit, Map<String, Object> filters);

    default List<RagDocument> search(String query, int limit, Map<String, Object> filters,
                                     List<Double> queryEmbedding) {
        return search(query, limit, filters);
    }

    void upsert(RagDocument document);

    default void upsert(RagDocument document, List<Double> embedding) {
        upsert(document);
    }
}
