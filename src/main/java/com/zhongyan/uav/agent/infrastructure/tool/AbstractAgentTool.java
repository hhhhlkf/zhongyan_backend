package com.zhongyan.uav.agent.infrastructure.tool;

import com.zhongyan.uav.agent.domain.ToolRiskLevel;
import com.zhongyan.uav.agent.port.AgentTool;
import com.zhongyan.uav.agent.port.AgentToolInputSchema;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Base class for Agent tools that centralizes immutable metadata and input helpers.
 */
abstract class AbstractAgentTool implements AgentTool {
    private final String name;
    private final String description;
    private final AgentToolInputSchema inputSchema;
    private final ToolRiskLevel riskLevel;
    private final Set<String> requiredPermissions;

    AbstractAgentTool(String name, String description, AgentToolInputSchema inputSchema,
                      ToolRiskLevel riskLevel, Set<String> requiredPermissions) {
        this.name = name;
        this.description = description;
        this.inputSchema = inputSchema;
        this.riskLevel = riskLevel;
        this.requiredPermissions = Set.copyOf(requiredPermissions);
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public String description() {
        return description;
    }

    @Override
    public AgentToolInputSchema inputSchema() {
        return inputSchema;
    }

    @Override
    public ToolRiskLevel riskLevel() {
        return riskLevel;
    }

    @Override
    public boolean approvalRequired() {
        return riskLevel.requiresApproval();
    }

    @Override
    public Set<String> requiredPermissions() {
        return requiredPermissions;
    }

    protected static String text(Map<String, Object> input, String field) {
        String value = optionalText(input, field);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Missing required Agent tool input field: " + field);
        }
        return value;
    }

    protected static String optionalText(Map<String, Object> input, String field) {
        Object value = input.get(field);
        return value == null ? null : String.valueOf(value);
    }

    protected static int intValue(Map<String, Object> input, String field, int fallback) {
        Object value = input.get(field);
        if (value == null || String.valueOf(value).isBlank()) {
            return fallback;
        }
        return Integer.parseInt(String.valueOf(value));
    }

    protected static boolean booleanValue(Map<String, Object> input, String field, boolean fallback) {
        Object value = input.get(field);
        return value == null ? fallback : Boolean.parseBoolean(String.valueOf(value));
    }

    @SuppressWarnings("unchecked")
    protected static Map<String, Object> mapValue(Map<String, Object> input, String field) {
        Object value = input.get(field);
        return value instanceof Map<?, ?> map ? (Map<String, Object>) map : Map.of();
    }

    protected static List<String> stringList(Object value) {
        if (value instanceof List<?> list) {
            return list.stream()
                    .filter(item -> item != null && !String.valueOf(item).isBlank())
                    .map(String::valueOf)
                    .toList();
        }
        if (value == null || String.valueOf(value).isBlank()) {
            return List.of();
        }
        return List.of(String.valueOf(value));
    }
}
