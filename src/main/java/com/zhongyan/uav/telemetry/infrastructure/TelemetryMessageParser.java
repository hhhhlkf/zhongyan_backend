package com.zhongyan.uav.telemetry.infrastructure;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhongyan.uav.telemetry.application.IngestTelemetryCommand;

import java.io.IOException;
import java.time.Instant;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class TelemetryMessageParser {
    private final ObjectMapper objectMapper;

    public TelemetryMessageParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public IngestTelemetryCommand parse(String defaultUavId, String message) {
        try {
            JsonNode root = objectMapper.readTree(message);
            JsonNode payload = root.has("eventId") && root.has("payload") ? root.path("payload") : root;
            String uavId = text(payload, "uavId", defaultUavId);
            return new IngestTelemetryCommand(uavId,
                    text(payload, "missionId", null),
                    text(payload, "taskId", null),
                    number(payload, "latitude"),
                    number(payload, "longitude"),
                    number(payload, "altitudeMeters"),
                    firstNumber(payload, "speedMetersPerSecond", "speedMps", "speed_mps"),
                    number(payload, "headingDegrees"),
                    instant(payload, "reportedAt", "recordedAt"),
                    toMap(payload));
        } catch (IOException ex) {
            throw new IllegalArgumentException("telemetry message is not valid JSON", ex);
        }
    }

    public boolean isEventEnvelope(String message) {
        try {
            JsonNode root = objectMapper.readTree(message);
            return root.has("eventId") && root.has("eventType") && root.has("payload");
        } catch (IOException ex) {
            return false;
        }
    }

    private String text(JsonNode node, String fieldName, String fallback) {
        JsonNode value = node.path(fieldName);
        if (value.isMissingNode() || value.isNull() || value.asText().isBlank()) {
            return fallback;
        }
        return value.asText();
    }

    private Double firstNumber(JsonNode node, String... fieldNames) {
        for (String fieldName : fieldNames) {
            Double value = number(node, fieldName);
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private Double number(JsonNode node, String fieldName) {
        JsonNode value = node.path(fieldName);
        if (value.isMissingNode() || value.isNull()) {
            return null;
        }
        return value.asDouble();
    }

    private Instant instant(JsonNode node, String... fieldNames) {
        for (String fieldName : fieldNames) {
            JsonNode value = node.path(fieldName);
            if (!value.isMissingNode() && !value.isNull() && !value.asText().isBlank()) {
                return Instant.parse(value.asText());
            }
        }
        return null;
    }

    private Map<String, Object> toMap(JsonNode node) {
        Map<String, Object> payload = new HashMap<>();
        Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> field = fields.next();
            if (!field.getValue().isNull()) {
                payload.put(field.getKey(), objectMapper.convertValue(field.getValue(), Object.class));
            }
        }
        return payload;
    }
}
