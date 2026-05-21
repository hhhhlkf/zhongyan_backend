package com.zhongyan.uav.agent.infrastructure.tool;

import com.zhongyan.uav.agent.application.RagService;
import com.zhongyan.uav.agent.application.ReportDraft;
import com.zhongyan.uav.agent.application.ReportDraftRequest;
import com.zhongyan.uav.agent.application.ReportDraftService;
import com.zhongyan.uav.agent.domain.ToolRiskLevel;
import com.zhongyan.uav.agent.port.AgentToolContext;
import com.zhongyan.uav.agent.port.AgentToolInputSchema;
import com.zhongyan.uav.agent.port.AgentToolResult;
import com.zhongyan.uav.agent.port.RagDocument;
import com.zhongyan.uav.task.application.CreateTaskInput;
import com.zhongyan.uav.task.application.TaskApplicationService;
import com.zhongyan.uav.task.domain.Task;
import com.zhongyan.uav.task.domain.TaskType;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Agent report tool that builds a Markdown draft and keeps every finding tied to RAG citations.
 */
public class ReportGenerateDraftTool extends AbstractAgentTool {
    private final ReportDraftService reportDraftService;
    private final RagService ragService;
    private final TaskApplicationService taskApplicationService;

    public ReportGenerateDraftTool() {
        this(null, null, null);
    }

    public ReportGenerateDraftTool(ReportDraftService reportDraftService, RagService ragService) {
        this(reportDraftService, ragService, null);
    }

    public ReportGenerateDraftTool(ReportDraftService reportDraftService, RagService ragService,
                                   TaskApplicationService taskApplicationService) {
        super("report.generateDraft", "Generate a report draft with source references.",
                AgentToolInputSchema.of(Set.of("missionId"), Map.of(
                        "missionId", "Mission identifier for the report draft.",
                        "taskId", "Optional task identifier for a focused report.",
                        "reportType", "Report type such as mission-summary or disaster-assessment.",
                        "title", "Optional report title.",
                        "missionSummary", "Optional mission summary written by the Agent.",
                        "agentFindings", "Optional Agent findings.",
                        "confirmed", "When true, create a REPORT_GENERATION draft Task from the generated markdown.",
                        "language", "Output language.")),
                ToolRiskLevel.MEDIUM,
                Set.of("AGENT_REPORT_DRAFT"));
        this.reportDraftService = reportDraftService;
        this.ragService = ragService;
        this.taskApplicationService = taskApplicationService;
    }

    @Override
    public AgentToolResult execute(AgentToolContext context, Map<String, Object> input) {
        if (reportDraftService == null || ragService == null) {
            return AgentToolResult.blocked("Report draft dependencies are not configured");
        }
        String missionId = text(input, "missionId");
        String taskId = optionalText(input, "taskId");
        List<RagDocument> sources = taskId == null
                ? ragService.searchMissionContext(missionId, missionId, 10)
                : ragService.search("report " + taskId, 10, Map.of("missionId", missionId, "taskId", taskId));
        ReportDraft draft = reportDraftService.generate(new ReportDraftRequest(
                defaultText(optionalText(input, "title"), "Agent 任务报告草稿"),
                defaultText(optionalText(input, "reportType"), "MISSION_SUMMARY"),
                missionId,
                taskId,
                optionalText(input, "missionSummary"),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                stringList(input.get("agentFindings")),
                sources));
        Map<String, Object> output = new LinkedHashMap<>();
        output.put("draftId", draft.draftId());
        output.put("status", draft.status());
        output.put("reportType", draft.reportType());
        output.put("markdown", draft.markdown());
        output.put("citationCount", draft.citations().size());
        output.put("citations", draft.citations().stream().map(citation -> citation.toMap()).toList());
        if (booleanValue(input, "confirmed", false)) {
            output.put("reportTask", createReportTask(context, input, draft, missionId));
        }
        return AgentToolResult.success(output);
    }

    private Map<String, Object> createReportTask(AgentToolContext context, Map<String, Object> input,
                                                 ReportDraft draft, String missionId) {
        if (taskApplicationService == null) {
            throw new IllegalStateException("Report generation task service is not configured");
        }
        Map<String, Object> config = new LinkedHashMap<>();
        config.put("reportMarkdown", draft.markdown());
        config.put("draftId", draft.draftId());
        config.put("reportType", draft.reportType());
        config.put("citationCount", draft.citations().size());
        config.put("confirmedBy", context.userId());
        Object toolCallId = context.attributes().get("toolCallId");
        if (toolCallId != null) {
            config.put("confirmedFromToolCall", toolCallId);
        }
        Task task = taskApplicationService.createTask(new CreateTaskInput(
                missionId,
                TaskType.REPORT_GENERATION,
                intValue(input, "priority", 1),
                null,
                null,
                config,
                List.of(),
                context.requestedBy()));
        return Map.of(
                "taskId", task.taskId(),
                "missionId", task.missionId(),
                "taskType", task.taskType().name(),
                "status", task.status().name(),
                "draftId", draft.draftId(),
                "nextStep", "submit REPORT_GENERATION task through Task API or scheduler flow");
    }

    private static String defaultText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
