package com.zhongyan.uav.task.api.request;

public record SubmitTaskRequest(String submittedBy) {
    /**
     * 获取提交人，空值时使用当前阶段默认 mock 用户。
     */
    public String submittedByOrDefault() {
        return submittedBy == null || submittedBy.isBlank() ? "mock-user" : submittedBy;
    }
}
