package com.zhongyan.uav.task.domain;

public enum TaskStatus {
    DRAFT,
    WAITING_APPROVAL,
    QUEUED,
    DISPATCHING,
    RUNNING,
    WAITING_ASSET,
    POST_PROCESSING,
    COMPLETED,
    FAILED,
    RETRYING,
    CANCELLED,
    TIMEOUT;

    /**
     * 判断 Task 是否已经进入终态。
     */
    public boolean isTerminal() {
        return this == COMPLETED || this == CANCELLED || this == TIMEOUT;
    }
}
