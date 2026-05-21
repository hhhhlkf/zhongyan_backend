package com.zhongyan.uav.agent.application;

import com.zhongyan.uav.agent.port.RagDocument;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Agent 报告草稿应用服务。
 * <p>
 * 根据 Mission、Task、资产、处理结果、日志和 RAG 来源生成 Markdown 与结构化 JSON 草稿；
 * 该服务只生成待确认草稿，正式报告文件写入仍由后续报告生成任务完成。
 */
public class ReportDraftService {
    private final Clock clock;

    public ReportDraftService() {
        this(Clock.systemUTC());
    }

    public ReportDraftService(Clock clock) {
        this.clock = clock;
    }

    /**
     * 兼容旧调用的 Markdown 草稿入口。所有来源都会转换为引用，便于前端继续展示证据链。
     */
    public String draftMarkdown(String title, List<RagDocument> sources) {
        return generate(new ReportDraftRequest(title, "MISSION_SUMMARY", null, null,
                null, List.of(), List.of(), List.of(), List.of(), List.of(), sources)).markdown();
    }

    /**
     * 生成结构化报告草稿。草稿只用于人工确认，正式报告文件仍需后续任务写入对象存储和 Asset 元数据。
     */
    public ReportDraft generate(ReportDraftRequest request) {
        ReportDraftRequest effective = request == null ? ReportDraftRequest.empty() : request;
        List<ReportCitation> citations = citations(effective.sources());
        Map<String, Object> structured = structured(effective, citations);
        String markdown = markdown(effective, citations);
        return new ReportDraft("report-draft-" + UUID.randomUUID(),
                defaultText(effective.title(), "Agent Report Draft"),
                defaultText(effective.reportType(), "MISSION_SUMMARY"),
                "DRAFT_REQUIRES_CONFIRMATION", markdown, structured, citations, clock.instant());
    }

    private String markdown(ReportDraftRequest request, List<ReportCitation> citations) {
        StringBuilder builder = new StringBuilder();
        builder.append("# ").append(defaultText(request.title(), "Agent Report Draft")).append('\n');
        builder.append('\n').append("- 报告类型：").append(defaultText(request.reportType(), "MISSION_SUMMARY"));
        appendOptional(builder, "Mission", request.missionId());
        appendOptional(builder, "Task", request.taskId());
        builder.append('\n').append("- 草稿状态：待人工确认");
        builder.append('\n').append("- 生成时间：").append(clock.instant()).append('\n');

        appendSection(builder, "任务概览", request.missionSummary(), firstCitation(citations));
        appendListSection(builder, "任务时间线", request.taskTimeline(), citations);
        appendListSection(builder, "资产清单", request.assetSummaries(), citations);
        appendListSection(builder, "处理结果", request.processingResults(), citations);
        appendListSection(builder, "传输与日志", request.transferLogs(), citations);
        appendListSection(builder, "Agent 分析结论", request.agentFindings(), citations);

        builder.append('\n').append("## 引用来源").append('\n');
        if (citations.isEmpty()) {
            builder.append("- 暂无引用来源，当前内容只能作为待补证据的草稿。").append('\n');
        } else {
            for (ReportCitation citation : citations) {
                builder.append("- [").append(citation.citationId()).append("] ")
                        .append(citation.sourceType()).append(" / ")
                        .append(defaultText(citation.sourceId(), "-")).append("：")
                        .append(defaultText(citation.title(), "-"));
                if (!defaultText(citation.snippet(), "").isBlank()) {
                    builder.append("，摘要：").append(citation.snippet());
                }
                builder.append('\n');
            }
        }

        builder.append('\n').append("## 确认说明").append('\n');
        builder.append("本报告为 Agent 草稿。正式报告文件必须经用户确认后，再由 REPORT_GENERATION Task 写入对象存储并生成 Asset 元数据。").append('\n');
        return builder.toString();
    }

    private Map<String, Object> structured(ReportDraftRequest request, List<ReportCitation> citations) {
        Map<String, Object> structured = new LinkedHashMap<>();
        structured.put("missionId", defaultText(request.missionId(), ""));
        structured.put("taskId", defaultText(request.taskId(), ""));
        structured.put("missionSummary", defaultText(request.missionSummary(), ""));
        structured.put("taskTimeline", safeList(request.taskTimeline()));
        structured.put("assetSummaries", safeList(request.assetSummaries()));
        structured.put("processingResults", safeList(request.processingResults()));
        structured.put("transferLogs", safeList(request.transferLogs()));
        structured.put("agentFindings", safeList(request.agentFindings()));
        structured.put("citations", citations.stream().map(ReportCitation::toMap).toList());
        structured.put("confirmationRequired", true);
        return structured;
    }

    private List<ReportCitation> citations(List<RagDocument> sources) {
        List<RagDocument> safeSources = sources == null ? List.of() : sources;
        List<ReportCitation> citations = new ArrayList<>();
        for (int index = 0; index < safeSources.size(); index++) {
            RagDocument source = safeSources.get(index);
            citations.add(new ReportCitation("S" + (index + 1), source.documentId(), source.sourceType(),
                    source.sourceId(), source.title(), source.snippet()));
        }
        return citations;
    }

    private void appendSection(StringBuilder builder, String title, String content, String citation) {
        builder.append('\n').append("## ").append(title).append('\n');
        if (content == null || content.isBlank()) {
            builder.append("- 暂无明确内容。");
        } else {
            builder.append("- ").append(content.trim());
        }
        if (citation != null) {
            builder.append(" [").append(citation).append("]");
        }
        builder.append('\n');
    }

    private void appendListSection(StringBuilder builder, String title, List<String> values,
                                   List<ReportCitation> citations) {
        builder.append('\n').append("## ").append(title).append('\n');
        List<String> safeValues = safeList(values);
        if (safeValues.isEmpty()) {
            builder.append("- 暂无明确内容。").append('\n');
            return;
        }
        for (int index = 0; index < safeValues.size(); index++) {
            builder.append("- ").append(safeValues.get(index));
            String citation = citationAt(citations, index);
            if (citation != null) {
                builder.append(" [").append(citation).append("]");
            }
            builder.append('\n');
        }
    }

    private void appendOptional(StringBuilder builder, String label, String value) {
        if (value != null && !value.isBlank()) {
            builder.append('\n').append("- ").append(label).append("：").append(value);
        }
    }

    private String firstCitation(List<ReportCitation> citations) {
        return citations.isEmpty() ? null : citations.get(0).citationId();
    }

    private String citationAt(List<ReportCitation> citations, int index) {
        return citations.isEmpty() ? null : citations.get(Math.min(index, citations.size() - 1)).citationId();
    }

    private List<String> safeList(List<String> values) {
        return values == null ? List.of() : values.stream()
                .filter(value -> value != null && !value.isBlank())
                .collect(Collectors.toList());
    }

    private static String defaultText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
