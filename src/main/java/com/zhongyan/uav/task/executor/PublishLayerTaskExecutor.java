package com.zhongyan.uav.task.executor;

import com.zhongyan.uav.asset.domain.AssetRepository;
import com.zhongyan.uav.asset.domain.AssetRole;
import com.zhongyan.uav.asset.domain.AssetType;
import com.zhongyan.uav.asset.domain.TaskAssetRepository;
import com.zhongyan.uav.task.domain.Task;
import com.zhongyan.uav.task.domain.TaskType;

import java.time.Clock;
import java.util.LinkedHashMap;
import java.util.Map;

public class PublishLayerTaskExecutor extends AbstractMetadataTaskExecutor {
    public PublishLayerTaskExecutor(AssetRepository assetRepository, TaskAssetRepository taskAssetRepository) {
        this(assetRepository, taskAssetRepository, Clock.systemUTC());
    }

    public PublishLayerTaskExecutor(AssetRepository assetRepository, TaskAssetRepository taskAssetRepository,
                                    Clock clock) {
        super(assetRepository, taskAssetRepository, clock);
    }

    @Override
    public boolean supports(TaskType taskType) {
        return taskType == TaskType.PUBLISH_LAYER;
    }

    @Override
    public TaskExecutionResult execute(TaskExecutionContext context) {
        Task task = context.task();
        String layerUrl = text(task.configSnapshot(), "layerUrl",
                "/v2/layers/mock/" + task.taskId());
        task.inputAssetIds().forEach(assetId -> assetRepository.findById(assetId)
                .map(asset -> asset.publishLayer(layerUrl, clock.instant()))
                .ifPresent(assetRepository::save));
        Map<String, Object> metadata = new LinkedHashMap<>(metadata(task, "publish-layer"));
        metadata.put("layerUrl", layerUrl);
        createAsset(task, AssetType.LAYER, AssetRole.OUTPUT, task.taskId() + "-layer",
                "layers/" + task.taskId(), "application/vnd.ogc.wms_xml", metadata);
        return success(task, "publish-layer", metadata);
    }
}
