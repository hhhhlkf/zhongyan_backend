package com.zhongyan.uav.task.executor;

import com.zhongyan.uav.asset.domain.AssetRepository;
import com.zhongyan.uav.asset.domain.AssetRole;
import com.zhongyan.uav.asset.domain.AssetType;
import com.zhongyan.uav.asset.domain.TaskAssetRepository;
import com.zhongyan.uav.task.domain.Task;
import com.zhongyan.uav.task.domain.TaskType;

import java.time.Clock;
import java.util.Map;

public class DemoPlaybackTaskExecutor extends AbstractMetadataTaskExecutor {
    public DemoPlaybackTaskExecutor(AssetRepository assetRepository, TaskAssetRepository taskAssetRepository) {
        this(assetRepository, taskAssetRepository, Clock.systemUTC());
    }

    public DemoPlaybackTaskExecutor(AssetRepository assetRepository, TaskAssetRepository taskAssetRepository,
                                    Clock clock) {
        super(assetRepository, taskAssetRepository, clock);
    }

    @Override
    public boolean supports(TaskType taskType) {
        return taskType == TaskType.DEMO_PLAYBACK;
    }

    @Override
    public TaskExecutionResult execute(TaskExecutionContext context) {
        Task task = context.task();
        Map<String, Object> metadata = metadata(task, "demo-playback");
        createAsset(task, AssetType.ATTACHMENT, AssetRole.OUTPUT, task.taskId() + "-playback.json",
                "demo/" + task.taskId() + "/playback.json", "application/json", metadata);
        return success(task, "demo-playback", metadata);
    }
}
