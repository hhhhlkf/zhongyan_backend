package com.zhongyan.uav.task.application;

public enum TaskScheduleStatus {
    COMMAND_CREATED,
    WAITING_APPROVAL,
    EXECUTED,
    FAILED,
    TIMEOUT,
    RETRY_QUEUED,
    RECOVERED,
    SKIPPED
}
