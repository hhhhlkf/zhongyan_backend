package com.zhongyan.uav.agent.application;

import com.zhongyan.uav.agent.domain.AgentSession;
import com.zhongyan.uav.agent.domain.AgentToolCall;
import com.zhongyan.uav.agent.domain.AgentToolCallStatus;
import com.zhongyan.uav.agent.infrastructure.PgVectorStoreAdapter;
import com.zhongyan.uav.agent.infrastructure.mock.InMemoryAgentMessageRepository;
import com.zhongyan.uav.agent.infrastructure.mock.InMemoryAgentSessionRepository;
import com.zhongyan.uav.agent.infrastructure.mock.InMemoryAgentToolCallRepository;
import com.zhongyan.uav.agent.infrastructure.tool.ReportGenerateDraftTool;
import com.zhongyan.uav.agent.infrastructure.tool.TaskCommandCreateTool;
import com.zhongyan.uav.agent.port.AgentToolContext;
import com.zhongyan.uav.agent.port.AgentChatRequest;
import com.zhongyan.uav.agent.port.AgentEvent;
import com.zhongyan.uav.agent.port.AgentEventPublisher;
import com.zhongyan.uav.agent.port.AgentReply;
import com.zhongyan.uav.agent.port.AgentStreamChunk;
import com.zhongyan.uav.agent.port.AgentTool;
import com.zhongyan.uav.agent.port.ChatModelPort;
import com.zhongyan.uav.agent.port.RagDocument;
import com.zhongyan.uav.asset.domain.Asset;
import com.zhongyan.uav.asset.domain.AssetRole;
import com.zhongyan.uav.asset.domain.AssetType;
import com.zhongyan.uav.asset.infrastructure.mock.InMemoryAssetRepository;
import com.zhongyan.uav.mission.domain.Mission;
import com.zhongyan.uav.mission.infrastructure.mock.InMemoryMissionRepository;
import com.zhongyan.uav.task.application.TaskApplicationService;
import com.zhongyan.uav.task.application.TaskCommandApplicationService;
import com.zhongyan.uav.task.domain.Task;
import com.zhongyan.uav.task.domain.TaskCommandStatus;
import com.zhongyan.uav.task.domain.TaskCommandType;
import com.zhongyan.uav.task.domain.TaskEvent;
import com.zhongyan.uav.task.domain.TaskEventType;
import com.zhongyan.uav.task.domain.TaskStatus;
import com.zhongyan.uav.task.domain.TaskType;
import com.zhongyan.uav.task.infrastructure.mock.InMemoryTaskAttemptRepository;
import com.zhongyan.uav.task.infrastructure.mock.InMemoryTaskCommandRepository;
import com.zhongyan.uav.task.infrastructure.mock.InMemoryTaskEventRepository;
import com.zhongyan.uav.task.infrastructure.mock.InMemoryTaskRepository;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Step 11 acceptance tests for Agent behavior that spans RAG, reports, events, and command safety.
 */
class AgentStep11AcceptanceTests {
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-05-19T08:00:00Z"), ZoneOffset.UTC);

    @Test
    void conversationPublishesRagReportAndAnswerEvents() {
        CollectingAgentEventPublisher publisher = new CollectingAgentEventPublisher();
        InMemoryAgentToolCallRepository toolCallRepository = new InMemoryAgentToolCallRepository();
        ToolGatewayService gateway = new ToolGatewayService(List.of(), toolCallRepository, publisher, CLOCK);
        RagService ragService = new RagService(text -> List.of(0.1, 0.2), new PgVectorStoreAdapter(), CLOCK);
        ragService.index(new RagDocument("doc-1", "TASK_LOG", "task-1", "Task log",
                "report source with risk finding", null,
                Map.of("missionId", "mission-1", "taskId", "task-1")));
        AgentApplicationService service = new AgentApplicationService(new InMemoryAgentSessionRepository(),
                new InMemoryAgentMessageRepository(), new StaticChatModel(), gateway,
                ragService, new ReportDraftService(CLOCK), publisher, CLOCK);
        AgentSession session = service.createSession(new CreateAgentSessionCommand(
                "mission-1", "task-1", "analyst-1", "Acceptance", Map.of()));

        AgentInteractionResult result = service.sendMessageAndReply(new SendAgentMessageCommand(
                session.sessionId(), "analyst-1", "please create report for risk",
                Map.of("reportRequested", true)));

        assertThat(result.assistantMessage().content()).isEqualTo("analysis completed");
        assertThat(publisher.eventTypes()).contains("USER_MESSAGE_RECEIVED",
                "RAG_SOURCES_RETRIEVED", "REPORT_DRAFT_CREATED",
                "ANSWER_STREAMED", "ASSISTANT_MESSAGE_CREATED");
    }

