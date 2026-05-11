package com.zhongyan.uav.asset.domain;

public enum AssetStatus {
    CREATED,
    UPLOADING,
    AVAILABLE,
    FAILED,
    DELETED;

    /**
     * 判断资产状态是否已经结束。
     */
    public boolean isTerminal() {
        return this == FAILED || this == DELETED;
    }
}
