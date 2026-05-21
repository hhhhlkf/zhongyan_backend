package com.zhongyan.uav.agent.application;

import java.util.Map;

public record ReportCitation(
        String citationId,
        String documentId,
        String sourceType,
        String sourceId,
        String title,
        String snippet) {
    public Map<String, Object> toMap() {
        return Map.of(
                "citationId", defaultText(citationId),
                "documentId", defaultText(documentId),
                "sourceType", defaultText(sourceType),
                "sourceId", defaultText(sourceId),
                "title", defaultText(title),
                "snippet", defaultText(snippet));
    }

    private static String defaultText(String value) {
        return value == null ? "" : value;
    }
}
