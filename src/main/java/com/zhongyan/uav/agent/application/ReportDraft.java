package com.zhongyan.uav.agent.application;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record ReportDraft(
        String draftId,
        String title,
        String reportType,
        String status,
        String markdown,
        Map<String, Object> structured,
        List<ReportCitation> citations,
        Instant createdAt) {
    public ReportDraft {
        structured = structured == null ? Map.of() : Map.copyOf(structured);
        citations = citations == null ? List.of() : List.copyOf(citations);
    }
}
