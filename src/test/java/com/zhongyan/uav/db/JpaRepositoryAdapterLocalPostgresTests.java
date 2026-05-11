package com.zhongyan.uav.db;

import com.zhongyan.uav.ZhongyanUavApplication;
import com.zhongyan.uav.asset.domain.Asset;
import com.zhongyan.uav.asset.domain.AssetRepository;
import com.zhongyan.uav.asset.domain.AssetRole;
import com.zhongyan.uav.asset.domain.AssetStatus;
import com.zhongyan.uav.asset.domain.AssetType;
import com.zhongyan.uav.asset.domain.TaskAsset;
import com.zhongyan.uav.asset.domain.TaskAssetRepository;
import com.zhongyan.uav.configcenter.domain.CameraConfigRepository;
import com.zhongyan.uav.configcenter.domain.CameraConfigVersion;
import com.zhongyan.uav.configcenter.domain.ConfigValidation;
import com.zhongyan.uav.configcenter.domain.ConfigValidationRepository;
import com.zhongyan.uav.configcenter.domain.DeviceConfig;
import com.zhongyan.uav.configcenter.domain.DeviceConfigRepository;
import com.zhongyan.uav.configcenter.domain.ModelArtifact;
import com.zhongyan.uav.configcenter.domain.ModelArtifactRepository;
import com.zhongyan.uav.configcenter.domain.ModelConfigRepository;
import com.zhongyan.uav.configcenter.domain.ModelConfigVersion;
import com.zhongyan.uav.configcenter.domain.TransferConfigRepository;
import com.zhongyan.uav.configcenter.domain.TransferConfigVersion;
import com.zhongyan.uav.mission.domain.Mission;
import com.zhongyan.uav.mission.domain.MissionRepository;
import com.zhongyan.uav.mission.domain.MissionStatus;
import com.zhongyan.uav.task.domain.RiskLevel;
import com.zhongyan.uav.task.domain.Task;
import com.zhongyan.uav.task.domain.TaskAttempt;
import com.zhongyan.uav.task.domain.TaskAttemptRepository;
import com.zhongyan.uav.task.domain.TaskCommand;
import com.zhongyan.uav.task.domain.TaskCommandRepository;
import com.zhongyan.uav.task.domain.TaskCommandStatus;
import com.zhongyan.uav.task.domain.TaskCommandType;
import com.zhongyan.uav.task.domain.TaskEvent;
import com.zhongyan.uav.task.domain.TaskEventRepository;
import com.zhongyan.uav.task.domain.TaskEventType;
import com.zhongyan.uav.task.domain.TaskRepository;
import com.zhongyan.uav.task.domain.TaskType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("local")
@SpringBootTest(classes = ZhongyanUavApplication.class, webEnvironment = SpringBootTest.WebEnvironment.NONE)
@EnabledIfEnvironmentVariable(named = "BMS_REPOSITORY_ADAPTER_TESTS", matches = "true")
class JpaRepositoryAdapterLocalPostgresTests {
    @Autowired
    private MissionRepository missionRepository;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private TaskAttemptRepository taskAttemptRepository;

    @Autowired
    private TaskCommandRepository taskCommandRepository;

    @Autowired
    private TaskEventRepository taskEventRepository;

    @Autowired
    private AssetRepository assetRepository;

    @Autowired
    private TaskAssetRepository taskAssetRepository;

    @Autowired
    private DeviceConfigRepository deviceConfigRepository;

    @Autowired
    private CameraConfigRepository cameraConfigRepository;

    @Autowired
    private ModelConfigRepository modelConfigRepository;

    @Autowired
    private ModelArtifactRepository modelArtifactRepository;

    @Autowired
    private TransferConfigRepository transferConfigRepository;

    @Autowired
    private ConfigValidationRepository configValidationRepository;

