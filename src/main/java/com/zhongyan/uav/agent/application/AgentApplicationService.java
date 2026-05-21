package com.zhongyan.uav.agent.application;

import com.zhongyan.uav.agent.domain.AgentMessage;
import com.zhongyan.uav.agent.domain.AgentMessageRepository;
import com.zhongyan.uav.agent.domain.AgentMessageRole;
import com.zhongyan.uav.agent.domain.AgentSession;
import com.zhongyan.uav.agent.domain.AgentSessionRepository;
import com.zhongyan.uav.agent.domain.AgentToolCall;
import com.zhongyan.uav.agent.port.AgentChatRequest;
import com.zhongyan.uav.agent.port.AgentEvent;
import com.zhongyan.uav.agent.port.AgentEventPublisher;
import com.zhongyan.uav.agent.port.AgentPlannedToolCall;
import com.zhongyan.uav.agent.port.AgentReply;
import com.zhongyan.uav.agent.port.ChatModelPort;
import com.zhongyan.uav.agent.port.RagDocument;
import com.zhongyan.uav.asset.domain.Asset;
import com.zhongyan.uav.asset.domain.AssetRepository;
import com.zhongyan.uav.mission.domain.Mission;
import com.zhongyan.uav.mission.domain.MissionRepository;
import com.zhongyan.uav.task.domain.Task;
import com.zhongyan.uav.task.domain.TaskEvent;
import com.zhongyan.uav.task.domain.TaskEventRepository;
import com.zhongyan.uav.task.domain.TaskRepository;
import com.zhongyan.uav.telemetry.domain.UavTelemetry;
import com.zhongyan.uav.telemetry.domain.UavTelemetryRepository;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Agent 会话编排应用服务。
 * <p>
 * 负责保存用户消息、收集 RAG 和任务上下文、调用模型、执行模型规划的工具调用，
 * 并发布可进入 Kafka 和 SSE 的 Agent 审计事件。
 */
public class AgentApplicationService {
    private final AgentSessionRepository sessionRepository;
    private final AgentMessageRepository messageRepository;
    private final ChatModelPort chatModelPort;
    private final ToolGatewayService toolGatewayService;
    private final RagService ragService;
    private final ReportDraftService reportDraftService;
    private final AgentEventPublisher eventPublisher;
    private final Clock clock;
    private final MissionRepository missionRepository;
    private final TaskRepository taskRepository;
    private final TaskEventRepository taskEventRepository;
    private final AssetRepository assetRepository;
    private final UavTelemetryRepository telemetryRepository;

    public AgentApplicationService(AgentSessionRepository sessionRepository,
                                   AgentMessageRepository messageRepository,
                                   ChatModelPort chatModelPort,
                                   ToolGatewayService toolGatewayService,
                                   RagService ragService,
                                   ReportDraftService reportDraftService,
                                   AgentEventPublisher eventPublisher,
                                   Clock clock) {
        this(sessionRepository, messageRepository, chatModelPort, toolGatewayService, ragService,
                reportDraftService, eventPublisher, clock, null, null, null, null, null);
    }

    public AgentApplicationService(AgentSessionRepository sessionRepository,
                                   AgentMessageRepository messageRepository,
                                   ChatModelPort chatModelPort,
                                   ToolGatewayService toolGatewayService,
                                   RagService ragService,
                                   ReportDraftService reportDraftService,
                                   AgentEventPublisher eventPublisher,
                                   Clock clock,
                                   MissionRepository missionRepository,
                                   TaskRepository taskRepository,
                                   TaskEventRepository taskEventRepository,
                                   AssetRepository assetRepository,
                                   UavTelemetryRepository telemetryRepository) {
        this.sessionRepository = sessionRepository;
        this.messageRepository = messageRepository;
        this.chatModelPort = chatModelPort;
        this.toolGatewayService = toolGatewayService;
        this.ragService = ragService;
        this.reportDraftService = reportDraftService;
        this.eventPublisher = eventPublisher;
        this.clock = clock;
        this.missionRepository = missionRepository;
        this.taskRepository = taskRepository;
        this.taskEventRepository = taskEventRepository;
        this.assetRepository = assetRepository;
        this.telemetryRepository = telemetryRepository;
    }

