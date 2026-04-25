package com.gosling.bms.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gosling.bms.dao.entity.UavRealtimeInfo;
import com.gosling.bms.service.UavService;
import com.gosling.bms.utils.UdpDataFileStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
@Slf4j
public class UavServiceImpl implements UavService {

    private static final String UAV_INFO_PREFIX = "Latest UAV info:";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Value("${udp-data.file-path:src/main/resources/static/udp_data.json}")
    private String udpDataFilePath;

    @Override
    public UavRealtimeInfo getRealtimeInfo() {
        Path filePath = Paths.get(udpDataFilePath);
        try {
            JsonNode latest = UdpDataFileStore.readLatest(filePath);
            if (latest == null) {
                log.warn("No realtime UAV info found in {}", filePath);
                return null;
            }
            UavRealtimeInfo realtimeInfo = parseRealtimeInfo(latest);
            log.info("Latest UAV info: {}", realtimeInfo);
            return realtimeInfo;
        } catch (IOException e) {
            log.error("Failed to read realtime UAV info from {}", filePath, e);
            return null;
        }
    }

    static UavRealtimeInfo parseRealtimeInfo(JsonNode latest) throws IOException {
        if (latest == null || latest.isNull()) {
            return null;
        }
        if (latest.isTextual()) {
            return parseRealtimeInfo(latest.asText());
        }
        return OBJECT_MAPPER.treeToValue(latest, UavRealtimeInfo.class);
    }

    static UavRealtimeInfo parseRealtimeInfo(String rawPayload) throws IOException {
        if (rawPayload == null) {
            return null;
        }
        String payload = rawPayload.trim();
        if (payload.isEmpty()) {
            return null;
        }
        int jsonStart = payload.indexOf('{');
        if (payload.startsWith(UAV_INFO_PREFIX) && jsonStart >= 0) {
            payload = payload.substring(jsonStart);
        } else if (!payload.startsWith("{") && jsonStart >= 0) {
            payload = payload.substring(jsonStart);
        }
        return OBJECT_MAPPER.readValue(payload, UavRealtimeInfo.class);
    }
}
