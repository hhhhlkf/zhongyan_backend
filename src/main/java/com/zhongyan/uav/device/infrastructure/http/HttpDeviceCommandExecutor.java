package com.zhongyan.uav.device.infrastructure.http;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhongyan.uav.device.domain.DeviceCommandPayload;
import com.zhongyan.uav.device.domain.DeviceCommandResult;
import com.zhongyan.uav.device.infrastructure.DevicePayloads;
import com.zhongyan.uav.device.port.DeviceCommandExecutor;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

public class HttpDeviceCommandExecutor implements DeviceCommandExecutor {
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public HttpDeviceCommandExecutor() {
        this(HttpClient.newHttpClient(), new ObjectMapper(), Clock.systemUTC());
    }

    public HttpDeviceCommandExecutor(HttpClient httpClient, ObjectMapper objectMapper, Clock clock) {
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    @Override
    public DeviceCommandResult execute(DeviceCommandPayload payload) {
        String url = DevicePayloads.text(payload.parameters(), null, "url", "endpoint");
        if (url == null) {
            return DeviceCommandResult.failure(payload, "HTTP_BAD_REQUEST", "HTTP url is required",
                    "", Map.of("protocol", "HTTP"), clock.instant());
        }

        Instant startedAt = clock.instant();
        try {
            HttpRequest request = request(payload, url);
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            Duration elapsed = Duration.between(startedAt, clock.instant());
            Map<String, Object> metadata = metadata(response, elapsed);
            boolean success = response.statusCode() >= 200 && response.statusCode() < 300;
            return new DeviceCommandResult(payload.commandId(), payload.taskId(), payload.deviceId(),
                    success, String.valueOf(response.statusCode()),
                    success ? "http command executed" : "http command failed",
                    response.body(), null, elapsed, metadata, clock.instant());
        } catch (IOException ex) {
            return DeviceCommandResult.failure(payload, "HTTP_IO_ERROR", ex.getMessage(),
                    "", Map.of("protocol", "HTTP"), clock.instant());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return DeviceCommandResult.failure(payload, "HTTP_INTERRUPTED", ex.getMessage(),
                    "", Map.of("protocol", "HTTP"), clock.instant());
        }
    }

    private HttpRequest request(DeviceCommandPayload payload, String url) throws JsonProcessingException {
        Map<String, Object> parameters = payload.parameters();
        String method = DevicePayloads.text(parameters, "POST", "method", "httpMethod").toUpperCase();
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(url)).timeout(payload.timeout());
        DevicePayloads.stringMap(parameters.get("headers"))
                .forEach((key, value) -> builder.header(key, String.valueOf(value)));
        if (!DevicePayloads.stringMap(parameters.get("headers")).containsKey("Content-Type")) {
            builder.header("Content-Type", "application/json");
        }

        String body = body(parameters);
        if ("GET".equals(method)) {
            builder.GET();
        } else if ("DELETE".equals(method)) {
            builder.DELETE();
        } else {
            builder.method(method, HttpRequest.BodyPublishers.ofString(body));
        }
        return builder.build();
    }

    private String body(Map<String, Object> parameters) throws JsonProcessingException {
        Object explicitBody = parameters.get("body");
        if (explicitBody instanceof String text) {
            return text;
        }
        if (explicitBody != null) {
            return objectMapper.writeValueAsString(explicitBody);
        }
        return objectMapper.writeValueAsString(parameters);
    }

    private Map<String, Object> metadata(HttpResponse<String> response, Duration elapsed) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("protocol", "HTTP");
        metadata.put("statusCode", response.statusCode());
        metadata.put("elapsedMs", elapsed.toMillis());
        return metadata;
    }
}
