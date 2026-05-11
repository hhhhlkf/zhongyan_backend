package com.zhongyan.uav.task.executor;

import com.zhongyan.uav.asset.domain.AssetRepository;
import com.zhongyan.uav.asset.domain.AssetRole;
import com.zhongyan.uav.asset.domain.AssetType;
import com.zhongyan.uav.asset.domain.TaskAssetRepository;
import com.zhongyan.uav.task.domain.Task;
import com.zhongyan.uav.task.domain.TaskType;

import java.time.Clock;
import java.util.Map;

public class AgentAnalysisTaskExecutor extends AbstractMetadataTaskExecutor {
    public AgentAnalysisTaskExecutor(AssetRepository assetRepository, TaskAssetRepository taskAssetRepository) {
        this(assetRepository, taskAssetRepository, Clock.systemUTC());
    }

    public AgentAnalysisTaskExecutor(AssetRepository assetRepository, TaskAssetRepository taskAssetRepository,
                                     Clock clock) {
        super(assetRepository, taskAssetRepository, clock);
    }

    @Override
    public boolean supports(TaskType taskType) {
        return taskType == TaskType.AGENT_ANALYSIS;
    }

    @Override
    public TaskExecutionResult execute(TaskExecutionContext context) {
        Task task = context.task();
        Map<String, Object> metadata = metadata(task, "agent-analysis");
        createAsset(task, AssetType.MODEL_RESULT, AssetRole.OUTPUT, task.taskId() + "-analysis.json",
                "agent/" + task.taskId() + "/analysis.json", "application/json", metadata);
        return success(task, "agent-analysis", metadata);
    }
}
