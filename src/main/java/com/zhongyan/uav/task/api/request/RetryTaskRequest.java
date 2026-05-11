package com.zhongyan.uav.task.api.request;

public record RetryTaskRequest(String requestedBy) {
    public String requestedByOrDefault() {
        return requestedBy == null || requestedBy.isBlank() ? "mock-user" : requestedBy;
    }
}
