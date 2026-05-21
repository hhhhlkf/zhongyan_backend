package com.zhongyan.uav.agent.application;

import com.zhongyan.uav.agent.domain.AgentMessageRole;
import com.zhongyan.uav.agent.domain.AgentSession;
import com.zhongyan.uav.agent.domain.AgentToolCallStatus;
import com.zhongyan.uav.agent.domain.ToolRiskLevel;
import com.zhongyan.uav.agent.infrastructure.mock.InMemoryAgentMessageRepository;
import com.zhongyan.uav.agent.infrastructure.mock.InMemoryAgentSessionRepository;
import com.zhongyan.uav.agent.infrastructure.mock.InMemoryAgentToolCallRepository;
import com.zhongyan.uav.agent.port.AgentChatRequest;
import com.zhongyan.uav.agent.port.AgentEvent;
import com.zhongyan.uav.agent.port.AgentEventPublisher;
import com.zhongyan.uav.agent.port.AgentPlannedToolCall;
import com.zhongyan.uav.agent.port.AgentReply;
import com.zhongyan.uav.agent.port.AgentStreamChunk;
import com.zhongyan.uav.agent.port.AgentTool;
import com.zhongyan.uav.agent.port.AgentToolContext;
import com.zhongyan.uav.agent.port.AgentToolInputSchema;
import com.zhongyan.uav.agent.port.AgentToolResult;
import com.zhongyan.uav.agent.port.ChatModelPort;
import com.zhongyan.uav.agent.port.RagDocument;
import com.zhongyan.uav.agent.port.VectorStorePort;
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

class AgentApplicationServiceTests {
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-05-19T08:00:00Z"), ZoneOffset.UTC);

    @Test
    void sendsMessageCallsModelExecutesPlannedToolAndSavesAssistantMessage() {
        InMemoryAgentSessionRepository sessionRepository = new InMemoryAgentSessionRepository();
        InMemoryAgentMessageRepository messageRepository = new InMemoryAgentMessageRepository();
        InMemoryAgentToolCallRepository toolCallRepository = new InMemoryAgentToolCallRepository();
        CollectingAgentEventPublisher publisher = new CollectingAgentEventPublisher();
        ToolGatewayService gateway = new ToolGatewayService(List.of(new EchoTool()), toolCallRepository,
                publisher, CLOCK);
        AgentApplicationService service = new AgentApplicationService(sessionRepository, messageRepository,
                new PlanningChatModel(), gateway, ragService(), new ReportDraftService(), publisher, CLOCK);

        AgentSession session = service.createSession(new CreateAgentSessionCommand(
                "mission-1", "task-1", "analyst-1", "研判会话", Map.of()));
        AgentInteractionResult result = service.sendMessageAndReply(new SendAgentMessageCommand(
                session.sessionId(), "analyst-1", "查询 task-1", Map.of("permissions", List.of("AGENT_CHAT"))));

        assertThat(result.userMessage().role()).isEqualTo(AgentMessageRole.USER);
        assertThat(result.assistantMessage().role()).isEqualTo(AgentMessageRole.ASSISTANT);
        assertThat(result.toolCalls()).hasSize(1);
        assertThat(result.toolCalls().get(0).status()).isEqualTo(AgentToolCallStatus.COMPLETED);
        assertThat(messageRepository.findBySessionId(session.sessionId())).hasSize(2);
        assertThat(service.listSessionEvents(session.sessionId()))
                .extracting(AgentEvent::eventType)
                .contains("MESSAGE_USER", "MESSAGE_ASSISTANT", "TOOL_CALL_COMPLETED");
        assertThat(publisher.eventTypes()).contains("USER_MESSAGE_RECEIVED", "PLAN_CREATED",
                "ASSISTANT_MESSAGE_CREATED");
    }

    @Test
    void modelFailureIsSavedAsAssistantMessageWithoutLosingUserMessage() {
        InMemoryAgentSessionRepository sessionRepository = new InMemoryAgentSessionRepository();
        InMemoryAgentMessageRepository messageRepository = new InMemoryAgentMessageRepository();
        InMemoryAgentToolCallRepository toolCallRepository = new InMemoryAgentToolCallRepository();
        CollectingAgentEventPublisher publisher = new CollectingAgentEventPublisher();
        ToolGatewayService gateway = new ToolGatewayService(List.of(new EchoTool()), toolCallRepository,
                publisher, CLOCK);
        AgentApplicationService service = new AgentApplicationService(sessionRepository, messageRepository,
                new FailingChatModel(), gateway, ragService(), new ReportDraftService(), publisher, CLOCK);

        AgentSession session = service.createSession(new CreateAgentSessionCommand(
                null, null, "analyst-1", "故障会话", Map.of()));
        AgentInteractionResult result = service.sendMessageAndReply(new SendAgentMessageCommand(
                session.sessionId(), "analyst-1", "请分析", Map.of()));

        assertThat(result.toolCalls()).isEmpty();
        assertThat(result.assistantMessage().content()).contains("模型服务暂不可用");
        assertThat(messageRepository.findBySessionId(session.sessionId())).hasSize(2);
        assertThat(publisher.eventTypes()).contains("MODEL_CALL_FAILED", "ASSISTANT_MESSAGE_CREATED");
    }

    private static RagService ragService() {
        return new RagService(text -> List.of(1.0), new VectorStorePort() {
            @Override
            public List<RagDocument> search(String query, int limit, Map<String, Object> filters) {
                return List.of();
            }

            @Override
            public void upsert(RagDocument document) {
            }
        });
    }

    private static final class PlanningChatModel implements ChatModelPort {
        @Override
        public AgentReply chat(AgentChatRequest request) {
            return new AgentReply("已查询任务。", List.of(new AgentPlannedToolCall(
                    "task.query", Map.of("taskId", "task-1"))), Map.of("source", "test"));
        }

        @Override
        public Flux<AgentStreamChunk> stream(AgentChatRequest request) {
            return Flux.empty();
        }
    }

    private static final class FailingChatModel implements ChatModelPort {
        @Override
        public AgentReply chat(AgentChatRequest request) {
            throw new IllegalStateException("model down");
        }

        @Override
        public Flux<AgentStreamChunk> stream(AgentChatRequest request) {
            return Flux.error(new IllegalStateException("model down"));
        }
    }

    private static final class EchoTool implements AgentTool {
        @Override
        public String name() {
            return "task.query";
        }

        @Override
        public String description() {
            return "query task";
        }

        @Override
        public AgentToolInputSchema inputSchema() {
            return AgentToolInputSchema.of(Set.of("taskId"), Map.of("taskId", "Task identifier."));
        }

        @Override
        public ToolRiskLevel riskLevel() {
            return ToolRiskLevel.LOW;
        }

        @Override
        public boolean approvalRequired() {
            return false;
        }

        @Override
        public Set<String> requiredPermissions() {
            return Set.of("AGENT_CHAT");
        }

        @Override
        public AgentToolResult execute(AgentToolContext context, Map<String, Object> input) {
            return AgentToolResult.success(Map.of("taskId", input.get("taskId")));
        }
    }

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
