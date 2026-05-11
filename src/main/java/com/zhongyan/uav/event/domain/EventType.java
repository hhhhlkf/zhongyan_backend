package com.zhongyan.uav.event.domain;

public enum EventType {
    TASK_EVENT("task-events"),
    DEVICE_COMMAND("device-commands"),
    ASSET_EVENT("asset-events"),
    UAV_TELEMETRY("uav-telemetry"),
    AGENT_EVENT("agent-events"),
    DEAD_LETTER("dead-letter-events");

    private final String topic;

    EventType(String topic) {
        this.topic = topic;
    }

    public String topic() {
        return topic;
    }
}
