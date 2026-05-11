package com.zhongyan.uav.task.domain;

public enum TaskEventType {
    CREATED,
    SUBMITTED,
    COMMAND_CREATED,
    APPROVED,
    DISPATCHED,
    STARTED,
    PROGRESS_CHANGED,
    ASSET_BOUND,
    COMPLETED,
    FAILED,
    RETRYING,
    CANCELLED,
    TIMEOUT
}
