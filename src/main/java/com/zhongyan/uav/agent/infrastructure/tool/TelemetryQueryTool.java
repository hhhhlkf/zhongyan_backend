package com.zhongyan.uav.agent.infrastructure.tool;

import com.zhongyan.uav.agent.domain.ToolRiskLevel;
import com.zhongyan.uav.agent.port.AgentToolContext;
import com.zhongyan.uav.agent.port.AgentToolInputSchema;
import com.zhongyan.uav.agent.port.AgentToolResult;
import com.zhongyan.uav.telemetry.application.UavTelemetryQueryService;
import com.zhongyan.uav.telemetry.domain.UavTelemetry;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Agent read tool for latest telemetry, time ranges, and mission tracks.
 */
public class TelemetryQueryTool extends AbstractAgentTool {
    private final UavTelemetryQueryService telemetryQueryService;

    public TelemetryQueryTool(UavTelemetryQueryService telemetryQueryService) {
        super("telemetry.query", "Query UAV telemetry context.",
                AgentToolInputSchema.of(Set.of("uavId"), Map.of(
                        "uavId", "UAV identifier.",
                        "missionId", "Optional mission identifier.",
                        "from", "Optional inclusive start time.",
                        "to", "Optional exclusive end time.",
                        "latestOnly", "Whether to return only the latest telemetry.")),
                ToolRiskLevel.LOW,
                Set.of("AGENT_CHAT"));
        this.telemetryQueryService = telemetryQueryService;
    }

    @Override
    public AgentToolResult execute(AgentToolContext context, Map<String, Object> input) {
        String uavId = text(input, "uavId");
        if (booleanValue(input, "latestOnly", false)) {
            return AgentToolResult.success(Map.of("telemetry", List.of(toMap(telemetryQueryService.latest(uavId)))));
        }
        String missionId = optionalText(input, "missionId");
        List<UavTelemetry> telemetry = missionId == null || missionId.isBlank()
                ? telemetryQueryService.range(uavId, instant(input, "from"), instant(input, "to"), intValue(input, "limit", 100))
                : telemetryQueryService.track(uavId, missionId, intValue(input, "limit", 100));
        return AgentToolResult.success(Map.of("telemetry", telemetry.stream().map(this::toMap).toList()));
    }

    private Instant instant(Map<String, Object> input, String field) {
        String value = optionalText(input, field);
        return value == null || value.isBlank() ? null : Instant.parse(value);
    }

    private Map<String, Object> toMap(UavTelemetry telemetry) {
        return Map.of(
                "telemetryId", telemetry.telemetryId(),
                "uavId", telemetry.uavId(),
                "missionId", defaultText(telemetry.missionId()),
                "taskId", defaultText(telemetry.taskId()),
                "reportedAt", telemetry.recordedAt().toString(),
                "latitude", telemetry.latitude(),
                "longitude", telemetry.longitude(),
                "altitudeMeters", defaultNumber(telemetry.altitudeMeters()),
                "headingDegrees", defaultNumber(telemetry.headingDegrees()),
                "speedMetersPerSecond", defaultNumber(telemetry.speedMetersPerSecond()));
    }

    private String defaultText(String value) {
        return value == null ? "" : value;
    }

    private Double defaultNumber(Double value) {
        return value == null ? 0.0 : value;
    }
}
