package com.zhongyan.uav.common.config;

import com.zhongyan.uav.asset.domain.AssetRepository;
import com.zhongyan.uav.asset.domain.TaskAssetRepository;
import com.zhongyan.uav.asset.infrastructure.mock.InMemoryAssetRepository;
import com.zhongyan.uav.asset.infrastructure.mock.InMemoryTaskAssetRepository;
import com.zhongyan.uav.configcenter.domain.CameraConfigRepository;
import com.zhongyan.uav.configcenter.domain.ConfigValidationRepository;
import com.zhongyan.uav.configcenter.domain.DeviceConfigRepository;
import com.zhongyan.uav.configcenter.domain.ModelArtifactRepository;
import com.zhongyan.uav.configcenter.domain.ModelConfigRepository;
import com.zhongyan.uav.configcenter.domain.TransferConfigRepository;
import com.zhongyan.uav.configcenter.infrastructure.mock.InMemoryCameraConfigRepository;
import com.zhongyan.uav.configcenter.infrastructure.mock.InMemoryConfigValidationRepository;
import com.zhongyan.uav.configcenter.infrastructure.mock.InMemoryDeviceConfigRepository;
import com.zhongyan.uav.configcenter.infrastructure.mock.InMemoryModelArtifactRepository;
import com.zhongyan.uav.configcenter.infrastructure.mock.InMemoryModelConfigRepository;
import com.zhongyan.uav.configcenter.infrastructure.mock.InMemoryTransferConfigRepository;
import com.zhongyan.uav.event.infrastructure.mock.InMemoryOutboxRepository;
import com.zhongyan.uav.event.port.OutboxRepository;
import com.zhongyan.uav.mission.domain.MissionRepository;
import com.zhongyan.uav.mission.infrastructure.mock.InMemoryMissionRepository;
import com.zhongyan.uav.task.domain.TaskAttemptRepository;
import com.zhongyan.uav.task.domain.TaskCommandRepository;
import com.zhongyan.uav.task.domain.TaskEventRepository;
import com.zhongyan.uav.task.domain.TaskRepository;
import com.zhongyan.uav.task.infrastructure.mock.InMemoryTaskAttemptRepository;
import com.zhongyan.uav.task.infrastructure.mock.InMemoryTaskCommandRepository;
import com.zhongyan.uav.task.infrastructure.mock.InMemoryTaskEventRepository;
import com.zhongyan.uav.task.infrastructure.mock.InMemoryTaskRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(prefix = "bms.repository", name = "mode", havingValue = "mock", matchIfMissing = true)
public class MockApplicationConfig {
    @Bean
    public MissionRepository missionRepository() {
        return new InMemoryMissionRepository();
    }

    @Bean
    public TaskRepository taskRepository() {
        return new InMemoryTaskRepository();
    }

    @Bean
    public TaskCommandRepository taskCommandRepository() {
        return new InMemoryTaskCommandRepository();
    }

    @Bean
    public TaskEventRepository taskEventRepository(OutboxRepository outboxRepository) {
        return new InMemoryTaskEventRepository(outboxRepository);
    }

    @Bean
    public TaskAttemptRepository taskAttemptRepository() {
        return new InMemoryTaskAttemptRepository();
    }

    @Bean
    public AssetRepository assetRepository() {
        return new InMemoryAssetRepository();
    }

    @Bean
    public TaskAssetRepository taskAssetRepository() {
        return new InMemoryTaskAssetRepository();
    }

    @Bean
    public DeviceConfigRepository deviceConfigRepository() {
        return new InMemoryDeviceConfigRepository();
    }

    @Bean
    public CameraConfigRepository cameraConfigRepository() {
        return new InMemoryCameraConfigRepository();
    }

    @Bean
    public ModelConfigRepository modelConfigRepository() {
        return new InMemoryModelConfigRepository();
    }

    @Bean
    public ModelArtifactRepository modelArtifactRepository() {
        return new InMemoryModelArtifactRepository();
    }

    @Bean
    public TransferConfigRepository transferConfigRepository() {
        return new InMemoryTransferConfigRepository();
    }

    @Bean
    public ConfigValidationRepository configValidationRepository() {
        return new InMemoryConfigValidationRepository();
    }

    @Bean
    public OutboxRepository outboxRepository() {
        return new InMemoryOutboxRepository();
    }
}
