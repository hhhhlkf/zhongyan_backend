package com.zhongyan.uav.task.application;

import com.zhongyan.uav.asset.domain.Asset;
import com.zhongyan.uav.asset.domain.AssetRepository;
import com.zhongyan.uav.task.domain.Task;
import com.zhongyan.uav.task.domain.TaskAttempt;
import com.zhongyan.uav.task.domain.TaskAttemptRepository;
import com.zhongyan.uav.task.domain.TaskCommand;
import com.zhongyan.uav.task.domain.TaskCommandRepository;
import com.zhongyan.uav.task.domain.TaskEvent;
import com.zhongyan.uav.task.domain.TaskEventRepository;
import com.zhongyan.uav.task.domain.TaskRepository;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;

public class TaskQueryService {
    private final TaskRepository taskRepository;
    private final TaskCommandRepository taskCommandRepository;
    private final TaskEventRepository taskEventRepository;
    private final TaskAttemptRepository taskAttemptRepository;
    private final AssetRepository assetRepository;

    /**
     * 创建 Task 查询服务。
     */
    public TaskQueryService(TaskRepository taskRepository, TaskCommandRepository taskCommandRepository,
                            TaskEventRepository taskEventRepository,
                            TaskAttemptRepository taskAttemptRepository,
                            AssetRepository assetRepository) {
        this.taskRepository = Objects.requireNonNull(taskRepository, "taskRepository must not be null");
        this.taskCommandRepository = Objects.requireNonNull(taskCommandRepository, "taskCommandRepository must not be null");
        this.taskEventRepository = Objects.requireNonNull(taskEventRepository, "taskEventRepository must not be null");
        this.taskAttemptRepository = Objects.requireNonNull(taskAttemptRepository, "taskAttemptRepository must not be null");
        this.assetRepository = Objects.requireNonNull(assetRepository, "assetRepository must not be null");
    }

    /**
     * 按 Task 编号查询，找不到时抛出异常。
     */
    public Task getTask(String taskId) {
        return taskRepository.findById(taskId)
                .orElseThrow(() -> new NoSuchElementException("Task not found: " + taskId));
    }

    /**
     * 查询某个 Mission 下的全部 Task。
     */
    public List<Task> listTasksByMission(String missionId) {
        return taskRepository.findByMissionId(missionId);
    }

    /**
     * 查询某个 Task 下的全部命令。
     */
    public List<TaskCommand> listCommands(String taskId) {
        return taskCommandRepository.findByTaskId(taskId);
    }

    /**
     * 查询某个 Task 的事件时间线。
     */
    public List<TaskEvent> listEvents(String taskId) {
        return taskEventRepository.findByTaskId(taskId);
    }

    public List<TaskAttempt> listAttempts(String taskId) {
        getTask(taskId);
        return taskAttemptRepository.findByTaskId(taskId);
    }

    /**
     * 查询某个 Task 关联的资产。
     */
    public List<Asset> listAssets(String taskId) {
        return assetRepository.findByTaskId(taskId);
    }
}
