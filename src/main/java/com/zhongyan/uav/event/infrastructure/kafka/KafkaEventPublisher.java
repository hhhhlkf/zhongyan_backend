package com.zhongyan.uav.event.infrastructure.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhongyan.uav.event.domain.EventEnvelope;
import com.zhongyan.uav.event.port.EventPublisher;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Kafka 事件发布适配器。
 * <p>
 * 当 {@code bms.event.publisher=kafka} 时启用，把统一事件序列化为 JSON，
 * 并按事件信封中的 topic 和 messageKey 写入 Kafka。
 */
@Component
@ConditionalOnProperty(prefix = "bms.event", name = "publisher", havingValue = "kafka")
public class KafkaEventPublisher implements EventPublisher {
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public KafkaEventPublisher(KafkaTemplate<String, String> kafkaTemplate, ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public void publish(EventEnvelope event) {
        try {
            kafkaTemplate.send(event.topic(), event.messageKey(), objectMapper.writeValueAsString(event)).join();
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize event " + event.eventId(), ex);
        }
    }
}
