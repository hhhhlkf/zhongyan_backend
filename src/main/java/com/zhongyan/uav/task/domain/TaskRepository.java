package com.zhongyan.uav.task.domain;

import java.util.List;
import java.util.Optional;

public interface TaskRepository {
    /**
     * 保存 Task 聚合，具体持久化方式由基础设施层实现。
     */
    Task save(Task task);

    /**
     * 按 Task 编号查找聚合。
     */
    Optional<Task> findById(String taskId);

    /**
     * 查询某个 Mission 下的全部 Task。
     */
    List<Task> findByMissionId(String missionId);

    /**
     * 按 Task 状态查询列表，用于调度或恢复扫描。
     */
    List<Task> findByStatus(TaskStatus status);

    /**
     * 判断 Task 是否存在，避免重复创建或引用不存在的 Task。
     */
    boolean existsById(String taskId);
}
