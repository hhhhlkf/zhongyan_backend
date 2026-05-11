package com.zhongyan.uav.task.executor;

import com.zhongyan.uav.task.domain.TaskType;

public interface TaskExecutor {
    boolean supports(TaskType taskType);

    TaskExecutionResult execute(TaskExecutionContext context);
}
