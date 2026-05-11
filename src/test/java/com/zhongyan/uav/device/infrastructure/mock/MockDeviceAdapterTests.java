package com.zhongyan.uav.device.infrastructure.mock;

import com.zhongyan.uav.configcenter.domain.CameraConfigVersion;
import com.zhongyan.uav.configcenter.domain.ModelConfigVersion;
import com.zhongyan.uav.configcenter.domain.TransferConfigVersion;
import com.zhongyan.uav.device.domain.CaptureParameters;
import com.zhongyan.uav.device.domain.ProcessParameters;
import com.zhongyan.uav.device.domain.TransferParameters;
import com.zhongyan.uav.device.port.DeviceCommandExecutor;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class MockDeviceAdapterTests {
    private final Clock clock = Clock.fixed(Instant.parse("2026-04-29T00:00:00Z"), ZoneOffset.UTC);
    private final DeviceCommandExecutor executor = new MockDeviceCommandExecutor(clock);

    @Test
    void mockAdaptersReturnHealthAndCommandResults() {
        CameraConfigVersion camera = CameraConfigVersion.create(
                "camera-rgb", 1, "RGB", Map.of(), Map.of(), "tester", clock.instant()).activate(clock.instant());
        ModelConfigVersion model = ModelConfigVersion.create(
                "model-detect", 1, "DETECTION", "PYTHON", Map.of(), "tester", clock.instant()).activate(clock.instant());
        TransferConfigVersion transfer = TransferConfigVersion.create(
                "transfer-sftp", 1, "SFTP", Map.of(), Map.of(), "tester", clock.instant()).activate(clock.instant());

        MockCameraAdapter cameraAdapter = new MockCameraAdapter(executor, clock);
        MockModelAdapter modelAdapter = new MockModelAdapter(executor, clock);
        MockTransferAdapter transferAdapter = new MockTransferAdapter(executor, clock);

        assertThat(cameraAdapter.check(camera).reachable()).isTrue();
        assertThat(modelAdapter.check(model).reachable()).isTrue();
        assertThat(transferAdapter.check(transfer).reachable()).isTrue();
        assertThat(cameraAdapter.start(camera, new CaptureParameters(Map.of("seconds", 10)),
                "task-1", "command-1").success()).isTrue();
        assertThat(modelAdapter.start(model, new ProcessParameters(Map.of("assetId", "asset-1")),
                "task-2", "command-2").rawOutput()).contains("START_PROCESS");
        assertThat(transferAdapter.start(transfer, new TransferParameters(Map.of("target", "minio")),
                "task-3", "command-3").rawLogObjectKey()).contains("command-3");
    }
}