    /**
     * 创建 Agent 会话，只落会话主记录，不在建会话阶段触发模型或工具。
     */
    public AgentSession createSession(CreateAgentSessionCommand command) {
        Instant now = clock.instant();
        AgentSession session = AgentSession.create("agent-session-" + UUID.randomUUID(),
                command.missionId(), command.taskId(), defaultText(command.userId(), "anonymous"),
                command.title(), now);
        AgentSession saved = sessionRepository.save(session);
        publish(saved.sessionId(), "SESSION_CREATED", Map.of("userId", saved.userId()));
        return saved;
    }

    /**
     * 只保存用户消息，供测试、导入或需要分步编排的调用方使用。
     */
    public AgentMessage sendMessage(SendAgentMessageCommand command) {
        requireSession(command.sessionId());
        AgentMessage userMessage = new AgentMessage("agent-message-" + UUID.randomUUID(),
                command.sessionId(), AgentMessageRole.USER, command.content(), command.metadata(), clock.instant());
        AgentMessage saved = messageRepository.save(userMessage);
        publish(command.sessionId(), "USER_MESSAGE_RECEIVED", Map.of("messageId", saved.messageId()));
        return saved;
    }

    /**
     * 完整对话编排入口：保存用户消息、收集上下文、调用模型、执行模型规划的工具并保存助手回复。
     */
    public AgentInteractionResult sendMessageAndReply(SendAgentMessageCommand command) {
        AgentSession session = requireSession(command.sessionId());
        AgentMessage userMessage = new AgentMessage("agent-message-" + UUID.randomUUID(),
                command.sessionId(), AgentMessageRole.USER, command.content(), command.metadata(), clock.instant());
        AgentMessage savedUserMessage = messageRepository.save(userMessage);
        publish(command.sessionId(), "USER_MESSAGE_RECEIVED", Map.of("messageId", savedUserMessage.messageId()));

        Map<String, Object> context = collectContext(session, savedUserMessage, command.metadata());
        AgentReply reply = callModel(command, context);
        List<AgentPlannedToolCall> plannedToolCalls = plannedToolCalls(reply, command.metadata());
        if (!plannedToolCalls.isEmpty()) {
            publish(command.sessionId(), "PLAN_CREATED", Map.of("toolCount", plannedToolCalls.size()));
        }

        List<AgentToolCall> toolCalls = executePlannedTools(savedUserMessage, command, plannedToolCalls);
        AgentMessage assistantMessage = saveAssistantMessage(command.sessionId(), reply, toolCalls, context);
        return new AgentInteractionResult(savedUserMessage, assistantMessage, toolCalls);
    }

    /**
     * 直接生成一次助手回复。保留该方法是为了兼容已有服务调用，内部仍走统一编排逻辑。
     */
    public AgentReply reply(String sessionId, String userId, Map<String, Object> context) {
        AgentSession session = requireSession(sessionId);
        List<AgentMessage> messages = messageRepository.findBySessionId(sessionId);
        Map<String, Object> mergedContext = new LinkedHashMap<>(context == null ? Map.of() : context);
        mergedContext.putIfAbsent("missionId", defaultText(session.missionId(), ""));
        mergedContext.putIfAbsent("taskId", defaultText(session.taskId(), ""));
        AgentReply reply = chatModelPort.chat(new AgentChatRequest(sessionId, userId, messages, mergedContext));
        AgentMessage assistantMessage = new AgentMessage("agent-message-" + UUID.randomUUID(), sessionId,
                AgentMessageRole.ASSISTANT, reply.content(), reply.metadata(), clock.instant());
        messageRepository.save(assistantMessage);
        publish(sessionId, "ASSISTANT_MESSAGE_CREATED", Map.of("messageId", assistantMessage.messageId()));
        return reply;
    }

    public Optional<AgentSession> findSession(String sessionId) {
        return sessionRepository.findById(sessionId);
    }

