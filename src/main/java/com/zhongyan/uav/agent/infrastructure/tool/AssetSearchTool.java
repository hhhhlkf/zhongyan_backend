package com.zhongyan.uav.agent.infrastructure.tool;

import com.zhongyan.uav.agent.domain.ToolRiskLevel;
import com.zhongyan.uav.agent.port.AgentToolContext;
import com.zhongyan.uav.agent.port.AgentToolInputSchema;
import com.zhongyan.uav.agent.port.AgentToolResult;
import com.zhongyan.uav.asset.domain.Asset;
import com.zhongyan.uav.asset.domain.AssetRepository;
import com.zhongyan.uav.asset.domain.AssetType;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Agent read tool for searching asset metadata by mission, task, type, and keyword.
 */
public class AssetSearchTool extends AbstractAgentTool {
    private final AssetRepository assetRepository;

    public AssetSearchTool(AssetRepository assetRepository) {
        super("asset.search", "Search assets available to an Agent session.",
                AgentToolInputSchema.of(Set.of(), Map.of(
                        "missionId", "Mission scope for the search.",
                        "taskId", "Task scope for the search.",
                        "assetType", "Optional asset type filter.",
                        "keyword", "Optional name or metadata keyword.",
                        "limit", "Maximum number of assets to return.")),
                ToolRiskLevel.LOW,
                Set.of("AGENT_CHAT"));
        this.assetRepository = assetRepository;
    }

    @Override
    public AgentToolResult execute(AgentToolContext context, Map<String, Object> input) {
        int limit = intValue(input, "limit", 20);
        List<Map<String, Object>> assets = selectAssets(input).stream()
                .filter(asset -> matchesType(asset, optionalText(input, "assetType")))
                .filter(asset -> matchesKeyword(asset, optionalText(input, "keyword")))
                .limit(Math.max(limit, 1))
                .map(this::toMap)
                .toList();
        return AgentToolResult.success(Map.of("assets", assets));
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

    private boolean matchesKeyword(Asset asset, String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return true;
        }
        String lowerKeyword = keyword.toLowerCase();
        return asset.name().toLowerCase().contains(lowerKeyword)
                || asset.metadata().toString().toLowerCase().contains(lowerKeyword);
    }

    private Map<String, Object> toMap(Asset asset) {
        return Map.of(
                "assetId", asset.assetId(),
                "missionId", asset.missionId(),
                "taskId", defaultText(asset.taskId()),
                "assetType", asset.assetType().name(),
                "status", asset.status().name(),
                "name", asset.name(),
                "objectKey", defaultText(asset.objectKey()));
    }

    private String defaultText(String value) {
        return value == null ? "" : value;
    }
}
