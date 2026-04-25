package com.gosling.bms.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class UdpDataFileStoreTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Test
    void shouldKeepLatestRecordsWhenAppending() throws Exception {
        Path tempDir = Files.createTempDirectory("udp-data-store-append");
        Path filePath = tempDir.resolve("udp_data.json");

        UdpDataFileStore.append(filePath, buildPayload(1), 3);
        UdpDataFileStore.append(filePath, buildPayload(2), 3);
        UdpDataFileStore.append(filePath, buildPayload(3), 3);
        UdpDataFileStore.append(filePath, buildPayload(4), 3);

        ArrayNode snapshot = UdpDataFileStore.readSnapshot(filePath);
        assertEquals(3, snapshot.size());
        assertEquals(2, snapshot.get(0).get("data").get("timestamp").asInt());
        assertEquals(4, snapshot.get(2).get("data").get("timestamp").asInt());
    }

    @Test
    void shouldTrimExistingFileToLatestRecords() throws Exception {
        Path tempDir = Files.createTempDirectory("udp-data-store-trim");
        Path filePath = tempDir.resolve("udp_data.json");

        ArrayNode existing = OBJECT_MAPPER.createArrayNode();
        existing.add(OBJECT_MAPPER.readTree(buildPayload(10)));
        existing.add(OBJECT_MAPPER.readTree(buildPayload(11)));
        existing.add(OBJECT_MAPPER.readTree(buildPayload(12)));
        existing.add(OBJECT_MAPPER.readTree(buildPayload(13)));
        OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValue(filePath.toFile(), existing);

        UdpDataFileStore.trimToLatest(filePath, 2);

        ArrayNode snapshot = UdpDataFileStore.readSnapshot(filePath);
        assertEquals(2, snapshot.size());
        assertEquals(12, snapshot.get(0).get("data").get("timestamp").asInt());
        assertEquals(13, snapshot.get(1).get("data").get("timestamp").asInt());
    }

    @Test
    void shouldReadLatestRecord() throws Exception {
        Path tempDir = Files.createTempDirectory("udp-data-store-latest");
        Path filePath = tempDir.resolve("udp_data.json");

        UdpDataFileStore.append(filePath, buildPayload(21), 10);
        UdpDataFileStore.append(filePath, buildPayload(22), 10);

        assertEquals(22, UdpDataFileStore.readLatest(filePath).get("data").get("timestamp").asInt());
    }

    @Test
    void shouldReturnNullWhenReadingLatestFromEmptyFile() throws Exception {
        Path tempDir = Files.createTempDirectory("udp-data-store-empty");
        Path filePath = tempDir.resolve("udp_data.json");

        assertNull(UdpDataFileStore.readLatest(filePath));
    }

    private String buildPayload(int timestamp) throws Exception {
        ObjectNode root = OBJECT_MAPPER.createObjectNode();
        root.put("id", 1);
        ObjectNode data = root.putObject("data");
        data.put("lon", 103.1);
        data.put("lat", 31.2);
        data.put("timestamp", timestamp);
        return OBJECT_MAPPER.writeValueAsString(root);
    }
}
