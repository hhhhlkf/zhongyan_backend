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

public class PreviewTaskExecutor extends AbstractMetadataTaskExecutor {
    public PreviewTaskExecutor(AssetRepository assetRepository, TaskAssetRepository taskAssetRepository) {
        this(assetRepository, taskAssetRepository, Clock.systemUTC());
    }

    public PreviewTaskExecutor(AssetRepository assetRepository, TaskAssetRepository taskAssetRepository,
                               Clock clock) {
        super(assetRepository, taskAssetRepository, clock);
    }

    @Override
    public boolean supports(TaskType taskType) {
        return taskType == TaskType.GENERATE_PREVIEW;
    }

    @Override
    public TaskExecutionResult execute(TaskExecutionContext context) {
        Task task = context.task();
        String previewKey = "previews/" + task.taskId() + "/preview.jpg";
        task.inputAssetIds().forEach(assetId -> assetRepository.findById(assetId)
                .map(asset -> asset.attachPreview(previewKey, clock.instant()))
                .ifPresent(assetRepository::save));
        Map<String, Object> metadata = new LinkedHashMap<>(metadata(task, "preview"));
        metadata.put("previewObjectKey", previewKey);
        createAsset(task, AssetType.IMAGE, AssetRole.PREVIEW, task.taskId() + "-preview.jpg",
                previewKey, "image/jpeg", metadata);
        return success(task, "preview", metadata);
    }
}
