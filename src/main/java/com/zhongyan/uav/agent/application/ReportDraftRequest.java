package com.zhongyan.uav.agent.application;

import com.zhongyan.uav.agent.port.RagDocument;

import java.util.List;
import java.util.Objects;

public record ReportDraftRequest(
        String title,
        String reportType,
        String missionId,
        String taskId,
        String missionSummary,
        List<String> taskTimeline,
        List<String> assetSummaries,
        List<String> processingResults,
        List<String> transferLogs,
        List<String> agentFindings,
        List<RagDocument> sources) {
    public ReportDraftRequest {
        taskTimeline = safeStrings(taskTimeline);
        assetSummaries = safeStrings(assetSummaries);
        processingResults = safeStrings(processingResults);
        transferLogs = safeStrings(transferLogs);
        agentFindings = safeStrings(agentFindings);
        sources = sources == null ? List.of() : sources.stream().filter(Objects::nonNull).toList();
    }

    public static ReportDraftRequest empty() {
        return new ReportDraftRequest(null, null, null, null, null,
                List.of(), List.of(), List.of(), List.of(), List.of(), List.of());
    }

    private static List<String> safeStrings(List<String> values) {
        return values == null ? List.of() : values.stream()
                .filter(value -> value != null && !value.isBlank())
                .toList();
    }
}
