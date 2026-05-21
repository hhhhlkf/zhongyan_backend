package com.zhongyan.uav.telemetry.infrastructure;

import com.zhongyan.uav.ZhongyanUavApplication;
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
import org.springframework.test.context.ActiveProfiles;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("local")
@SpringBootTest(classes = ZhongyanUavApplication.class, properties = {
        "bms.telemetry.kafka.enabled=true",
        "bms.telemetry.udp.enabled=true",
        "bms.telemetry.udp.port=18090",
        "bms.event.consumer=mock",
        "bms.event.publisher=mock",
        "spring.kafka.consumer.group-id=zhongyan-uav-telemetry-local-e2e-${random.uuid}",
        "spring.kafka.consumer.auto-offset-reset=latest"
})
@EnabledIfEnvironmentVariable(named = "BMS_TELEMETRY_LOCAL_E2E_TESTS", matches = "true")
class TelemetryLocalEndToEndIntegrationTests {
    private static final String TOPIC = "uav-telemetry";

    @Autowired
    private UavTelemetryQueryService queryService;

    @Test
    void consumesKafkaTelemetryAndPersistsToLocalPostgres() throws Exception {
        String bootstrapServers = System.getenv().getOrDefault("BMS_KAFKA_BOOTSTRAP_SERVERS", "localhost:9092");
        ensureTopic(bootstrapServers, TOPIC);
        String uavId = "uav-local-kafka-" + UUID.randomUUID();
        String message = """
                {"uavId":"%s","latitude":30.71,"longitude":104.11,
                "altitudeMeters":130.0,"speedMetersPerSecond":15.5,"headingDegrees":93.0,
                "reportedAt":"2026-04-29T00:00:02Z"}
                """.formatted(uavId);

        try (KafkaProducer<String, String> producer = new KafkaProducer<>(producerProperties(bootstrapServers))) {
            producer.send(new ProducerRecord<>(TOPIC, uavId, message)).get(5, TimeUnit.SECONDS);
        }

        awaitTelemetry(uavId, 30.71, 104.11);
    }

    @Test
    void receivesUdpTelemetryAndPersistsToLocalPostgres() throws Exception {
        String uavId = "uav-local-udp-" + UUID.randomUUID();
        String message = """
                {"uavId":"%s","latitude":30.72,"longitude":104.12,
                "altitudeMeters":131.0,"speedMetersPerSecond":16.5,"headingDegrees":94.0,
                "reportedAt":"2026-04-29T00:00:03Z"}
                """.formatted(uavId);
        byte[] payload = message.getBytes(StandardCharsets.UTF_8);

        try (DatagramSocket socket = new DatagramSocket()) {
            socket.send(new DatagramPacket(payload, payload.length,
                    InetAddress.getByName("127.0.0.1"), 18090));
        }

        awaitTelemetry(uavId, 30.72, 104.12);
    }

    private void awaitTelemetry(String uavId, double latitude, double longitude) throws Exception {
        Instant deadline = Instant.now().plusSeconds(10);
        while (Instant.now().isBefore(deadline)) {
            try {
                assertThat(queryService.latest(uavId).latitude()).isEqualTo(latitude);
                assertThat(queryService.latest(uavId).longitude()).isEqualTo(longitude);
                return;
            } catch (RuntimeException ex) {
                Thread.sleep(200);
            }
        }
        assertThat(queryService.latest(uavId).latitude()).isEqualTo(latitude);
        assertThat(queryService.latest(uavId).longitude()).isEqualTo(longitude);
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
