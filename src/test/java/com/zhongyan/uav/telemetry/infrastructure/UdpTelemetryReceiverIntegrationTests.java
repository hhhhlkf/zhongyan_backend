package com.zhongyan.uav.telemetry.infrastructure;

import com.zhongyan.uav.telemetry.application.UavTelemetryQueryService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "bms.telemetry.udp.enabled=true",
        "bms.telemetry.udp.port=18089"
})
class UdpTelemetryReceiverIntegrationTests {
    @Autowired
    private UavTelemetryQueryService queryService;

    @Test
    @EnabledIfEnvironmentVariable(named = "BMS_TELEMETRY_UDP_INTEGRATION_TESTS", matches = "true")
    void receivesTelemetryFromLocalUdpSocket() throws Exception {
        String uavId = "uav-udp-it-" + UUID.randomUUID();
        String message = """
                {"uavId":"%s","missionId":"mission-udp-it","latitude":30.62,"longitude":104.07,
                "altitudeMeters":129.0,"speedMetersPerSecond":14.5,"headingDegrees":92.0,
                "reportedAt":"2026-04-29T00:00:01Z"}
                """.formatted(uavId);
        byte[] payload = message.getBytes(StandardCharsets.UTF_8);

        try (DatagramSocket socket = new DatagramSocket()) {
            DatagramPacket packet = new DatagramPacket(payload, payload.length,
                    InetAddress.getByName("127.0.0.1"), 18089);
            socket.send(packet);
        }

        Instant deadline = Instant.now().plusSeconds(10);
        while (Instant.now().isBefore(deadline)) {
            try {
                assertThat(queryService.latest(uavId).longitude()).isEqualTo(104.07);
                return;
            } catch (RuntimeException ex) {
                Thread.sleep(200);
            }
        }

        assertThat(queryService.latest(uavId).longitude()).isEqualTo(104.07);
    }
}
