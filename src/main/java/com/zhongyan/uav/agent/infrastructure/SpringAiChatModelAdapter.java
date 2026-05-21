package com.zhongyan.uav.agent.infrastructure;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhongyan.uav.agent.domain.AgentMessage;
import com.zhongyan.uav.agent.port.AgentChatRequest;
import com.zhongyan.uav.agent.port.AgentPlannedToolCall;
import com.zhongyan.uav.agent.port.AgentReply;
import com.zhongyan.uav.agent.port.AgentStreamChunk;
import com.zhongyan.uav.agent.port.AgentTool;
import com.zhongyan.uav.agent.port.ChatModelPort;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.tool.ToolCallback;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class SpringAiChatModelAdapter implements ChatModelPort {
    private final AgentProperties properties;
    private final ObjectMapper objectMapper;
    private final ChatModel chatModel;
    private final List<ToolCallback> toolCallbacks;
    private final Map<String, String> springToolNameToAgentToolName;

    public SpringAiChatModelAdapter(AgentProperties properties,
                                    ObjectMapper objectMapper,
                                    ChatModel chatModel,
                                    List<AgentTool> agentTools) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.chatModel = chatModel;
        this.toolCallbacks = agentTools == null ? List.of() : agentTools.stream()
                .map(tool -> new SpringAiAgentToolCallback(tool, objectMapper))
                .map(ToolCallback.class::cast)
                .toList();
        Map<String, String> names = new LinkedHashMap<>();
        if (agentTools != null) {
            for (AgentTool tool : agentTools) {
                names.put(SpringAiAgentToolCallback.springToolName(tool.name()), tool.name());
            }
        }
        this.springToolNameToAgentToolName = Map.copyOf(names);
    }

    @Override
    public AgentReply chat(AgentChatRequest request) {
        if (!available()) {
            return new AgentReply("Agent model adapter is not connected yet.", List.of(),
                    Map.of("enabled", false, "model", defaultText(properties.chatModel(), "not-configured")));
        }
        try {
            ChatResponse response = chatModel.call(prompt(request));
            AssistantMessage message = response.getResult().getOutput();
            String content = defaultText(message.getText(), "");
            List<AgentPlannedToolCall> toolCalls = parseToolCalls(message, content);
            return new AgentReply(stripToolCallJson(content), toolCalls, Map.of(
                    "enabled", true,
                    "provider", "spring-ai-openai",
                    "model", properties.chatModel(),
                    "toolSchemaCount", toolCallbacks.size(),
                    "toolCallCount", toolCalls.size()));
        } catch (RuntimeException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IllegalStateException("Spring AI chat model call failed: " + exception.getMessage(), exception);
        }
    }

    @Override
    public Flux<AgentStreamChunk> stream(AgentChatRequest request) {
        if (!available() || !properties.streamEnabled()) {
            return Flux.just(new AgentStreamChunk(request.sessionId(), chat(request).content(), true,
                    Map.of("stream", false, "model", defaultText(properties.chatModel(), "not-configured"))));
        }
        Map<String, Object> metadata = Map.of(
                "stream", true,
                "provider", "spring-ai-openai",
                "model", properties.chatModel());
        return chatModel.stream(prompt(request))
                .map(response -> new AgentStreamChunk(request.sessionId(),
                        defaultText(response.getResult().getOutput().getText(), ""), false, metadata))
                .concatWith(Flux.just(new AgentStreamChunk(request.sessionId(), "", true, metadata)));
    }

    private boolean available() {
        return properties.enabled() && chatModel != null;
    }

    private Prompt prompt(AgentChatRequest request) {
        List<Message> messages = new ArrayList<>();
        messages.add(new SystemMessage(systemPrompt(request)));
        request.messages().forEach(message -> messages.add(toSpringMessage(message)));
        OpenAiChatOptions options = OpenAiChatOptions.builder()
                .model(properties.chatModel())
                .toolCallbacks(toolCallbacks)
                .internalToolExecutionEnabled(false)
                .build();
        return new Prompt(messages, options);
    }

    private Message toSpringMessage(AgentMessage message) {
        return switch (message.role()) {
            case USER -> new UserMessage(message.content());
            case ASSISTANT -> new AssistantMessage(message.content());
            case SYSTEM -> new SystemMessage(message.content());
            case TOOL -> ToolResponseMessage.builder()
                    .responses(List.of(new ToolResponseMessage.ToolResponse(
                            "agent-tool-response", "agent-tool", message.content())))
                    .build();
        };
    }

    private String systemPrompt(AgentChatRequest request) {
        String context;
        try {
            context = objectMapper.writeValueAsString(request.context());
        } catch (Exception exception) {
            context = "{}";
        }
        return """
                You are Zhongyan Agent Orchestrator. Answer in Chinese when the user writes Chinese.
                Use the supplied context and RAG sources. Do not claim that a high-risk action has executed.
                Tools are declared through Spring AI tool callbacks. Tool calls are planned by the model,
                but execution, approval, idempotency and auditing are handled only by ToolGatewayService.
                If the model provider cannot emit native tool calls, include a compact JSON object at the end:
                {"toolCalls":[{"toolName":"task.query","input":{"taskId":"task-1"}}]}
                Context: %s
                """.formatted(context);
    }

    private List<AgentPlannedToolCall> parseToolCalls(AssistantMessage message, String content) {
        List<AgentPlannedToolCall> planned = new ArrayList<>();
        if (message.hasToolCalls()) {
            for (AssistantMessage.ToolCall toolCall : message.getToolCalls()) {
                String name = springToolNameToAgentToolName.getOrDefault(toolCall.name(), toolCall.name());
                if (!name.isBlank()) {
                    planned.add(new AgentPlannedToolCall(name, parseObject(toolCall.arguments())));
                }
            }
        }
        planned.addAll(parseToolCallsFromContent(content));
        return planned;
    }

    private List<AgentPlannedToolCall> parseToolCallsFromContent(String content) {
        if (content == null || !content.contains("\"toolCalls\"")) {
            return List.of();
        }
        int start = content.indexOf('{');
        int end = content.lastIndexOf('}');
        if (start < 0 || end <= start) {
            return List.of();
        }
        try {
            JsonNode root = objectMapper.readTree(content.substring(start, end + 1));
            List<AgentPlannedToolCall> planned = new ArrayList<>();
            JsonNode toolCalls = root.path("toolCalls");
            if (toolCalls.isArray()) {
                toolCalls.forEach(toolCall -> {
                    String name = toolCall.path("toolName").asText("");
                    if (!name.isBlank()) {
                        planned.add(new AgentPlannedToolCall(name, jsonObjectToMap(toolCall.path("input"))));
                    }
                });
            }
            return planned;
        } catch (Exception ignored) {
            return List.of();
        }
    }

    private String stripToolCallJson(String content) {
        if (content == null || !content.contains("\"toolCalls\"")) {
            return defaultText(content, "");
        }
        int start = content.indexOf('{');
        return start <= 0 ? defaultText(content, "") : content.substring(0, start).trim();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseObject(String json) {
        try {
            Object value = objectMapper.readValue(defaultText(json, "{}"), Map.class);
            return value instanceof Map<?, ?> map
                    ? (Map<String, Object>) map
                    : Map.of();
        } catch (Exception exception) {
            return Map.of();
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> jsonObjectToMap(JsonNode node) {
        if (node == null || !node.isObject()) {
            return Map.of();
        }
        return objectMapper.convertValue(node, Map.class);
    }

    private static String defaultText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