    @Test
    void approvedAgentWriteToolCreatesTaskCommandThroughGateway() {
        InMemoryTaskRepository taskRepository = new InMemoryTaskRepository();
        InMemoryTaskCommandRepository commandRepository = new InMemoryTaskCommandRepository();
        InMemoryTaskEventRepository eventRepository = new InMemoryTaskEventRepository();
        taskRepository.save(Task.draft("task-1", "mission-1", TaskType.CAPTURE, 1,
                "device-1", null, Map.of(), List.of(), "operator-1", CLOCK.instant()));
        TaskCommandApplicationService taskCommandService = new TaskCommandApplicationService(
                taskRepository, commandRepository, eventRepository, CLOCK);
        AgentTool tool = new TaskCommandCreateTool(taskCommandService);
        CollectingAgentEventPublisher publisher = new CollectingAgentEventPublisher();
        ToolGatewayService gateway = new ToolGatewayService(List.of(tool),
                new InMemoryAgentToolCallRepository(), publisher, CLOCK);

        AgentToolCall waiting = gateway.invoke(new InvokeAgentToolCommand("session-1", "message-1",
                "agent-user", "taskCommand.create",
                Map.of("taskId", "task-1",
                        "commandType", TaskCommandType.START_CAPTURE.name(),
                        "reason", "acceptance test"),
                Set.of("AGENT_TOOL_APPROVE")));
        assertThat(waiting.status()).isEqualTo(AgentToolCallStatus.WAITING_APPROVAL);

        AgentToolCall completed = gateway.approve(waiting.toolCallId(), "reviewer-1");

        assertThat(completed.status()).isEqualTo(AgentToolCallStatus.COMPLETED);
        assertThat(completed.output()).containsEntry("commandStatus", TaskCommandStatus.PENDING_APPROVAL.name());
        assertThat(commandRepository.findByIdempotencyKey("agent-tool-" + waiting.toolCallId())).isPresent();
        assertThat(eventRepository.findByTaskId("task-1")).singleElement()
                .satisfies(event -> assertThat(event.payload()).containsEntry("commandId",
                        completed.output().get("commandId")));
        assertThat(publisher.eventTypes()).contains("APPROVAL_REQUESTED", "APPROVAL_CONFIRMED",
                "TOOL_CALL_STARTED", "TOOL_CALL_COMPLETED");
    }

    @Test
    void confirmedReportDraftCreatesReportGenerationTask() {
        InMemoryMissionRepository missionRepository = new InMemoryMissionRepository();
        missionRepository.save(Mission.draft("mission-1", "flood mission", "emergency",
                null, 1, "analyst-1", CLOCK.instant(), "report acceptance"));
        InMemoryTaskRepository taskRepository = new InMemoryTaskRepository();
        InMemoryTaskEventRepository eventRepository = new InMemoryTaskEventRepository();
        TaskApplicationService taskApplicationService = new TaskApplicationService(taskRepository,
                missionRepository, eventRepository, new InMemoryTaskAttemptRepository(), CLOCK);
        RagService ragService = new RagService(text -> List.of(0.1, 0.2), new PgVectorStoreAdapter(), CLOCK);
        ragService.index(new RagDocument("doc-1", "MISSION_NOTE", "mission-1", "Mission note",
                "confirmed report source", null, Map.of("missionId", "mission-1")));
        ReportGenerateDraftTool tool = new ReportGenerateDraftTool(new ReportDraftService(CLOCK),
                ragService, taskApplicationService);

        var result = tool.execute(new AgentToolContext("session-1", "analyst-1",
                "agent:session-1", Set.of("AGENT_REPORT_DRAFT"), Map.of("toolCallId", "tool-1")),
                Map.of("missionId", "mission-1", "confirmed", true));

        assertThat(result.success()).isTrue();
        assertThat(result.output()).containsKey("reportTask");
        assertThat(taskRepository.findByMissionId("mission-1")).singleElement()
                .satisfies(task -> {
                    assertThat(task.taskType()).isEqualTo(TaskType.REPORT_GENERATION);
                    assertThat(task.status()).isEqualTo(TaskStatus.DRAFT);
                    assertThat(task.configSnapshot()).containsKeys("reportMarkdown", "draftId");
                });
        assertThat(eventRepository.findByTaskId(taskRepository.findByMissionId("mission-1").get(0).taskId()))
                .extracting(TaskEvent::eventType)
                .contains(TaskEventType.CREATED);
    }

