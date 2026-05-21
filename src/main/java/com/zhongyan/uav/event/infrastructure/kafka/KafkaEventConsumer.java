package com.zhongyan.uav.event.infrastructure.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhongyan.uav.event.application.DeadLetterEventService;
import com.zhongyan.uav.event.domain.EventEnvelope;
import com.zhongyan.uav.event.port.EventSubscriber;
import com.zhongyan.uav.realtime.application.RealtimePushService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Kafka 事件消费者。
 * <p>
 * 该消费者订阅系统事件主题，负责反序列化、按 eventId 去重，并把事件转交给实时推送服务。
 * 反序列化或推送失败时会记录死信事件，避免消费线程吞掉诊断信息。
 */
@Component
@ConditionalOnProperty(prefix = "bms.event", name = "consumer", havingValue = "kafka")
public class KafkaEventConsumer implements EventSubscriber {
    private final ObjectMapper objectMapper;
    private final RealtimePushService realtimePushService;
    private final DeadLetterEventService deadLetterEventService;
    private final Set<String> consumedEventIds = ConcurrentHashMap.newKeySet();

    public KafkaEventConsumer(ObjectMapper objectMapper,
                              RealtimePushService realtimePushService,
                              DeadLetterEventService deadLetterEventService) {
        this.objectMapper = objectMapper;
        this.realtimePushService = realtimePushService;
        this.deadLetterEventService = deadLetterEventService;
    }

    @KafkaListener(topics = {
            "task-events",
            "device-commands",
            "asset-events",
            "agent-events",
            "dead-letter-events"
    }, groupId = "${spring.kafka.consumer.group-id:zhongyan-uav-local}")
    public void onMessage(String message) {
        try {
            onEvent(objectMapper.readValue(message, EventEnvelope.class));
        } catch (JsonProcessingException ex) {
            deadLetterEventService.record("kafka", "unknown", message, ex.getMessage(),
                    Map.of("stage", "deserialize"));
        }
    }

    @Override
    public void onEvent(EventEnvelope event) {
        if (!consumedEventIds.add(event.eventId())) {
            return;
        }
        try {
            realtimePushService.push(event);
        } catch (RuntimeException ex) {
            deadLetterEventService.record(event.topic(), event.messageKey(), event.eventId(), ex.getMessage(),
                    Map.of("stage", "realtime-push", "sourceEventId", event.eventId()));
        }
    }
}
