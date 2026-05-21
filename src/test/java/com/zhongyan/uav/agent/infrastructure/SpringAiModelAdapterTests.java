package com.zhongyan.uav.agent.infrastructure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhongyan.uav.agent.domain.AgentMessage;
import com.zhongyan.uav.agent.domain.AgentMessageRole;
import com.zhongyan.uav.agent.domain.ToolRiskLevel;
import com.zhongyan.uav.agent.port.AgentChatRequest;
import com.zhongyan.uav.agent.port.AgentReply;
import com.zhongyan.uav.agent.port.AgentTool;
import com.zhongyan.uav.agent.port.AgentToolContext;
import com.zhongyan.uav.agent.port.AgentToolInputSchema;
import com.zhongyan.uav.agent.port.AgentToolResult;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class SpringAiModelAdapterTests {
    @Test
    void declaresAgentToolsAsSpringAiToolCallbacksAndMapsReturnedToolCalls() {
        CapturingChatModel chatModel = new CapturingChatModel();
        SpringAiChatModelAdapter adapter = new SpringAiChatModelAdapter(enabledProperties(), new ObjectMapper(),
                chatModel, List.of(taskQueryTool()));

        AgentReply reply = adapter.chat(new AgentChatRequest("session-1", "user-1", List.of(
                new AgentMessage("message-1", "session-1", AgentMessageRole.USER,
                        "查询 task-1", Map.of(), Instant.parse("2026-05-19T00:00:00Z"))),
                Map.of("taskId", "task-1")));

        assertThat(reply.content()).isEqualTo("需要查询任务。");
        assertThat(reply.toolCalls()).singleElement().satisfies(toolCall -> {
            assertThat(toolCall.toolName()).isEqualTo("task.query");
            assertThat(toolCall.input()).containsEntry("taskId", "task-1");
        });
        assertThat(chatModel.lastPrompt.getOptions()).isInstanceOf(ChatOptions.class);
        assertThat(chatModel.lastPrompt.getInstructions()).hasSize(2);
    }

    @Test
    void delegatesEmbeddingToSpringAiEmbeddingModel() {
        SpringAiEmbeddingAdapter adapter = new SpringAiEmbeddingAdapter(enabledProperties(), new FakeEmbeddingModel());

        assertThat(adapter.embed("hello")).containsExactly(0.25d, 0.5d, 0.75d);
    }

    private static AgentProperties enabledProperties() {
        return new AgentProperties(true, "http://localhost:11434/v1", "test-key",
                "qwen3:8b", "qwen3-embedding:0.6b", 5, true, true,
                "agent-events", Duration.ofSeconds(1), Duration.ofSeconds(5), null);
    }

    private static AgentTool taskQueryTool() {
        return new AgentTool() {
            @Override
            public String name() {
                return "task.query";
            }

            @Override
            public String description() {
                return "Query task details.";
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
        };
    }

    private static final class CapturingChatModel implements ChatModel {
        private Prompt lastPrompt;

        @Override
        public ChatResponse call(Prompt prompt) {
            this.lastPrompt = prompt;
            AssistantMessage message = AssistantMessage.builder()
                    .content("需要查询任务。")
                    .toolCalls(List.of(new AssistantMessage.ToolCall("call-1", "function",
                            SpringAiAgentToolCallback.springToolName("task.query"), "{\"taskId\":\"task-1\"}")))
                    .build();
            return new ChatResponse(List.of(new Generation(message)));
        }

        @Override
        public Flux<ChatResponse> stream(Prompt prompt) {
            return Flux.just(call(prompt));
        }
    }

    private static final class FakeEmbeddingModel implements EmbeddingModel {
        @Override
        public float[] embed(String text) {
            return new float[]{0.25f, 0.5f, 0.75f};
        }

        @Override
        public EmbeddingResponse call(EmbeddingRequest request) {
            throw new UnsupportedOperationException("not used");
        }

        @Override
        public float[] embed(Document document) {
            return new float[]{0.25f, 0.5f, 0.75f};
        }
    }
}
