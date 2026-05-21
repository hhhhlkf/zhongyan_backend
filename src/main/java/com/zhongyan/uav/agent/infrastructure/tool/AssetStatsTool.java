package com.zhongyan.uav.agent.infrastructure.tool;

import com.zhongyan.uav.agent.domain.ToolRiskLevel;
import com.zhongyan.uav.agent.port.AgentToolContext;
import com.zhongyan.uav.agent.port.AgentToolInputSchema;
import com.zhongyan.uav.agent.port.AgentToolResult;
import com.zhongyan.uav.asset.domain.Asset;
import com.zhongyan.uav.asset.domain.AssetRepository;
import com.zhongyan.uav.asset.domain.AssetStatus;
import com.zhongyan.uav.asset.domain.AssetType;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Agent read tool for summarizing asset counts by type and status.
 */
public class AssetStatsTool extends AbstractAgentTool {
    private final AssetRepository assetRepository;

    public AssetStatsTool(AssetRepository assetRepository) {
        super("asset.stats", "Summarize asset counts and processing status.",
                AgentToolInputSchema.of(Set.of(), Map.of(
                        "missionId", "Optional mission scope.",
                        "taskId", "Optional task scope.",
                        "assetType", "Optional asset type filter.")),
                ToolRiskLevel.LOW,
                Set.of("AGENT_CHAT"));
        this.assetRepository = assetRepository;
    }

    @Override
    public AgentToolResult execute(AgentToolContext context, Map<String, Object> input) {
        List<Asset> assets = selectAssets(input).stream()
                .filter(asset -> matchesType(asset, optionalText(input, "assetType")))
                .toList();
        Map<String, Long> byType = assets.stream()
                .collect(Collectors.groupingBy(asset -> asset.assetType().name(), LinkedHashMap::new, Collectors.counting()));
        Map<String, Long> byStatus = assets.stream()
                .collect(Collectors.groupingBy(asset -> asset.status().name(), LinkedHashMap::new, Collectors.counting()));
        return AgentToolResult.success(Map.of(
                "total", assets.size(),
                "byType", byType,
                "byStatus", byStatus,
                "available", assets.stream().filter(asset -> asset.status() == AssetStatus.AVAILABLE).count()));
    }

    private List<Asset> selectAssets(Map<String, Object> input) {
        String taskId = optionalText(input, "taskId");
        if (taskId != null && !taskId.isBlank()) {
            return assetRepository.findByTaskId(taskId);
        }
        String missionId = optionalText(input, "missionId");
        if (missionId != null && !missionId.isBlank()) {
            return assetRepository.findByMissionId(missionId);
        }
        return assetRepository.findAll();
    }

    private boolean matchesType(Asset asset, String assetType) {
        return assetType == null || assetType.isBlank() || asset.assetType() == AssetType.valueOf(assetType.toUpperCase());
    }
}
