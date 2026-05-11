package com.zhongyan.uav.task.api.request;

public record CancelTaskRequest(String cancelledBy) {
    public String cancelledByOrDefault() {
        return cancelledBy == null || cancelledBy.isBlank() ? "mock-user" : cancelledBy;
    }
}
