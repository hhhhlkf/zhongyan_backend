package com.zhongyan.uav.task.executor;

import com.zhongyan.uav.asset.domain.AssetRepository;
import com.zhongyan.uav.asset.domain.AssetRole;
import com.zhongyan.uav.asset.domain.AssetType;
import com.zhongyan.uav.asset.domain.TaskAssetRepository;
import com.zhongyan.uav.agent.application.AgentApplicationService;
import com.zhongyan.uav.agent.application.AgentInteractionResult;
import com.zhongyan.uav.agent.application.CreateAgentSessionCommand;
import com.zhongyan.uav.agent.application.SendAgentMessageCommand;
import com.zhongyan.uav.task.domain.Task;
import com.zhongyan.uav.task.domain.TaskType;

import java.time.Clock;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class AgentAnalysisTaskExecutor extends AbstractMetadataTaskExecutor {
    private final AgentApplicationService agentApplicationService;

    public AgentAnalysisTaskExecutor(AssetRepository assetRepository, TaskAssetRepository taskAssetRepository) {
        this(assetRepository, taskAssetRepository, null, Clock.systemUTC());
    }

    public AgentAnalysisTaskExecutor(AssetRepository assetRepository, TaskAssetRepository taskAssetRepository,
                                     Clock clock) {
        this(assetRepository, taskAssetRepository, null, clock);
    }

    public AgentAnalysisTaskExecutor(AssetRepository assetRepository, TaskAssetRepository taskAssetRepository,
                                     AgentApplicationService agentApplicationService, Clock clock) {
        super(assetRepository, taskAssetRepository, clock);
        this.agentApplicationService = agentApplicationService;
    }

    @Override
    public boolean supports(TaskType taskType) {
        return taskType == TaskType.AGENT_ANALYSIS;
    }

    @Override
    public TaskExecutionResult execute(TaskExecutionContext context) {
        Task task = context.task();
        Map<String, Object> metadata = new LinkedHashMap<>(metadata(task, "agent-analysis"));
        metadata.put("mock", agentApplicationService == null);
        if (agentApplicationService != null) {
            metadata.putAll(runAgentAnalysis(task));
        }
        createAsset(task, AssetType.MODEL_RESULT, AssetRole.OUTPUT, task.taskId() + "-analysis.json",
                "agent/" + task.taskId() + "/analysis.json", "application/json", metadata);
        return success(task, "agent-analysis", metadata);
    }

    private Map<String, Object> runAgentAnalysis(Task task) {
        String prompt = text(task.configSnapshot(), "analysisPrompt",
                "请基于当前 Mission、Task、资产、遥测和日志上下文生成分析结论、风险判断、建议动作和报告草稿。");
        var session = agentApplicationService.createSession(new CreateAgentSessionCommand(
                task.missionId(), task.taskId(), task.createdBy(), "Task " + task.taskId() + " Agent Analysis",
                Map.of("sourceTaskId", task.taskId())));
        AgentInteractionResult result = agentApplicationService.sendMessageAndReply(new SendAgentMessageCommand(
                session.sessionId(), task.createdBy(), prompt, Map.of(
                "reportRequested", true,
                "permissions", List.of("AGENT_CHAT", "AGENT_REPORT_DRAFT"))));

        Map<String, Object> output = new LinkedHashMap<>();
        output.put("agentSessionId", session.sessionId());
        output.put("userMessageId", result.userMessage().messageId());
        output.put("assistantMessageId", result.assistantMessage().messageId());
        output.put("analysisConclusion", result.assistantMessage().content());
        output.put("toolCalls", result.toolCalls().stream().map(toolCall -> Map.of(
                "toolCallId", toolCall.toolCallId(),
                "toolName", toolCall.toolName(),
                "status", toolCall.status().name())).toList());
        output.put("result", "AGENT_ANALYSIS_COMPLETED");
        return output;
    }
}
