package com.zhongyan.uav.telemetry.infrastructure;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhongyan.uav.event.application.DeadLetterEventService;
import com.zhongyan.uav.event.domain.EventEnvelope;
import com.zhongyan.uav.realtime.application.RealtimePushService;
import com.zhongyan.uav.telemetry.application.UavTelemetryIngestService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@ConditionalOnProperty(prefix = "bms.telemetry.kafka", name = "enabled", havingValue = "true")
public class KafkaTelemetryConsumer {
    private final ObjectMapper objectMapper;
    private final TelemetryMessageParser messageParser;
    private final UavTelemetryIngestService ingestService;
    private final RealtimePushService realtimePushService;
    private final DeadLetterEventService deadLetterEventService;

    public KafkaTelemetryConsumer(ObjectMapper objectMapper,
                                  UavTelemetryIngestService ingestService,
                                  RealtimePushService realtimePushService,
                                  DeadLetterEventService deadLetterEventService) {
        this.objectMapper = objectMapper;
        this.messageParser = new TelemetryMessageParser(objectMapper);
        this.ingestService = ingestService;
        this.realtimePushService = realtimePushService;
        this.deadLetterEventService = deadLetterEventService;
    }

    @KafkaListener(topics = "${bms.telemetry.kafka.topic:uav-telemetry}",
            groupId = "${spring.kafka.consumer.group-id:zhongyan-uav-local}")
    public void onMessage(String message) {
        try {
            if (messageParser.isEventEnvelope(message)) {
                realtimePushService.push(objectMapper.readValue(message, EventEnvelope.class));
                return;
            }
            ingestService.ingest(messageParser.parse(null, message));
        } catch (JsonProcessingException ex) {
            deadLetterEventService.record("uav-telemetry", "unknown", message, ex.getMessage(),
                    Map.of("stage", "deserialize-telemetry-event"));
        } catch (RuntimeException ex) {
            deadLetterEventService.record("uav-telemetry", "unknown", message, ex.getMessage(),
                    Map.of("stage", "ingest-telemetry"));
        }
    }
}
