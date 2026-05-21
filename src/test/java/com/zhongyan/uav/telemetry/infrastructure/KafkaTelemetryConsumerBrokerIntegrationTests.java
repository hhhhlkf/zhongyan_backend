package com.zhongyan.uav.telemetry.infrastructure;

import com.zhongyan.uav.telemetry.application.UavTelemetryQueryService;
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
        "bms.telemetry.kafka.enabled=true",
        "spring.kafka.bootstrap-servers=${BMS_KAFKA_BOOTSTRAP_SERVERS:localhost:9092}",
        "spring.kafka.consumer.group-id=zhongyan-uav-telemetry-it-${random.uuid}",
        "spring.kafka.consumer.auto-offset-reset=latest"
})
class KafkaTelemetryConsumerBrokerIntegrationTests {
    private static final String TOPIC = "uav-telemetry";

    @Autowired
    private UavTelemetryQueryService queryService;

    @Test
    @EnabledIfEnvironmentVariable(named = "BMS_TELEMETRY_KAFKA_INTEGRATION_TESTS", matches = "true")
    void consumesRawTelemetryFromRealKafkaBroker() throws Exception {
        String bootstrapServers = System.getenv().getOrDefault("BMS_KAFKA_BOOTSTRAP_SERVERS", "localhost:9092");
        ensureTopic(bootstrapServers, TOPIC);
        String uavId = "uav-kafka-it-" + UUID.randomUUID();
        String message = """
                {"uavId":"%s","missionId":"mission-kafka-it","latitude":30.61,"longitude":104.06,
                "altitudeMeters":128.0,"speedMetersPerSecond":13.5,"headingDegrees":91.0,
                "reportedAt":"2026-04-29T00:00:00Z"}
                """.formatted(uavId);

        try (KafkaProducer<String, String> producer = new KafkaProducer<>(producerProperties(bootstrapServers))) {
            producer.send(new ProducerRecord<>(TOPIC, uavId, message)).get(5, TimeUnit.SECONDS);
        }

        Instant deadline = Instant.now().plusSeconds(10);
        while (Instant.now().isBefore(deadline)) {
            try {
                assertThat(queryService.latest(uavId).latitude()).isEqualTo(30.61);
                return;
            } catch (RuntimeException ex) {
                Thread.sleep(200);
            }
        }

        assertThat(queryService.latest(uavId).latitude()).isEqualTo(30.61);
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
