package com.zhongyan.uav.event.application;

public record OutboxPublishResult(String eventId, boolean success, String message) {
}
