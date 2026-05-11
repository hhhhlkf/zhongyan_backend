package com.zhongyan.uav.task.executor;

import com.zhongyan.uav.asset.domain.Asset;
import com.zhongyan.uav.asset.domain.AssetRepository;
import com.zhongyan.uav.asset.domain.AssetRole;
import com.zhongyan.uav.asset.domain.AssetType;
import com.zhongyan.uav.asset.domain.TaskAsset;
import com.zhongyan.uav.asset.domain.TaskAssetRepository;
import com.zhongyan.uav.task.domain.Task;

import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

abstract class AbstractMetadataTaskExecutor implements TaskExecutor {
    protected final AssetRepository assetRepository;
    protected final TaskAssetRepository taskAssetRepository;
    protected final Clock clock;

    protected AbstractMetadataTaskExecutor(AssetRepository assetRepository,
                                           TaskAssetRepository taskAssetRepository,
                                           Clock clock) {
        this.assetRepository = Objects.requireNonNull(assetRepository, "assetRepository must not be null");
        this.taskAssetRepository = Objects.requireNonNull(taskAssetRepository, "taskAssetRepository must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    protected Asset createAsset(Task task, AssetType type, AssetRole role, String name,
                                String objectKey, String contentType, Map<String, Object> metadata) {
        Instant now = clock.instant();
        Asset asset = Asset.created("asset-" + UUID.randomUUID(), task.missionId(), task.taskId(),
                type, role, name, objectKey, contentType, 0, "sha256:mock-" + task.taskId(),
                metadata, task.createdBy(), now).markAvailable(now);
        Asset savedAsset = assetRepository.save(asset);
        taskAssetRepository.save(new TaskAsset(task.taskId(), savedAsset.assetId(), role, now));
        return savedAsset;
    }

    protected Map<String, Object> metadata(Task task, String executorName) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("taskId", task.taskId());
        metadata.put("taskType", task.taskType().name());
        metadata.put("executor", executorName);
        metadata.put("mock", true);
        return metadata;
    }

    protected String text(Map<String, Object> values, String key, String fallback) {
        Object value = values.get(key);
        if (value == null || value.toString().isBlank()) {
            return fallback;
        }
        return value.toString();
    }

    protected TaskExecutionResult success(Task task, String executorName, Map<String, Object> metadata) {
        return new TaskExecutionResult(true, "0", executorName + " completed",
                "task-logs/" + task.taskId() + "/" + executorName + ".log",
                metadata, clock.instant());
    }
}
