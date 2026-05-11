package com.zhongyan.uav.mission.domain;

public enum MissionStatus {
    DRAFT,
    ACTIVE,
    PAUSED,
    COMPLETED,
    CANCELLED,
    FAILED;

    /**
     * 判断 Mission 状态是否已经结束，终态不允许继续流转。
     */
    public boolean isTerminal() {
        return this == COMPLETED || this == CANCELLED || this == FAILED;
    }
}
