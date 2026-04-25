package com.gosling.bms.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.gosling.bms.utils.UdpDataFileStore;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UdpReceiverServiceTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final double LON_MIN = 103.7576450;
    private static final double LON_MAX = 103.7593238;
    private static final double LAT_MIN = 31.1094678;
    private static final double LAT_MAX = 31.1109092;
    private static final double OFFSET = 0.00001;

    @Test
    void shouldReceiveUdpPayloadAndKeepLatestCoordinates() throws Exception {
        Path tempDir = Files.createTempDirectory("udp-receiver-service");
        Path filePath = tempDir.resolve("udp_data.json");
        int port = findFreePort();

        UdpReceiverService service = new UdpReceiverService();
        ReflectionTestUtils.setField(service, "maxRecords", 3);
        ReflectionTestUtils.setField(service, "trimIntervalMs", 60000L);
        ReflectionTestUtils.setField(service, "listenPort", port);
        ReflectionTestUtils.setField(service, "udpDataFilePath", filePath.toString());

        List<JsonNode> sentPayloads = new ArrayList<>();
        try {
            service.start();
            Thread.sleep(200);

            Random random = new Random(42L);
            for (int i = 0; i < 5; i++) {
                JsonNode payload = buildRandomPayload(random, 1712563200L + i);
                sentPayloads.add(payload);
                sendUdpPacket(port, payload.toString());
            }

            ArrayNode snapshot = waitForSnapshot(filePath, 3, sentPayloads.get(sentPayloads.size() - 1)
                    .get("data").get("timestamp").asLong());
            assertEquals(3, snapshot.size());

            for (int i = 0; i < 3; i++) {
                JsonNode expected = sentPayloads.get(i + 2);
                JsonNode actual = snapshot.get(i);

                assertEquals(expected.get("id").asLong(), actual.get("id").asLong());
                assertEquals(expected.get("data").get("timestamp").asLong(), actual.get("data").get("timestamp").asLong());
                assertEquals(expected.get("data").get("lon").asDouble(), actual.get("data").get("lon").asDouble(), 0.0);
                assertEquals(expected.get("data").get("lat").asDouble(), actual.get("data").get("lat").asDouble(), 0.0);
                assertCoordinateRange(actual.get("data").get("lon").asDouble(), actual.get("data").get("lat").asDouble());
            }
        } finally {
            service.stop();
        }
    }

    private ArrayNode waitForSnapshot(Path filePath, int expectedSize, long expectedLastTimestamp) throws Exception {
        long deadline = System.currentTimeMillis() + 5000;
        while (System.currentTimeMillis() < deadline) {
            ArrayNode snapshot = UdpDataFileStore.readSnapshot(filePath);
            if (snapshot.size() == expectedSize
                    && snapshot.get(expectedSize - 1).get("data").get("timestamp").asLong() == expectedLastTimestamp) {
                return snapshot;
            }
            Thread.sleep(100);
        }
        throw new AssertionError("Timed out waiting for udp_data.json to reach expected size");
    }

    private void assertCoordinateRange(double lon, double lat) {
        assertTrue(lon >= LON_MIN - OFFSET && lon <= LON_MAX + OFFSET, "lon out of expected range");
        assertTrue(lat >= LAT_MIN - OFFSET && lat <= LAT_MAX + OFFSET, "lat out of expected range");
    }

    private JsonNode buildRandomPayload(Random random, long timestamp) {
        double lon = randomBetween(random, LON_MIN, LON_MAX) + randomOffset(random);
        double lat = randomBetween(random, LAT_MIN, LAT_MAX) + randomOffset(random);

        ObjectNode root = OBJECT_MAPPER.createObjectNode();
        root.put("id", 11011589L);
        ObjectNode data = root.putObject("data");
        data.put("lon", lon);
        data.put("lat", lat);
        data.put("alt", 834);
        data.put("velo", 0);
        data.put("yaw", 242);
        data.put("roll", 0);
        data.put("pitch", -2);
        data.put("timestamp", timestamp);
        return root;
    }

    private double randomBetween(Random random, double min, double max) {
        return min + (max - min) * random.nextDouble();
    }

    private double randomOffset(Random random) {
        return (random.nextDouble() * 2 - 1) * OFFSET;
    }

    private void sendUdpPacket(int port, String payload) throws Exception {
        byte[] body = payload.getBytes(StandardCharsets.UTF_8);
        DatagramPacket packet = new DatagramPacket(body, body.length, InetAddress.getLoopbackAddress(), port);
        try (DatagramSocket socket = new DatagramSocket()) {
            socket.send(packet);
        }
    }

    private int findFreePort() throws Exception {
        try (DatagramSocket socket = new DatagramSocket(0)) {
            return socket.getLocalPort();
        }
    }
}