    public List<AgentMessage> listMessages(String sessionId) {
        requireSession(sessionId);
        return messageRepository.findBySessionId(sessionId);
    }

    /**
     * 根据持久化的消息和工具调用重建轻量时间线，REST 查询不直接依赖 Kafka 或 SSE。
     */
    public List<AgentEvent> listSessionEvents(String sessionId) {
        requireSession(sessionId);
        List<AgentEvent> events = new ArrayList<>();
        for (AgentMessage message : messageRepository.findBySessionId(sessionId)) {
            events.add(new AgentEvent("agent-event-message-" + message.messageId(), sessionId,
                    "MESSAGE_" + message.role().name(), Map.of(
                    "messageId", message.messageId(),
                    "role", message.role().name()), message.createdAt()));
        }
        for (AgentToolCall toolCall : toolGatewayService.listToolCalls(sessionId)) {
            events.add(new AgentEvent("agent-event-tool-" + toolCall.toolCallId(), sessionId,
                    "TOOL_CALL_" + toolCall.status().name(), Map.of(
                    "toolCallId", toolCall.toolCallId(),
                    "toolName", toolCall.toolName(),
                    "status", toolCall.status().name()), updatedAt(toolCall)));
        }
        events.sort(Comparator.comparing(AgentEvent::occurredAt));
        return events;
    }

    private AgentSession requireSession(String sessionId) {
        return sessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Agent session not found: " + sessionId));
    }

    private Map<String, Object> collectContext(AgentSession session, AgentMessage latestMessage,
                                               Map<String, Object> userContext) {
        Map<String, Object> context = new LinkedHashMap<>();
        context.put("missionId", defaultText(session.missionId(), ""));
        context.put("taskId", defaultText(session.taskId(), ""));
        context.put("sessionUserId", session.userId());
        context.put("userContext", userContext == null ? Map.of() : userContext);
        context.put("recentMessages", recentMessages(session.sessionId()));
        context.put("businessSummary", businessSummary(session, userContext));

        List<RagDocument> ragDocuments = safeRagSearch(latestMessage.content(), session);
        context.put("ragDocuments", ragDocuments.stream().map(this::ragView).toList());
        if (!ragDocuments.isEmpty()) {
            publish(session.sessionId(), "RAG_SOURCES_RETRIEVED", Map.of(
                    "sourceCount", ragDocuments.size(),
                    "sources", ragDocuments.stream().map(this::ragView).toList()));
        }
        if (shouldDraftReport(latestMessage.content(), userContext)) {
            context.put("reportDraft", reportDraftService.draftMarkdown("Agent 分析报告草稿", ragDocuments));
        }
        if (context.containsKey("reportDraft")) {
            publish(session.sessionId(), "REPORT_DRAFT_CREATED", Map.of(
                    "format", "markdown",
                    "citationCount", ragDocuments.size()));
        }
        publish(session.sessionId(), "CONTEXT_SUMMARIZED", Map.of(
                "keys", context.keySet().stream().toList(),
                "hasBusinessSummary", true));
        return context;
    }

    private Map<String, Object> businessSummary(AgentSession session, Map<String, Object> userContext) {
        Map<String, Object> summary = new LinkedHashMap<>();
        addIfPresent(summary, "mission", missionSummary(session.missionId()));
        addIfPresent(summary, "task", taskSummary(session.taskId()));
        addIfPresent(summary, "missionTasks", missionTaskSummary(session.missionId()));
        addIfPresent(summary, "recentTaskEvents", recentTaskEvents(session.taskId()));
        addIfPresent(summary, "assets", assetSummaries(session.missionId(), session.taskId()));
        addIfPresent(summary, "telemetry", telemetrySummary(session, userContext));
        return summary;
    }

    private Map<String, Object> missionSummary(String missionId) {
        if (missionRepository == null || missionId == null || missionId.isBlank()) {
            return Map.of();
        }
        return missionRepository.findById(missionId)
                .map(this::missionView)
                .orElse(Map.of());
    }

