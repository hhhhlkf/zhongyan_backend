package com.zhongyan.uav.task.executor;

import com.zhongyan.uav.task.domain.Task;
import com.zhongyan.uav.task.domain.TaskCommand;

public record TaskExecutionContext(Task task, TaskCommand command) {
}
