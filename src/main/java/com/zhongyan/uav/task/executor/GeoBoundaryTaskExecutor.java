package com.zhongyan.uav.task.executor;

import com.zhongyan.uav.asset.domain.Asset;
import com.zhongyan.uav.asset.domain.AssetRepository;
import com.zhongyan.uav.asset.domain.AssetRole;
import com.zhongyan.uav.asset.domain.AssetType;
import com.zhongyan.uav.asset.domain.TaskAssetRepository;
import com.zhongyan.uav.task.domain.Task;
import com.zhongyan.uav.task.domain.TaskType;

import java.time.Clock;
import java.util.Map;

public class GeoBoundaryTaskExecutor extends AbstractMetadataTaskExecutor {
    public GeoBoundaryTaskExecutor(AssetRepository assetRepository, TaskAssetRepository taskAssetRepository) {
        this(assetRepository, taskAssetRepository, Clock.systemUTC());
    }

    public GeoBoundaryTaskExecutor(AssetRepository assetRepository, TaskAssetRepository taskAssetRepository,
                                   Clock clock) {
        super(assetRepository, taskAssetRepository, clock);
    }

    @Override
    public boolean supports(TaskType taskType) {
        return taskType == TaskType.CALCULATE_GEO_BOUNDARY;
    }

    @Override
    public TaskExecutionResult execute(TaskExecutionContext context) {
        Task task = context.task();
        task.inputAssetIds().forEach(assetId -> assetRepository.findById(assetId)
                .map(asset -> asset.markGeoCalculated(clock.instant()))
                .ifPresent(assetRepository::save));
        Map<String, Object> metadata = metadata(task, "geo-boundary");
        Asset geometry = createAsset(task, AssetType.GEOMETRY, AssetRole.OUTPUT,
                text(task.configSnapshot(), "name", task.taskId() + "-boundary.geojson"),
                "geo/" + task.taskId() + "/boundary.geojson", "application/geo+json", metadata);
        metadata = new java.util.LinkedHashMap<>(metadata);
        metadata.put("geometryAssetId", geometry.assetId());
        return success(task, "geo-boundary", metadata);
    }
}