    private Map<String, Object> missionView(Mission mission) {
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("missionId", mission.missionId());
        view.put("name", truncate(mission.name(), 120));
        view.put("scenarioType", mission.scenarioType());
        view.put("status", mission.status().name());
        view.put("priority", mission.priority());
        view.put("createdAt", mission.createdAt().toString());
        if (mission.description() != null && !mission.description().isBlank()) {
            view.put("description", truncate(mission.description(), 240));
        }
        return view;
    }

    private Map<String, Object> taskSummary(String taskId) {
        if (taskRepository == null || taskId == null || taskId.isBlank()) {
            return Map.of();
        }
        return taskRepository.findById(taskId)
                .map(this::taskView)
                .orElse(Map.of());
    }

    private List<Map<String, Object>> missionTaskSummary(String missionId) {
        if (taskRepository == null || missionId == null || missionId.isBlank()) {
            return List.of();
        }
        return taskRepository.findByMissionId(missionId).stream()
                .sorted(Comparator.comparing(Task::updatedAt).reversed())
                .limit(5)
                .map(this::taskView)
                .toList();
    }

    private Map<String, Object> taskView(Task task) {
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("taskId", task.taskId());
        view.put("missionId", task.missionId());
        view.put("taskType", task.taskType().name());
        view.put("status", task.status().name());
        view.put("progress", task.progress());
        view.put("priority", task.priority());
        view.put("updatedAt", task.updatedAt().toString());
        if (task.errorCode() != null) {
            view.put("errorCode", task.errorCode());
        }
        if (task.errorMessage() != null) {
            view.put("errorMessage", truncate(task.errorMessage(), 240));
        }
        return view;
    }

    private List<Map<String, Object>> recentTaskEvents(String taskId) {
        if (taskEventRepository == null || taskId == null || taskId.isBlank()) {
            return List.of();
        }
        return taskEventRepository.findByTaskId(taskId).stream()
                .sorted(Comparator.comparing(TaskEvent::createdAt).reversed())
                .limit(8)
                .map(this::taskEventView)
                .toList();
    }

    private Map<String, Object> taskEventView(TaskEvent event) {
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("eventId", event.eventId());
        view.put("taskId", event.taskId());
        view.put("eventType", event.eventType().name());
        view.put("createdAt", event.createdAt().toString());
        if (!event.payload().isEmpty()) {
            view.put("payload", compactPayload(event.payload()));
        }
        return view;
    }

    private List<Map<String, Object>> assetSummaries(String missionId, String taskId) {
        if (assetRepository == null) {
            return List.of();
        }
        List<Asset> assets = taskId != null && !taskId.isBlank()
                ? assetRepository.findByTaskId(taskId)
                : missionId != null && !missionId.isBlank()
                ? assetRepository.findByMissionId(missionId)
                : List.of();
        return assets.stream()
                .sorted(Comparator.comparing(Asset::updatedAt).reversed())
                .limit(8)
                .map(this::assetView)
                .toList();
    }

    private Map<String, Object> assetView(Asset asset) {
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("assetId", asset.assetId());
        view.put("assetType", asset.assetType().name());
        view.put("assetRole", asset.assetRole().name());
        view.put("status", asset.status().name());
        view.put("geoStatus", asset.geoStatus().name());
        view.put("name", truncate(asset.name(), 120));
        view.put("objectKey", truncate(defaultText(asset.objectKey(), ""), 180));
        if (asset.layerUrl() != null) {
            view.put("layerUrl", truncate(asset.layerUrl(), 180));
        }
        return view;
    }

    private Map<String, Object> telemetrySummary(AgentSession session, Map<String, Object> userContext) {
        if (telemetryRepository == null) {
            return Map.of();
        }
        String uavId = optionalText(userContext, "uavId");
        if ((uavId == null || uavId.isBlank()) && taskRepository != null
                && session.taskId() != null && !session.taskId().isBlank()) {
            uavId = taskRepository.findById(session.taskId())
                    .map(Task::deviceId)
                    .orElse(null);
        }
        if (uavId == null || uavId.isBlank()) {
            return Map.of();
        }
        return telemetryRepository.findTrack(uavId, session.missionId(), 5).stream()
                .max(Comparator.comparing(UavTelemetry::recordedAt))
                .map(this::telemetryView)
                .orElse(Map.of("uavId", uavId, "status", "NO_RECENT_TELEMETRY"));
    }