    @Test
    void jpaAdaptersPersistAndQueryMainChain() {
        String suffix = UUID.randomUUID().toString();
        Instant now = Instant.now();

        Mission mission = missionRepository.save(Mission.draft("mission-" + suffix,
                "local postgres mission", "test", null, 1, "tester", now, "adapter test"));
        assertThat(missionRepository.findById(mission.missionId())).isPresent()
                .get()
                .extracting(Mission::missionId, Mission::name, Mission::status)
                .containsExactly(mission.missionId(), mission.name(), mission.status());
        assertThat(missionRepository.findByStatus(MissionStatus.DRAFT)).extracting(Mission::missionId)
                .contains(mission.missionId());

        Task task = taskRepository.save(Task.draft("task-" + suffix, mission.missionId(),
                TaskType.CAPTURE, 1, "device-" + suffix, "model-" + suffix,
                Map.of("cameraConfigId", "camera-" + suffix), List.of(), "tester", now));
        assertThat(taskRepository.findByMissionId(mission.missionId())).extracting(Task::taskId)
                .containsExactly(task.taskId());

        TaskAttempt attempt = taskAttemptRepository.save(TaskAttempt.started("attempt-" + suffix,
                task.taskId(), 1, "local-node", now));
        assertThat(taskAttemptRepository.findByTaskId(task.taskId())).extracting(TaskAttempt::attemptId)
                .containsExactly(attempt.attemptId());

        TaskCommand command = taskCommandRepository.save(TaskCommand.create("command-" + suffix,
                task.taskId(), mission.missionId(), "device-" + suffix, TaskCommandType.START_CAPTURE,
                Map.of("durationSeconds", 10), "idem-" + suffix, "tester", RiskLevel.HIGH,
                "adapter test", now));
        assertThat(taskCommandRepository.findByIdempotencyKey(command.idempotencyKey())).isPresent()
                .get()
                .extracting(TaskCommand::commandId, TaskCommand::status)
                .containsExactly(command.commandId(), command.status());
        assertThat(taskCommandRepository.findByStatus(TaskCommandStatus.PENDING_APPROVAL))
                .extracting(TaskCommand::commandId)
                .contains(command.commandId());

        TaskEvent event = taskEventRepository.save(TaskEvent.statusChanged("event-" + suffix,
                task.taskId(), TaskEventType.CREATED, null, task.status(), now));
        assertThat(taskEventRepository.findByTaskId(task.taskId())).extracting(TaskEvent::eventId)
                .containsExactly(event.eventId());

        Asset asset = assetRepository.save(Asset.created("asset-" + suffix, mission.missionId(),
                task.taskId(), AssetType.IMAGE, AssetRole.OUTPUT, "adapter image",
                "objects/" + suffix + ".jpg", "image/jpeg", 1024, "sha256-" + suffix,
                Map.of("source", "adapter-test"), "tester", now).markAvailable(now));
        assertThat(assetRepository.findByStatus(AssetStatus.AVAILABLE)).extracting(Asset::assetId)
                .contains(asset.assetId());

        TaskAsset taskAsset = taskAssetRepository.save(new TaskAsset(task.taskId(), asset.assetId(),
                AssetRole.OUTPUT, now));
        assertThat(taskAssetRepository.findByTaskId(task.taskId())).extracting(TaskAsset::assetId)
                .containsExactly(taskAsset.assetId());
    }

    @Test
    void jpaAdaptersPersistAndQueryConfigCenter() {
        String suffix = UUID.randomUUID().toString();
        Instant now = Instant.now();

        DeviceConfig device = deviceConfigRepository.save(DeviceConfig.create("device-" + suffix,
                "RGB Camera", "camera", "192.168.1.101", Map.of("username", "root"),
                Map.of("capture", true), "tester", now));
        assertThat(deviceConfigRepository.findAll()).extracting(DeviceConfig::deviceId)
                .contains(device.deviceId());

        CameraConfigVersion cameraV1 = cameraConfigRepository.save(CameraConfigVersion.create(
                "camera-config-" + suffix, 1, "rgb", Map.of("horizontal", 23.5),
                Map.of("fps", 25), "tester", now));
        CameraConfigVersion cameraV2 = cameraConfigRepository.save(CameraConfigVersion.create(
                cameraV1.cameraConfigId(), 2, "rgb", Map.of("horizontal", 24.0),
                Map.of("fps", 30), "tester", now.plusSeconds(1)));
        assertThat(cameraConfigRepository.findLatestByConfigId(cameraV1.cameraConfigId())).isPresent()
                .get()
                .extracting(CameraConfigVersion::cameraConfigId, CameraConfigVersion::version)
                .containsExactly(cameraV2.cameraConfigId(), cameraV2.version());
        assertThat(cameraConfigRepository.findByConfigId(cameraV1.cameraConfigId()))
                .extracting(CameraConfigVersion::version)
                .containsExactly(cameraV1.version(), cameraV2.version());

        ModelConfigVersion model = modelConfigRepository.save(ModelConfigVersion.create(
                "model-config-" + suffix, 1, "segmentation", "python",
                Map.of("threshold", 0.7), "tester", now));
        ModelArtifact artifact = modelArtifactRepository.save(ModelArtifact.create("artifact-" + suffix,
                model.modelConfigId(), "weights", "models/" + suffix + ".pt", "sha256-" + suffix,
                Map.of(), "tester", now));
        assertThat(modelConfigRepository.findAllLatest()).extracting(ModelConfigVersion::modelConfigId)
                .contains(model.modelConfigId());
        assertThat(modelArtifactRepository.findByModelConfigId(model.modelConfigId()))
                .extracting(ModelArtifact::artifactId)
                .containsExactly(artifact.artifactId());

        TransferConfigVersion transfer = transferConfigRepository.save(TransferConfigVersion.create(
                "transfer-config-" + suffix, 1, "ssh", Map.of("host", "192.168.1.100"),
                Map.of("path", "/data"), "tester", now));
        assertThat(transferConfigRepository.findLatestByConfigId(transfer.transferConfigId())).isPresent()
                .get()
                .extracting(TransferConfigVersion::transferConfigId, TransferConfigVersion::version)
                .containsExactly(transfer.transferConfigId(), transfer.version());

        ConfigValidation validation = configValidationRepository.save(ConfigValidation.passed(
                "validation-" + suffix, "DEVICE", device.deviceId(), now));
        assertThat(configValidationRepository.findByConfig(validation.configType(), validation.configId()))
                .extracting(ConfigValidation::validationId)
                .containsExactly(validation.validationId());
    }
}
