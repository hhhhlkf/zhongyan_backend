package com.zhongyan.uav.event.infrastructure.kafka;

import com.zhongyan.uav.event.application.DeadLetterEventService;
import com.zhongyan.uav.event.domain.EventEnvelope;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.errors.TopicExistsException;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "bms.event.consumer=kafka",
        "bms.event.publisher=mock",
        "spring.kafka.bootstrap-servers=${BMS_KAFKA_BOOTSTRAP_SERVERS:localhost:9092}",
        "spring.kafka.consumer.group-id=zhongyan-uav-kafka-listener-it-${random.uuid}",
        "spring.kafka.consumer.auto-offset-reset=latest"
})
class KafkaEventConsumerBrokerIntegrationTests {
    private static final String TASK_EVENTS_TOPIC = "task-events";

    @Autowired
    private DeadLetterEventService deadLetterEventService;

    @Test
    @EnabledIfEnvironmentVariable(named = "BMS_KAFKA_INTEGRATION_TESTS", matches = "true")
    void recordsInvalidBrokerMessageAsDeadLetter() throws Exception {
        String bootstrapServers = System.getenv().getOrDefault("BMS_KAFKA_BOOTSTRAP_SERVERS", "localhost:9092");
        ensureTopic(bootstrapServers, TASK_EVENTS_TOPIC);
        String rawMessage = "{bad-json-" + UUID.randomUUID();

        try (KafkaProducer<String, String> producer = new KafkaProducer<>(producerProperties(bootstrapServers))) {
            producer.send(new ProducerRecord<>(TASK_EVENTS_TOPIC, "bad-message", rawMessage)).get(5, TimeUnit.SECONDS);
        }

        Instant deadline = Instant.now().plusSeconds(10);
        while (Instant.now().isBefore(deadline)) {
            List<EventEnvelope> deadLetters = deadLetterEventService.listDeadLetters(100);
            if (deadLetters.stream().anyMatch(event -> rawMessage.equals(event.payload().get("rawMessage")))) {
                return;
            }
            Thread.sleep(200);
        }

        assertThat(deadLetterEventService.listDeadLetters(100))
                .anyMatch(event -> rawMessage.equals(event.payload().get("rawMessage")));
    }

    private static Map<String, Object> producerProperties(String bootstrapServers) {
        return Map.of(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers,
                ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class,
                ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class,
                ProducerConfig.ACKS_CONFIG, "all");
    }

    private static void ensureTopic(String bootstrapServers, String topic) throws Exception {
        try (AdminClient adminClient = AdminClient.create(Map.of(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers))) {
            try {
                adminClient.createTopics(List.of(new NewTopic(topic, 1, (short) 1)))
                        .all()
                        .get(5, TimeUnit.SECONDS);
            } catch (ExecutionException ex) {
                if (!(ex.getCause() instanceof TopicExistsException)) {
                    throw ex;
                }
            }
        }
    }
}
