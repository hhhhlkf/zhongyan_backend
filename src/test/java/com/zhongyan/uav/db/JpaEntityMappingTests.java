package com.zhongyan.uav.db;

import com.zhongyan.uav.asset.domain.Asset;
import com.zhongyan.uav.asset.domain.AssetRole;
import com.zhongyan.uav.asset.domain.AssetStatus;
import com.zhongyan.uav.asset.domain.AssetType;
import com.zhongyan.uav.asset.domain.GeoStatus;
import com.zhongyan.uav.asset.domain.TaskAsset;
import com.zhongyan.uav.asset.infrastructure.jpa.JpaAssetEntity;
import com.zhongyan.uav.asset.infrastructure.jpa.JpaTaskAssetEntity;
import com.zhongyan.uav.configcenter.domain.CameraConfigVersion;
import com.zhongyan.uav.configcenter.domain.ConfigValidation;
import com.zhongyan.uav.configcenter.domain.DeviceConfig;
import com.zhongyan.uav.configcenter.domain.ModelArtifact;
import com.zhongyan.uav.configcenter.domain.ModelConfigVersion;
import com.zhongyan.uav.configcenter.domain.TransferConfigVersion;
import com.zhongyan.uav.configcenter.infrastructure.jpa.JpaCameraConfigEntity;
import com.zhongyan.uav.configcenter.infrastructure.jpa.JpaConfigValidationEntity;
import com.zhongyan.uav.configcenter.infrastructure.jpa.JpaDeviceConfigEntity;
import com.zhongyan.uav.configcenter.infrastructure.jpa.JpaModelArtifactEntity;
import com.zhongyan.uav.configcenter.infrastructure.jpa.JpaModelConfigEntity;
import com.zhongyan.uav.configcenter.infrastructure.jpa.JpaTransferConfigEntity;
import com.zhongyan.uav.event.domain.EventEnvelope;
import com.zhongyan.uav.event.domain.EventType;
import com.zhongyan.uav.event.domain.OutboxStatus;
import com.zhongyan.uav.event.infrastructure.jpa.JpaOutboxEntity;
import com.zhongyan.uav.mission.domain.Mission;
import com.zhongyan.uav.mission.domain.MissionRegion;
import com.zhongyan.uav.mission.infrastructure.jpa.JpaMissionEntity;
import com.zhongyan.uav.task.domain.RiskLevel;
import com.zhongyan.uav.task.domain.Task;
import com.zhongyan.uav.task.domain.TaskAttempt;
import com.zhongyan.uav.task.domain.TaskCommand;
import com.zhongyan.uav.task.domain.TaskCommandType;
import com.zhongyan.uav.task.domain.TaskEvent;
import com.zhongyan.uav.task.domain.TaskEventType;
import com.zhongyan.uav.task.domain.TaskStatus;
import com.zhongyan.uav.task.domain.TaskType;
import com.zhongyan.uav.task.infrastructure.jpa.JpaTaskAttemptEntity;
import com.zhongyan.uav.task.infrastructure.jpa.JpaTaskCommandEntity;
import com.zhongyan.uav.task.infrastructure.jpa.JpaTaskEntity;
import com.zhongyan.uav.task.infrastructure.jpa.JpaTaskEventEntity;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class JpaEntityMappingTests {
    private static final Instant NOW = Instant.parse("2026-04-29T08:00:00Z");

    @Test
    void mapsMissionTaskCommandEventAttemptAndAssetEntities() {
        Mission mission = Mission.draft("mission-jpa-map", "mapping mission", "test",
                new MissionRegion("510100", "Chengdu", 104.06, 30.67, "{\"type\":\"Polygon\"}"),
                5, "tester", NOW, "mapping");
        assertThat(JpaMissionEntity.fromDomain(mission).toDomain()).isEqualTo(mission);

        Task task = Task.draft("task-jpa-map", mission.missionId(), TaskType.CAPTURE, 10,
                "device-1", "model-1", Map.of("camera", "rgb"),
                List.of("asset-input"), "tester", NOW).transitionTo(TaskStatus.QUEUED, NOW);
        assertThat(JpaTaskEntity.fromDomain(task).toDomain()).isEqualTo(task);

        TaskAttempt attempt = TaskAttempt.started("attempt-jpa-map", task.taskId(), 1,
                "node-a", NOW).complete(NOW.plusSeconds(5), "logs/attempt.log");
        assertThat(JpaTaskAttemptEntity.fromDomain(attempt).toDomain()).isEqualTo(attempt);

        TaskCommand command = TaskCommand.create("command-jpa-map", task.taskId(), mission.missionId(),
                "device-1", TaskCommandType.START_CAPTURE, Map.of("duration", 10),
                "idem-jpa-map", "tester", RiskLevel.HIGH, "mapping", NOW);
        assertThat(JpaTaskCommandEntity.fromDomain(command).toDomain()).isEqualTo(command);

        TaskEvent event = new TaskEvent("event-jpa-map", task.taskId(), TaskEventType.SUBMITTED,
                TaskStatus.DRAFT, TaskStatus.QUEUED, Map.of("source", "test"), NOW);
        assertThat(JpaTaskEventEntity.fromDomain(event).toDomain()).isEqualTo(event);

        EventEnvelope envelope = new EventEnvelope("outbox-jpa-map", "TASK", task.taskId(),
                EventType.TASK_EVENT, "task-events", task.taskId(), Map.of("source", "test"),
                Map.of("traceId", "trace-1"), OutboxStatus.PENDING, NOW, null, 0, null);
        assertThat(JpaOutboxEntity.fromDomain(envelope).toDomain()).isEqualTo(envelope);

        Asset asset = new Asset("asset-jpa-map", mission.missionId(), task.taskId(),
                AssetType.IMAGE, AssetRole.OUTPUT, AssetStatus.AVAILABLE, GeoStatus.PENDING,
                "rgb image", "objects/rgb.jpg", "image/jpeg", 1234,
                "sha256", "previews/rgb.jpg", null, Map.of("band", "rgb"),
                "tester", NOW, NOW.plusSeconds(1));
        assertThat(JpaAssetEntity.fromDomain(asset).toDomain()).isEqualTo(asset);

        TaskAsset taskAsset = new TaskAsset(task.taskId(), asset.assetId(), AssetRole.OUTPUT, NOW);
        assertThat(JpaTaskAssetEntity.fromDomain(taskAsset).toDomain()).isEqualTo(taskAsset);

        assertThat(task.progress()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void mapsConfigCenterEntities() {
        DeviceConfig device = DeviceConfig.create("device-jpa-map", "RGB Camera", "camera",
                "192.168.1.101", Map.of("username", "root"),
                Map.of("capture", true), "tester", NOW);
        assertThat(JpaDeviceConfigEntity.fromDomain(device).toDomain()).isEqualTo(device);

        CameraConfigVersion camera = CameraConfigVersion.create("camera-config-jpa-map", 2,
                "rgb", Map.of("horizontal", 23.5), Map.of("fps", 25), "tester", NOW);
        assertThat(JpaCameraConfigEntity.fromDomain(camera).toDomain()).isEqualTo(camera);

        ModelConfigVersion model = ModelConfigVersion.create("model-config-jpa-map", 3,
                "segmentation", "python", Map.of("threshold", 0.7), "tester", NOW);
        assertThat(JpaModelConfigEntity.fromDomain(model).toDomain()).isEqualTo(model);

        TransferConfigVersion transfer = TransferConfigVersion.create("transfer-config-jpa-map", 4,
                "ssh", Map.of("host", "192.168.1.100"), Map.of("path", "/data"), "tester", NOW);
        assertThat(JpaTransferConfigEntity.fromDomain(transfer).toDomain()).isEqualTo(transfer);

        ModelArtifact artifact = ModelArtifact.create("artifact-jpa-map", model.modelConfigId(),
                "weights", "models/model.pt", "sha256", Map.of("size", 1024), "tester", NOW);
        assertThat(JpaModelArtifactEntity.fromDomain(artifact).toDomain()).isEqualTo(artifact);

        ConfigValidation validation = ConfigValidation.failed("validation-jpa-map", "MODEL",
                model.modelConfigId(), List.of("missing artifact"), NOW);
        assertThat(JpaConfigValidationEntity.fromDomain(validation).toDomain()).isEqualTo(validation);
    }
}