    private Map<String, Object> telemetryView(UavTelemetry telemetry) {
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("uavId", telemetry.uavId());
        view.put("missionId", defaultText(telemetry.missionId(), ""));
        view.put("taskId", defaultText(telemetry.taskId(), ""));
        view.put("recordedAt", telemetry.recordedAt().toString());
        if (telemetry.latitude() != null) {
            view.put("latitude", telemetry.latitude());
        }
        if (telemetry.longitude() != null) {
            view.put("longitude", telemetry.longitude());
        }
        if (telemetry.altitudeMeters() != null) {
            view.put("altitudeMeters", telemetry.altitudeMeters());
        }
        if (telemetry.speedMetersPerSecond() != null) {
            view.put("speedMetersPerSecond", telemetry.speedMetersPerSecond());
        }
        return view;
    }

    private List<Map<String, Object>> recentMessages(String sessionId) {
        List<AgentMessage> messages = messageRepository.findBySessionId(sessionId);
        return messages.stream()
                .skip(Math.max(0, messages.size() - 8))
                .map(message -> Map.<String, Object>of(
                        "role", message.role().name(),
                        "content", message.content(),
                        "createdAt", message.createdAt().toString()))
                .toList();
    }

    private List<RagDocument> safeRagSearch(String query, AgentSession session) {
        try {
            return ragService.search(query, 5, Map.of(
                    "missionId", defaultText(session.missionId(), ""),
                    "taskId", defaultText(session.taskId(), "")));
        } catch (RuntimeException ex) {
            publish(session.sessionId(), "RAG_SEARCH_FAILED", Map.of("errorMessage",
                    defaultText(ex.getMessage(), ex.getClass().getSimpleName())));
            return List.of();
        }
    }

    private Map<String, Object> ragView(RagDocument document) {
        return Map.of(
                "documentId", defaultText(document.documentId(), ""),
                "sourceType", defaultText(document.sourceType(), ""),
                "sourceId", defaultText(document.sourceId(), ""),
                "title", defaultText(document.title(), ""),
                "snippet", defaultText(document.snippet(), ""));
    }

    private AgentReply callModel(SendAgentMessageCommand command, Map<String, Object> context) {
        try {
            List<AgentMessage> messages = messageRepository.findBySessionId(command.sessionId());
            return chatModelPort.chat(new AgentChatRequest(command.sessionId(), command.userId(), messages, context));
        } catch (RuntimeException ex) {
            publish(command.sessionId(), "MODEL_CALL_FAILED", Map.of("errorMessage",
                    defaultText(ex.getMessage(), ex.getClass().getSimpleName())));
            return new AgentReply("模型服务暂不可用，已保存用户消息，请稍后重试。",
                    List.of(), Map.of("modelError", defaultText(ex.getMessage(), ex.getClass().getSimpleName())));
        }
    }

    private List<AgentPlannedToolCall> plannedToolCalls(AgentReply reply, Map<String, Object> metadata) {
        if (!reply.toolCalls().isEmpty()) {
            return reply.toolCalls();
        }
        return plannedToolCallsFromMetadata(metadata);
    }

    @SuppressWarnings("unchecked")
    private List<AgentPlannedToolCall> plannedToolCallsFromMetadata(Map<String, Object> metadata) {
        Object rawToolCalls = metadata == null ? null : metadata.get("toolCalls");
        if (!(rawToolCalls instanceof List<?> list)) {
            return List.of();
        }
        List<AgentPlannedToolCall> planned = new ArrayList<>();
        for (Object item : list) {
            if (item instanceof Map<?, ?> map) {
                Object toolName = map.get("toolName");
                Object input = map.get("input");
                if (toolName != null) {
                    planned.add(new AgentPlannedToolCall(String.valueOf(toolName),
                            input instanceof Map<?, ?> inputMap ? (Map<String, Object>) inputMap : Map.of()));
                }
            }
        }
        return planned;
    }

