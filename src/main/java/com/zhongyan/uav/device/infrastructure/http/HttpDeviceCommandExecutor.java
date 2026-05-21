package com.zhongyan.uav.device.infrastructure.http;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhongyan.uav.device.domain.DeviceCommandPayload;
import com.zhongyan.uav.device.domain.DeviceCommandResult;
import com.zhongyan.uav.device.infrastructure.DevicePayloads;
import com.zhongyan.uav.device.port.DeviceCommandExecutor;
import com.zhongyan.uav.common.resilience.ExternalCallGuard;

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
    private final ExternalCallGuard guard;

    public HttpDeviceCommandExecutor() {
        this(HttpClient.newHttpClient(), new ObjectMapper(), Clock.systemUTC());
    }

    public HttpDeviceCommandExecutor(HttpClient httpClient, ObjectMapper objectMapper, Clock clock) {
        this(httpClient, objectMapper, clock, 1, 250, 3, 30000);
    }

    public HttpDeviceCommandExecutor(HttpClient httpClient, ObjectMapper objectMapper, Clock clock,
                                     int retryMaxAttempts, long retryBackoffMs,
                                     int circuitFailureThreshold, long circuitOpenDurationMs) {
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
        this.clock = clock;
        this.guard = new ExternalCallGuard("device-http", retryMaxAttempts, retryBackoffMs,
                circuitFailureThreshold, circuitOpenDurationMs);
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
            HttpResponse<String> response = guard.execute("send", () -> send(request));
            Duration elapsed = Duration.between(startedAt, clock.instant());
            Map<String, Object> metadata = metadata(response, elapsed);
            boolean success = response.statusCode() >= 200 && response.statusCode() < 300;
            return new DeviceCommandResult(payload.commandId(), payload.taskId(), payload.deviceId(),
                    success, String.valueOf(response.statusCode()),
                    success ? "http command executed" : "http command failed",
                    response.body(), null, elapsed, metadata, clock.instant());
        } catch (IOException ex) {
            return failure(payload, "HTTP_IO_ERROR", "HTTP command I/O failure: " + ex.getMessage());
        } catch (HttpCommandException ex) {
            Throwable cause = ex.getCause();
            if (cause instanceof InterruptedException) {
                Thread.currentThread().interrupt();
                return failure(payload, "HTTP_INTERRUPTED", "HTTP command interrupted: " + cause.getMessage());
            }
            return failure(payload, "HTTP_IO_ERROR", "HTTP command I/O failure: " + cause.getMessage());
        } catch (IllegalArgumentException ex) {
            return failure(payload, "HTTP_BAD_REQUEST", "HTTP command request is invalid: " + ex.getMessage());
        } catch (IllegalStateException ex) {
            return failure(payload, "HTTP_CIRCUIT_OPEN", ex.getMessage());
        }
    }

    private HttpResponse<String> send(HttpRequest request) {
        try {
            return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException ex) {
            throw new HttpCommandException(ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new HttpCommandException(ex);
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

    private DeviceCommandResult failure(DeviceCommandPayload payload, String exitCode, String message) {
        return DeviceCommandResult.failure(payload, exitCode, message,
                "", Map.of("protocol", "HTTP", "failureClass", exitCode), clock.instant());
    }

    private static class HttpCommandException extends RuntimeException {
        private HttpCommandException(Throwable cause) {
            super(cause);
        }
    }
}
