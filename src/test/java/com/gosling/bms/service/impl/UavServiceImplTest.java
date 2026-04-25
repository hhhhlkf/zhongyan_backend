package com.gosling.bms.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.gosling.bms.dao.entity.UavRealtimeInfo;
import com.gosling.bms.utils.UdpDataFileStore;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class UavServiceImplTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Test
    void shouldReturnLatestRealtimeInfo() throws Exception {
        Path tempDir = Files.createTempDirectory("uav-service");
        Path filePath = tempDir.resolve("udp_data.json");
        UdpDataFileStore.append(filePath, buildPayload(1710000000L, 114.123456, 30.123456), 10);
        UdpDataFileStore.append(filePath, buildPayload(1710000001L, 114.223456, 30.223456), 10);

        UavServiceImpl service = new UavServiceImpl();
        ReflectionTestUtils.setField(service, "udpDataFilePath", filePath.toString());

        UavRealtimeInfo result = service.getRealtimeInfo();
        assertEquals(1, result.getId());
        assertEquals(1710000001L, result.getData().getTimestamp());
        assertEquals(114.223456, result.getData().getLon(), 0.0);
        assertEquals(30.223456, result.getData().getLat(), 0.0);
    }

    @Test
    void shouldReturnNullWhenNoRealtimeInfoExists() throws Exception {
        Path tempDir = Files.createTempDirectory("uav-service-empty");
        Path filePath = tempDir.resolve("udp_data.json");

        UavServiceImpl service = new UavServiceImpl();
        ReflectionTestUtils.setField(service, "udpDataFilePath", filePath.toString());

        assertNull(service.getRealtimeInfo());
    }

    @Test
    void shouldParsePrefixedRealtimeInfoText() throws Exception {
        Path tempDir = Files.createTempDirectory("uav-service-text");
        Path filePath = tempDir.resolve("udp_data.json");
        String payload = "Latest UAV info: " + buildPayload(1775818036L, 114.30556347290877, 30.591946069480326);
        UdpDataFileStore.append(filePath, OBJECT_MAPPER.writeValueAsString(payload), 10);

        UavServiceImpl service = new UavServiceImpl();
        ReflectionTestUtils.setField(service, "udpDataFilePath", filePath.toString());

        UavRealtimeInfo result = service.getRealtimeInfo();
        assertEquals(1, result.getId());
        assertEquals(1775818036L, result.getData().getTimestamp());
        assertEquals(114.30556347290877, result.getData().getLon(), 0.0);
        assertEquals(30.591946069480326, result.getData().getLat(), 0.0);
    }

    private String buildPayload(long timestamp, double lon, double lat) throws Exception {
        ObjectNode root = OBJECT_MAPPER.createObjectNode();
        root.put("id", 1);
        ObjectNode data = root.putObject("data");
        data.put("lon", lon);
        data.put("lat", lat);
        data.put("alt", 120);
        data.put("velo", 15);
        data.put("yaw", 90);
        data.put("roll", 0);
        data.put("pitch", -10);
        data.put("timestamp", timestamp);
        return OBJECT_MAPPER.writeValueAsString(root);
    }
}
