package com.zhongyan.uav.task.application;

public record TaskScheduleResult(
        String taskId,
        String commandId,
        TaskScheduleStatus status,
        String message) {
}
