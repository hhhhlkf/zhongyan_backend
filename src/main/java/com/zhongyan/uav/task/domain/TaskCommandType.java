package com.zhongyan.uav.task.domain;

public enum TaskCommandType {
    START_CAPTURE(RiskLevel.HIGH),
    STOP_DEVICE(RiskLevel.HIGH),
    START_PROCESS(RiskLevel.HIGH),
    STOP_PROCESS(RiskLevel.HIGH),
    START_TRANSFER(RiskLevel.HIGH),
    CALCULATE_GEO_BOUNDARY(RiskLevel.MEDIUM),
    GENERATE_PREVIEW(RiskLevel.MEDIUM),
    CANCEL_TASK(RiskLevel.MEDIUM),
    RETRY_TASK(RiskLevel.MEDIUM),
    DELETE_ASSET(RiskLevel.HIGH),
    OVERWRITE_ASSET(RiskLevel.HIGH),
    PUBLISH_LAYER(RiskLevel.HIGH),
    START_DEMO_PLAYBACK(RiskLevel.MEDIUM),
    START_AGENT_ANALYSIS(RiskLevel.HIGH),
    GENERATE_REPORT(RiskLevel.MEDIUM),
    UPDATE_DEVICE_COMMAND(RiskLevel.CRITICAL),
    UPDATE_MODEL_COMMAND(RiskLevel.CRITICAL);

    private final RiskLevel defaultRiskLevel;

    /**
     * 为命令类型绑定默认风险等级。
     */
    TaskCommandType(RiskLevel defaultRiskLevel) {
        this.defaultRiskLevel = defaultRiskLevel;
    }

    /**
     * 返回命令类型的默认风险等级。
     */
    public RiskLevel defaultRiskLevel() {
        return defaultRiskLevel;
    }

    /**
     * 判断该命令类型默认是否需要审批。
     */
    public boolean requiresApproval() {
        return defaultRiskLevel.requiresApproval();
    }
}
