package com.zhongyan.uav.agent.domain;

public enum ToolRiskLevel {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL;

    public boolean requiresApproval() {
        return this == HIGH || this == CRITICAL;
    }
}
