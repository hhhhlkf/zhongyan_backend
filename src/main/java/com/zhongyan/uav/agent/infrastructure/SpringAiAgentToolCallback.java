package com.zhongyan.uav.agent.infrastructure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhongyan.uav.agent.port.AgentTool;
import com.zhongyan.uav.agent.port.AgentToolInputSchema;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.tool.metadata.ToolMetadata;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class SpringAiAgentToolCallback implements ToolCallback {
    private final AgentTool tool;
    private final ObjectMapper objectMapper;
    private final ToolDefinition toolDefinition;

    SpringAiAgentToolCallback(AgentTool tool, ObjectMapper objectMapper) {
        this.tool = tool;
        this.objectMapper = objectMapper;
        this.toolDefinition = ToolDefinition.builder()
                .name(springToolName(tool.name()))
                .description(tool.description() + " Original Agent tool name: " + tool.name()
                        + ". Execution is delegated to ToolGatewayService.")
                .inputSchema(inputSchemaJson(tool.inputSchema(), objectMapper))
                .build();
    }

    @Override
    public ToolDefinition getToolDefinition() {
        return toolDefinition;
    }

    @Override
    public ToolMetadata getToolMetadata() {
        return ToolMetadata.builder().returnDirect(false).build();
    }

    @Override
    public String call(String toolInput) {
        return pendingGatewayExecution(toolInput);
    }

    @Override
    public String call(String toolInput, ToolContext toolContext) {
        return pendingGatewayExecution(toolInput);
    }

    static String springToolName(String agentToolName) {
        String normalized = agentToolName == null ? "agent_tool" : agentToolName.replaceAll("[^A-Za-z0-9_-]", "_");
        if (normalized.isBlank()) {
            return "agent_tool";
        }
        if (Character.isLetter(normalized.charAt(0)) || normalized.charAt(0) == '_') {
            return normalized;
        }
        return "agent_" + normalized;
    }

    private String pendingGatewayExecution(String toolInput) {
        try {
            return objectMapper.writeValueAsString(Map.of(
                    "status", "PENDING_TOOL_GATEWAY_EXECUTION",
                    "toolName", tool.name(),
                    "input", toolInput == null ? "{}" : toolInput));
        } catch (Exception exception) {
            return "{\"status\":\"PENDING_TOOL_GATEWAY_EXECUTION\"}";
        }
    }

    private static String inputSchemaJson(AgentToolInputSchema schema, ObjectMapper objectMapper) {
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("type", "object");
        Map<String, Object> properties = new LinkedHashMap<>();
        schema.properties().forEach((name, description) -> properties.put(name, Map.of(
                "type", jsonType(name),
                "description", description)));
        root.put("properties", properties);
        root.put("required", List.copyOf(schema.requiredFields()));
        root.put("additionalProperties", !schema.strict());
        try {
            return objectMapper.writeValueAsString(root);
        } catch (Exception exception) {
            return "{\"type\":\"object\",\"properties\":{}}";
        }
    }

    private static String jsonType(String fieldName) {
        String name = fieldName == null ? "" : fieldName.toLowerCase();
        if (name.equals("limit") || name.endsWith("limit") || name.endsWith("count")) {
            return "integer";
        }
        if (name.startsWith("include") || name.startsWith("is") || name.startsWith("has")
                || name.equals("enabled") || name.equals("submitted")) {
            return "boolean";
        }
        if (name.equals("payload") || name.equals("config") || name.equals("metadata") || name.endsWith("snapshot")) {
            return "object";
        }
        return "string";
    }
}
