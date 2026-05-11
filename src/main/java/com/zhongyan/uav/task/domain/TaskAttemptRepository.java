package com.zhongyan.uav.task.domain;

import java.util.List;
import java.util.Optional;

public interface TaskAttemptRepository {
    /**
     * 保存 TaskAttempt，重试时应保存新 Attempt 而不是覆盖旧 Attempt。
     */
    TaskAttempt save(TaskAttempt attempt);

    /**
     * 按 Attempt 编号查找执行尝试。
     */
    Optional<TaskAttempt> findById(String attemptId);

    /**
     * 查询某个 Task 的全部执行尝试。
     */
    List<TaskAttempt> findByTaskId(String taskId);
}
