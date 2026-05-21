package com.zhongyan.uav.device.infrastructure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import com.zhongyan.uav.configcenter.domain.CameraConfigVersion;
import com.zhongyan.uav.configcenter.domain.ModelConfigVersion;
import com.zhongyan.uav.configcenter.domain.TransferConfigVersion;
import com.zhongyan.uav.device.domain.CaptureParameters;
import com.zhongyan.uav.device.domain.DeviceCommandPayload;
import com.zhongyan.uav.device.domain.DeviceCommandResult;
import com.zhongyan.uav.device.domain.DeviceProtocol;
import com.zhongyan.uav.device.domain.ProcessParameters;
import com.zhongyan.uav.device.domain.TransferParameters;
import com.zhongyan.uav.device.infrastructure.http.HttpDeviceCommandExecutor;
import com.zhongyan.uav.device.infrastructure.ssh.SshCameraAdapter;
import com.zhongyan.uav.device.infrastructure.ssh.SshDeviceCommandExecutor;
import com.zhongyan.uav.device.infrastructure.ssh.SshModelAdapter;
import com.zhongyan.uav.device.infrastructure.ssh.SshTransferAdapter;
import com.zhongyan.uav.device.port.DeviceCommandExecutor;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class RealDeviceAdapterTests {
    private final Clock clock = Clock.fixed(Instant.parse("2026-04-29T00:00:00Z"), ZoneOffset.UTC);

    @Test
    void sshAdaptersBuildPayloadsFromConfigCommands() {
        CapturingExecutor executor = new CapturingExecutor(clock);
        CameraConfigVersion camera = CameraConfigVersion.create("camera-rgb", 1, "RGB",
                Map.of(), Map.of("host", "192.168.1.101", "username", "root",
                        "start-command", "camera-start", "stop-command", "camera-stop"),
                "tester", clock.instant()).activate(clock.instant());
        ModelConfigVersion model = ModelConfigVersion.create("model-detect", 1, "DETECTION", "PYTHON",
                Map.of("host", "192.168.1.102", "start-command", "model-start"),
                "tester", clock.instant()).activate(clock.instant());
        TransferConfigVersion transfer = TransferConfigVersion.create("transfer-share", 1, "SSH",
                Map.of("host", "192.168.1.103"),
                Map.of("start-command", "transfer-start"), "tester", clock.instant()).activate(clock.instant());

        new SshCameraAdapter(executor, clock).start(camera, new CaptureParameters(Map.of("seconds", 10)),
                "task-1", "command-1");
        assertThat(executor.lastPayload.protocol()).isEqualTo(DeviceProtocol.SSH);
        assertThat(executor.lastPayload.parameters()).containsEntry("command", "camera-start")
                .containsEntry("host", "192.168.1.101")
                .containsEntry("seconds", 10);

        new SshModelAdapter(executor, clock).start(model, new ProcessParameters(Map.of("assetId", "asset-1")),
                "task-2", "command-2");
        assertThat(executor.lastPayload.parameters()).containsEntry("command", "model-start")
                .containsEntry("assetId", "asset-1");

        new SshTransferAdapter(executor, clock).start(transfer, new TransferParameters(Map.of("target", "minio")),
                "task-3", "command-3");
        assertThat(executor.lastPayload.parameters()).containsEntry("command", "transfer-start")
                .containsEntry("host", "192.168.1.103")
                .containsEntry("target", "minio");
    }

    @Test
    void sshExecutorReturnsFailureWhenRequiredPayloadIsMissing() {
        DeviceCommandPayload payload = new DeviceCommandPayload("command-1", "task-1", "device-1",
                com.zhongyan.uav.task.domain.TaskCommandType.START_CAPTURE, DeviceProtocol.SSH,
                Map.of("host", "127.0.0.1"), null);

        DeviceCommandResult result = new SshDeviceCommandExecutor(clock).execute(payload);

        assertThat(result.success()).isFalse();
        assertThat(result.exitCode()).isEqualTo("SSH_BAD_REQUEST");
    }

    @Test
    void httpExecutorPostsPayloadToEndpoint() throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/command", exchange -> {
            byte[] response = "{\"ok\":true}".getBytes(java.nio.charset.StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.start();
        try {
            String url = "http://127.0.0.1:" + server.getAddress().getPort() + "/command";
            DeviceCommandPayload payload = new DeviceCommandPayload("command-1", "task-1", "device-1",
                    com.zhongyan.uav.task.domain.TaskCommandType.START_CAPTURE, DeviceProtocol.HTTP,
                    Map.of("url", url, "body", Map.of("action", "start")), null);

            DeviceCommandResult result = new HttpDeviceCommandExecutor(HttpClient.newHttpClient(),
                    new ObjectMapper(), clock).execute(payload);

            assertThat(result.success()).isTrue();
            assertThat(result.exitCode()).isEqualTo("200");
            assertThat(result.rawOutput()).contains("ok");
            assertThat(result.metadata()).containsEntry("protocol", "HTTP");
        } finally {
            server.stop(0);
        }
    }

    @Test
    void httpExecutorClassifiesOfflineDeviceAndOpensCircuit() {
        DeviceCommandPayload payload = new DeviceCommandPayload("command-1", "task-1", "device-1",
                com.zhongyan.uav.task.domain.TaskCommandType.START_CAPTURE, DeviceProtocol.HTTP,
                Map.of("url", "http://127.0.0.1:9/device-command", "body", Map.of("action", "start")),
                null);
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(java.time.Duration.ofMillis(200))
                .build();
        HttpDeviceCommandExecutor executor = new HttpDeviceCommandExecutor(client,
                new ObjectMapper(), clock, 2, 1, 1, 30_000);

        DeviceCommandResult first = executor.execute(payload);
        DeviceCommandResult second = executor.execute(payload);

        assertThat(first.success()).isFalse();
        assertThat(first.exitCode()).isEqualTo("HTTP_IO_ERROR");
        assertThat(first.metadata()).containsEntry("failureClass", "HTTP_IO_ERROR");
        assertThat(second.success()).isFalse();
        assertThat(second.exitCode()).isEqualTo("HTTP_CIRCUIT_OPEN");
        assertThat(second.metadata()).containsEntry("failureClass", "HTTP_CIRCUIT_OPEN");
    }

    private static class CapturingExecutor implements DeviceCommandExecutor {
        private final Clock clock;
        private DeviceCommandPayload lastPayload;

        private CapturingExecutor(Clock clock) {
            this.clock = clock;
        }

        @Override
        public DeviceCommandResult execute(DeviceCommandPayload payload) {
            lastPayload = payload;
            return DeviceCommandResult.success(payload, "captured", "captured",
                    Map.of("protocol", payload.protocol().name()), clock.instant());
        }
    }
}
