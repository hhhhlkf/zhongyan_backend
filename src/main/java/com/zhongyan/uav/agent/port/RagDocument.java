package com.zhongyan.uav.agent.port;

import java.util.Map;

public record RagDocument(
        String documentId,
        String sourceType,
        String sourceId,
        String title,
        String content,
        String snippet,
        Map<String, Object> metadata) {
    public RagDocument {
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }
}
