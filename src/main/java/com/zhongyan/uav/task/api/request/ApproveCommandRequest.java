package com.zhongyan.uav.task.api.request;

public record ApproveCommandRequest(
        String approver,
        String reason) {
    /**
     * 获取审批人，空值时使用当前阶段默认 mock 审批人。
     */
    public String approverOrDefault() {
        return approver == null || approver.isBlank() ? "mock-approver" : approver;
    }
}