    @Test
    void agentContextIncludesBoundMissionTaskEventsAndAssets() {
        InMemoryMissionRepository missionRepository = new InMemoryMissionRepository();
        missionRepository.save(Mission.draft("mission-1", "flood mission", "emergency",
                null, 1, "analyst-1", CLOCK.instant(), "mission context"));
        InMemoryTaskRepository taskRepository = new InMemoryTaskRepository();
        Task task = Task.draft("task-1", "mission-1", TaskType.AGENT_ANALYSIS, 1,
                "uav-1", null, Map.of(), List.of(), "analyst-1", CLOCK.instant());
        taskRepository.save(task);
        InMemoryTaskEventRepository taskEventRepository = new InMemoryTaskEventRepository();
        taskEventRepository.save(new TaskEvent("event-1", "task-1", TaskEventType.CREATED,
                null, TaskStatus.DRAFT, Map.of("source", "test"), CLOCK.instant()));
        InMemoryAssetRepository assetRepository = new InMemoryAssetRepository();
        assetRepository.save(Asset.created("asset-1", "mission-1", "task-1",
                AssetType.REPORT, AssetRole.REPORT, "report.md", "reports/report.md",
                "text/markdown", 10, "sha256:test", Map.of(), "analyst-1",
                CLOCK.instant()).markAvailable(CLOCK.instant()));
        CapturingChatModel chatModel = new CapturingChatModel();
        AgentApplicationService service = new AgentApplicationService(new InMemoryAgentSessionRepository(),
                new InMemoryAgentMessageRepository(), chatModel,
                new ToolGatewayService(List.of(), new InMemoryAgentToolCallRepository(),
                        new CollectingAgentEventPublisher(), CLOCK),
                new RagService(text -> List.of(0.1), new PgVectorStoreAdapter(), CLOCK),
                new ReportDraftService(CLOCK), new CollectingAgentEventPublisher(), CLOCK,
                missionRepository, taskRepository, taskEventRepository, assetRepository, null);
        AgentSession session = service.createSession(new CreateAgentSessionCommand(
                "mission-1", "task-1", "analyst-1", "context", Map.of()));

        service.sendMessageAndReply(new SendAgentMessageCommand(session.sessionId(),
                "analyst-1", "请汇总上下文", Map.of()));

        assertThat(chatModel.context()).containsKey("businessSummary");
        @SuppressWarnings("unchecked")
        Map<String, Object> summary = (Map<String, Object>) chatModel.context().get("businessSummary");
        assertThat(summary).containsKeys("mission", "task", "recentTaskEvents", "assets");
    }

    /**
     * Deterministic chat model for acceptance tests.
     */
    private static final class StaticChatModel implements ChatModelPort {
        @Override
        public AgentReply chat(AgentChatRequest request) {
            return new AgentReply("analysis completed", List.of(), Map.of());
        }

        @Override
        public Flux<AgentStreamChunk> stream(AgentChatRequest request) {
            return Flux.empty();
        }
    }

    private static final class CapturingChatModel implements ChatModelPort {
        private Map<String, Object> context = Map.of();

        @Override
        public AgentReply chat(AgentChatRequest request) {
            context = request.context();
            return new AgentReply("context captured", List.of(), Map.of());
        }

        @Override
        public Flux<AgentStreamChunk> stream(AgentChatRequest request) {
            return Flux.empty();
        }

        private Map<String, Object> context() {
            return context;
        }
    }

    /**
     * Test publisher that exposes emitted Agent event types.
     */
    private static final class CollectingAgentEventPublisher implements AgentEventPublisher {
        private final List<AgentEvent> events = new ArrayList<>();

        @Override
        public void publish(AgentEvent event) {
            events.add(event);
        }

        private List<String> eventTypes() {
            return events.stream().map(AgentEvent::eventType).toList();
        }
    }
}
