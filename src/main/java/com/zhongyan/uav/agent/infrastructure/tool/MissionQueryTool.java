package com.zhongyan.uav.agent.infrastructure.tool;

import com.zhongyan.uav.agent.domain.ToolRiskLevel;
import com.zhongyan.uav.agent.port.AgentToolContext;
import com.zhongyan.uav.agent.port.AgentToolInputSchema;
import com.zhongyan.uav.agent.port.AgentToolResult;
import com.zhongyan.uav.mission.domain.Mission;
import com.zhongyan.uav.mission.domain.MissionRepository;
import com.zhongyan.uav.mission.domain.MissionStatus;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Agent read tool for retrieving mission summaries from the Mission repository.
 */
public class MissionQueryTool extends AbstractAgentTool {
    private final MissionRepository missionRepository;

    public MissionQueryTool(MissionRepository missionRepository) {
        super("mission.query", "Query mission context for Agent answers.",
                AgentToolInputSchema.of(Set.of(), Map.of(
                        "missionId", "Optional mission identifier.",
                        "status", "Optional mission status filter.",
                        "limit", "Maximum number of missions to return.")),
                ToolRiskLevel.LOW,
                Set.of("AGENT_CHAT"));
        this.missionRepository = missionRepository;
    }

    @Override
    public AgentToolResult execute(AgentToolContext context, Map<String, Object> input) {
        String missionId = optionalText(input, "missionId");
        if (missionId != null && !missionId.isBlank()) {
            return missionRepository.findById(missionId)
                    .map(mission -> AgentToolResult.success(Map.of("missions", List.of(toMap(mission)))))
                    .orElseGet(() -> AgentToolResult.success(Map.of("missions", List.of(), "notFound", missionId)));
        }
        String status = optionalText(input, "status");
        if (status == null || status.isBlank()) {
            return AgentToolResult.success(Map.of("missions", List.of(),
                    "message", "missionId or status is required for mission.query"));
        }
        int limit = intValue(input, "limit", 20);
        List<Map<String, Object>> missions = missionRepository.findByStatus(MissionStatus.valueOf(status.toUpperCase()))
                .stream()
                .limit(Math.max(limit, 1))
                .map(this::toMap)
                .toList();
        return AgentToolResult.success(Map.of("missions", missions));
    }

    private Map<String, Object> toMap(Mission mission) {
        return Map.of(
                "missionId", mission.missionId(),
                "name", mission.name(),
                "scenarioType", mission.scenarioType(),
                "status", mission.status().name(),
                "priority", mission.priority(),
                "createdAt", mission.createdAt().toString());
    }
}
