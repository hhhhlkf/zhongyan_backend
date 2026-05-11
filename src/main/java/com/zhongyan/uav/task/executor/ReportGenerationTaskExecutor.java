package com.zhongyan.uav.task.executor;

import com.zhongyan.uav.asset.domain.AssetRepository;
import com.zhongyan.uav.asset.domain.AssetRole;
import com.zhongyan.uav.asset.domain.AssetType;
import com.zhongyan.uav.asset.domain.TaskAssetRepository;
import com.zhongyan.uav.task.domain.Task;
import com.zhongyan.uav.task.domain.TaskType;

import java.time.Clock;
import java.util.Map;

public class ReportGenerationTaskExecutor extends AbstractMetadataTaskExecutor {
    public ReportGenerationTaskExecutor(AssetRepository assetRepository, TaskAssetRepository taskAssetRepository) {
        this(assetRepository, taskAssetRepository, Clock.systemUTC());
    }

    public ReportGenerationTaskExecutor(AssetRepository assetRepository, TaskAssetRepository taskAssetRepository,
                                        Clock clock) {
        super(assetRepository, taskAssetRepository, clock);
    }

    @Override
    public boolean supports(TaskType taskType) {
        return taskType == TaskType.REPORT_GENERATION;
    }

    @Override
    public TaskExecutionResult execute(TaskExecutionContext context) {
        Task task = context.task();
        Map<String, Object> metadata = metadata(task, "report-generation");
        createAsset(task, AssetType.REPORT, AssetRole.REPORT, task.taskId() + "-report.md",
                "reports/" + task.taskId() + "/report.md", "text/markdown", metadata);
        return success(task, "report-generation", metadata);
    }
}
