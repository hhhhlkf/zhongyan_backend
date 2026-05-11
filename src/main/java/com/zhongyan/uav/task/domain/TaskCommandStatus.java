package com.zhongyan.uav.task.domain;

public enum TaskCommandStatus {
    PENDING_APPROVAL,
    PENDING_DISPATCH,
    APPROVED,
    REJECTED,
    DISPATCHED,
    COMPLETED,
    FAILED,
    CANCELLED;

    /**
     * 判断命令是否已经结束，终态命令不应再次下发或审批。
     */
    public boolean isTerminal() {
        return this == REJECTED || this == COMPLETED || this == FAILED || this == CANCELLED;
    }
}