    private List<AgentToolCall> executePlannedTools(AgentMessage userMessage, SendAgentMessageCommand command,
                                                    List<AgentPlannedToolCall> plannedToolCalls) {
        Set<String> permissions = permissions(command.metadata());
        List<AgentToolCall> toolCalls = new ArrayList<>();
        for (AgentPlannedToolCall plannedToolCall : plannedToolCalls) {
            toolCalls.add(toolGatewayService.invoke(new InvokeAgentToolCommand(command.sessionId(),
                    userMessage.messageId(), command.userId(), plannedToolCall.toolName(),
                    plannedToolCall.input(), permissions)));
        }
        return List.copyOf(toolCalls);
    }

    @SuppressWarnings("unchecked")
    private Set<String> permissions(Map<String, Object> metadata) {
        Object rawPermissions = metadata == null ? null : metadata.get("permissions");
        if (rawPermissions instanceof Set<?> set) {
            return set.stream().map(String::valueOf).collect(java.util.stream.Collectors.toUnmodifiableSet());
        }
        if (rawPermissions instanceof List<?> list) {
            return list.stream().map(String::valueOf).collect(java.util.stream.Collectors.toUnmodifiableSet());
        }
        return Set.of("AGENT_CHAT");
    }

    private AgentMessage saveAssistantMessage(String sessionId, AgentReply reply, List<AgentToolCall> toolCalls,
                                              Map<String, Object> context) {
        Map<String, Object> metadata = new LinkedHashMap<>(reply.metadata());
        metadata.put("toolCalls", toolCalls.stream().map(toolCall -> Map.of(
                "toolCallId", toolCall.toolCallId(),
                "toolName", toolCall.toolName(),
                "status", toolCall.status().name())).toList());
        metadata.put("contextKeys", context.keySet().stream().toList());
        AgentMessage assistantMessage = new AgentMessage("agent-message-" + UUID.randomUUID(), sessionId,
                AgentMessageRole.ASSISTANT, defaultText(reply.content(), "已完成 Agent 编排。"),
                metadata, clock.instant());
        AgentMessage saved = messageRepository.save(assistantMessage);
        publish(sessionId, "ANSWER_STREAMED", Map.of(
                "messageId", saved.messageId(),
                "chunkIndex", 0,
                "finalChunk", true,
                "content", defaultText(saved.content(), "")));
        publish(sessionId, "ASSISTANT_MESSAGE_CREATED", Map.of("messageId", saved.messageId(),
                "toolCallCount", toolCalls.size()));
        return saved;
    }

    private boolean shouldDraftReport(String content, Map<String, Object> metadata) {
        Object explicit = metadata == null ? null : metadata.get("reportRequested");
        return Boolean.TRUE.equals(explicit) || content != null && content.contains("报告");
    }

    private void addIfPresent(Map<String, Object> target, String key, Object value) {
        if (value instanceof Map<?, ?> map && map.isEmpty()) {
            return;
        }
        if (value instanceof List<?> list && list.isEmpty()) {
            return;
        }
        if (value != null) {
            target.put(key, value);
        }
    }

    private String optionalText(Map<String, Object> values, String key) {
        Object value = values == null ? null : values.get(key);
        return value == null ? null : String.valueOf(value);
    }

    private Map<String, Object> compactPayload(Map<String, Object> payload) {
        Map<String, Object> compact = new LinkedHashMap<>();
        payload.forEach((key, value) -> compact.put(key, value instanceof String text ? truncate(text, 180) : value));
        return compact;
    }

    private String truncate(String text, int maxLength) {
        if (text == null || text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, Math.max(0, maxLength - 3)) + "...";
    }

    private Instant updatedAt(AgentToolCall toolCall) {
        return toolCall.completedAt() == null ? toolCall.createdAt() : toolCall.completedAt();
    }

    private void publish(String sessionId, String eventType, Map<String, Object> payload) {
        eventPublisher.publish(new AgentEvent("agent-event-" + UUID.randomUUID(), sessionId, eventType,
                payload, clock.instant()));
    }

    private static String defaultText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
