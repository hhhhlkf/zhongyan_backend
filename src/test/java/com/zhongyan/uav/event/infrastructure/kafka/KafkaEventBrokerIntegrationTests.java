package com.zhongyan.uav.event.infrastructure.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.zhongyan.uav.event.domain.EventEnvelope;
import com.zhongyan.uav.event.domain.EventType;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.errors.TopicExistsException;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

class KafkaEventBrokerIntegrationTests {
    private static final String TASK_EVENTS_TOPIC = "task-events";

    @Test
    @EnabledIfEnvironmentVariable(named = "BMS_KAFKA_INTEGRATION_TESTS", matches = "true")
    void publishesEventToRealKafkaBroker() throws Exception {
        String bootstrapServers = System.getenv().getOrDefault("BMS_KAFKA_BOOTSTRAP_SERVERS", "localhost:9092");
        ensureTopic(bootstrapServers, TASK_EVENTS_TOPIC);

        ObjectMapper objectMapper = objectMapper();
        DefaultKafkaProducerFactory<String, String> producerFactory = new DefaultKafkaProducerFactory<>(
                Map.of(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers,
                        ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class,
                        ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class,
                        ProducerConfig.ACKS_CONFIG, "all"));
        KafkaEventPublisher publisher = new KafkaEventPublisher(new KafkaTemplate<>(producerFactory), objectMapper);

        String taskId = "task-kafka-it-" + UUID.randomUUID();
        EventEnvelope event = EventEnvelope.pending("event-" + UUID.randomUUID(), "TASK", taskId,
                EventType.TASK_EVENT, Map.of("status", "COMPLETED"), Map.of("test", "kafka-it"), Instant.now());

        try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(consumerProperties(bootstrapServers))) {
            consumer.subscribe(List.of(TASK_EVENTS_TOPIC));
            consumer.poll(Duration.ofMillis(250));

            publisher.publish(event);

            Instant deadline = Instant.now().plusSeconds(10);
            while (Instant.now().isBefore(deadline)) {
                for (ConsumerRecord<String, String> record : consumer.poll(Duration.ofMillis(500))) {
                    EventEnvelope consumedEvent = readEventOrNull(objectMapper, record.value());
                    if (consumedEvent != null && event.eventId().equals(consumedEvent.eventId())) {
                        assertThat(record.key()).isEqualTo(taskId);
                        return;
                    }
                }
            }
            fail("Did not consume event " + event.eventId() + " from real Kafka broker");
        } finally {
            producerFactory.destroy();
        }
    }

    private static Map<String, Object> consumerProperties(String bootstrapServers) {
        return Map.of(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers,
                ConsumerConfig.GROUP_ID_CONFIG, "zhongyan-uav-kafka-it-" + UUID.randomUUID(),
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest",
                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class,
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
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

    private static ObjectMapper objectMapper() {
        return new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    private static EventEnvelope readEventOrNull(ObjectMapper objectMapper, String value) {
        try {
            return objectMapper.readValue(value, EventEnvelope.class);
        } catch (Exception ex) {
            return null;
        }
    }
}
