package com.zhongyan.uav.agent.port;

import java.util.Map;
import java.util.Set;

/**
 * Minimal tool input contract exposed to the Agent planner and enforced before execution.
 */
public record AgentToolInputSchema(
        Set<String> requiredFields,
        Map<String, String> properties,
        boolean strict) {
    public AgentToolInputSchema {
        requiredFields = requiredFields == null ? Set.of() : Set.copyOf(requiredFields);
        properties = properties == null ? Map.of() : Map.copyOf(properties);
    }

    public static AgentToolInputSchema none() {
        return new AgentToolInputSchema(Set.of(), Map.of(), false);
    }

    public static AgentToolInputSchema of(Set<String> requiredFields, Map<String, String> properties) {
        return new AgentToolInputSchema(requiredFields, properties, true);
    }

    public void validate(Map<String, Object> input) {
        Map<String, Object> actual = input == null ? Map.of() : input;
        for (String field : requiredFields) {
            Object value = actual.get(field);
            if (value == null || value instanceof String text && text.isBlank()) {
                throw new IllegalArgumentException("Missing required Agent tool input field: " + field);
            }
        }
        if (strict && !properties.isEmpty() && !properties.keySet().containsAll(actual.keySet())) {
            throw new IllegalArgumentException("Agent tool input contains unsupported fields");
        }
    }
}
