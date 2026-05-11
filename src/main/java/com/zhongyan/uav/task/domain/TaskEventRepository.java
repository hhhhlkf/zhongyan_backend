package com.zhongyan.uav.task.domain;

import java.util.List;
import java.util.Optional;

public interface TaskEventRepository {
    /**
     * 保存 TaskEvent，用于记录 Task 生命周期和审计事件。
     */
    TaskEvent save(TaskEvent event);

    /**
     * 按事件编号查找事件。
     */
    Optional<TaskEvent> findById(String eventId);

    /**
     * 查询某个 Task 的事件时间线。
     */
    List<TaskEvent> findByTaskId(String taskId);
}
