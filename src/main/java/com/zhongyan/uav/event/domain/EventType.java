package com.zhongyan.uav.event.domain;

/**
 * 系统级事件类型和对应 Kafka topic。
 * <p>
 * 枚举值表示业务语义，{@link #topic()} 返回该类事件默认进入的消息主题。
 */
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
