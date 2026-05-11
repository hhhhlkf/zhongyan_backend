package com.zhongyan.uav.task.domain;

public enum RiskLevel {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL;

    /**
     * 判断风险等级是否必须进入审批流程。
     */
    public boolean requiresApproval() {
        return this == HIGH || this == CRITICAL;
    }
}
