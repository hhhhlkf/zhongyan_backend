package com.zhongyan.uav.common.config;

import com.zhongyan.uav.asset.domain.AssetRepository;
import com.zhongyan.uav.asset.domain.TaskAssetRepository;
import com.zhongyan.uav.asset.application.AssetApplicationService;
import com.zhongyan.uav.asset.application.AssetQueryService;
import com.zhongyan.uav.asset.application.PreviewApplicationService;
import com.zhongyan.uav.asset.port.AssetStoragePort;
import com.zhongyan.uav.asset.port.PreviewGeneratorPort;
import com.zhongyan.uav.asset.infrastructure.mock.InMemoryAssetStorage;
import com.zhongyan.uav.configcenter.application.CameraConfigApplicationService;
import com.zhongyan.uav.configcenter.application.ConfigValidationService;
import com.zhongyan.uav.configcenter.application.DeviceConfigApplicationService;
import com.zhongyan.uav.configcenter.application.ModelConfigApplicationService;
import com.zhongyan.uav.configcenter.application.TransferConfigApplicationService;
import com.zhongyan.uav.configcenter.domain.CameraConfigRepository;
import com.zhongyan.uav.configcenter.domain.ConfigValidationRepository;
import com.zhongyan.uav.configcenter.domain.DeviceConfigRepository;
import com.zhongyan.uav.configcenter.domain.ModelArtifactRepository;
import com.zhongyan.uav.configcenter.domain.ModelConfigRepository;
import com.zhongyan.uav.configcenter.domain.TransferConfigRepository;
import com.zhongyan.uav.device.application.DeviceCommandService;
import com.zhongyan.uav.device.port.CameraAdapter;
import com.zhongyan.uav.device.port.DeviceCommandExecutor;
import com.zhongyan.uav.device.port.ModelAdapter;
import com.zhongyan.uav.device.port.TransferAdapter;
import com.zhongyan.uav.event.application.DeadLetterEventService;
import com.zhongyan.uav.event.application.EventReplayService;
import com.zhongyan.uav.event.application.OutboxPublishService;
import com.zhongyan.uav.event.port.EventPublisher;
import com.zhongyan.uav.event.port.OutboxRepository;
import com.zhongyan.uav.geo.application.GeoBoundaryApplicationService;
import com.zhongyan.uav.geo.application.LayerPublishApplicationService;
import com.zhongyan.uav.geo.infrastructure.mock.MockGeoServerPort;
import com.zhongyan.uav.geo.infrastructure.mock.MockGroundElevationPort;
import com.zhongyan.uav.geo.port.GeoServerPort;
import com.zhongyan.uav.geo.port.GroundElevationPort;
import com.zhongyan.uav.mission.application.MissionApplicationService;
import com.zhongyan.uav.mission.application.MissionQueryService;
import com.zhongyan.uav.mission.domain.MissionRepository;
import com.zhongyan.uav.realtime.application.RealtimePushService;
import com.zhongyan.uav.realtime.infrastructure.SseRealtimePushService;
import com.zhongyan.uav.task.application.TaskApplicationService;
import com.zhongyan.uav.task.application.TaskApprovalService;
import com.zhongyan.uav.task.application.TaskCommandApplicationService;
import com.zhongyan.uav.task.application.TaskQueryService;
import com.zhongyan.uav.task.application.TaskSchedulerService;
import com.zhongyan.uav.task.domain.TaskAttemptRepository;
import com.zhongyan.uav.task.domain.TaskCommandRepository;
import com.zhongyan.uav.task.domain.TaskEventRepository;
import com.zhongyan.uav.task.domain.TaskRepository;
import com.zhongyan.uav.task.executor.CaptureTaskExecutor;
import com.zhongyan.uav.task.executor.AgentAnalysisTaskExecutor;
import com.zhongyan.uav.task.executor.DemoPlaybackTaskExecutor;
import com.zhongyan.uav.task.executor.GeoBoundaryTaskExecutor;
import com.zhongyan.uav.task.executor.PreviewTaskExecutor;
import com.zhongyan.uav.task.executor.ProcessTaskExecutor;
import com.zhongyan.uav.task.executor.PublishLayerTaskExecutor;
import com.zhongyan.uav.task.executor.ReportGenerationTaskExecutor;
import com.zhongyan.uav.task.executor.TaskExecutor;
import com.zhongyan.uav.task.executor.TransferTaskExecutor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;

import java.time.Clock;
import java.util.List;

@Configuration
public class ApplicationServiceConfig {
    @Bean
    public MissionApplicationService missionApplicationService(MissionRepository missionRepository) {
        return new MissionApplicationService(missionRepository);
    }

    @Bean
    public MissionQueryService missionQueryService(MissionRepository missionRepository) {
        return new MissionQueryService(missionRepository);
    }

    @Bean
    public TaskApplicationService taskApplicationService(TaskRepository taskRepository,
                                                         MissionRepository missionRepository,
                                                         TaskEventRepository taskEventRepository,
                                                         TaskAttemptRepository taskAttemptRepository) {
        return new TaskApplicationService(taskRepository, missionRepository, taskEventRepository,
                taskAttemptRepository);
    }

    @Bean
    public TaskCommandApplicationService taskCommandApplicationService(TaskRepository taskRepository,
                                                                      TaskCommandRepository taskCommandRepository,
                                                                      TaskEventRepository taskEventRepository) {
        return new TaskCommandApplicationService(taskRepository, taskCommandRepository, taskEventRepository);
    }

    @Bean
    public TaskApprovalService taskApprovalService(TaskCommandRepository taskCommandRepository,
                                                   TaskEventRepository taskEventRepository) {
        return new TaskApprovalService(taskCommandRepository, taskEventRepository);
    }

    @Bean
    public TaskQueryService taskQueryService(TaskRepository taskRepository,
                                             TaskCommandRepository taskCommandRepository,
                                             TaskEventRepository taskEventRepository,
                                             TaskAttemptRepository taskAttemptRepository,
                                             AssetRepository assetRepository) {
        return new TaskQueryService(taskRepository, taskCommandRepository, taskEventRepository,
                taskAttemptRepository, assetRepository);
    }

    @Bean
    @ConditionalOnMissingBean(AssetStoragePort.class)
    public AssetStoragePort assetStoragePort() {
        return new InMemoryAssetStorage();
    }

    @Bean
    public AssetApplicationService assetApplicationService(AssetRepository assetRepository,
                                                           TaskAssetRepository taskAssetRepository) {
        return new AssetApplicationService(assetRepository, taskAssetRepository);
    }

    @Bean
    public AssetQueryService assetQueryService(AssetRepository assetRepository,
                                               AssetStoragePort assetStoragePort) {
        return new AssetQueryService(assetRepository, assetStoragePort);
    }

    @Bean
    public PreviewApplicationService previewApplicationService(AssetRepository assetRepository,
                                                               PreviewGeneratorPort previewGeneratorPort) {
        return new PreviewApplicationService(assetRepository, previewGeneratorPort);
    }

    @Bean
    @ConditionalOnMissingBean(GroundElevationPort.class)
    public GroundElevationPort groundElevationPort() {
        return new MockGroundElevationPort();
    }

    @Bean
    @ConditionalOnMissingBean(GeoServerPort.class)
    public GeoServerPort geoServerPort() {
        return new MockGeoServerPort();
    }

    @Bean
    public GeoBoundaryApplicationService geoBoundaryApplicationService(AssetRepository assetRepository,
                                                                       GroundElevationPort groundElevationPort) {
        return new GeoBoundaryApplicationService(assetRepository, groundElevationPort);
    }

    @Bean
    public LayerPublishApplicationService layerPublishApplicationService(AssetRepository assetRepository,
                                                                         GeoServerPort geoServerPort) {
        return new LayerPublishApplicationService(assetRepository, geoServerPort);
    }

    @Bean
    public RealtimePushService realtimePushService() {
        return new SseRealtimePushService();
    }

    @Bean
    public DeadLetterEventService deadLetterEventService(OutboxRepository outboxRepository) {
        return new DeadLetterEventService(outboxRepository);
    }

    @Bean
    public OutboxPublishService outboxPublishService(OutboxRepository outboxRepository,
                                                     EventPublisher eventPublisher,
                                                     RealtimePushService realtimePushService,
                                                     DeadLetterEventService deadLetterEventService) {
        return new OutboxPublishService(outboxRepository, eventPublisher, realtimePushService,
                deadLetterEventService, Clock.systemUTC(), 3);
    }

    @Bean
    public EventReplayService eventReplayService(OutboxRepository outboxRepository,
                                                 RealtimePushService realtimePushService) {
        return new EventReplayService(outboxRepository, realtimePushService);
    }

    @Bean
    public TaskSchedulerService taskSchedulerService(TaskRepository taskRepository,
                                                     TaskAttemptRepository taskAttemptRepository,
                                                     TaskCommandRepository taskCommandRepository,
                                                     TaskEventRepository taskEventRepository,
                                                     TaskCommandApplicationService taskCommandApplicationService,
                                                     TaskApplicationService taskApplicationService,
                                                     List<TaskExecutor> taskExecutors) {
        return new TaskSchedulerService(taskRepository, taskAttemptRepository, taskCommandRepository, taskEventRepository,
                taskCommandApplicationService, taskApplicationService, taskExecutors);
    }

    @Bean
    public CaptureTaskExecutor captureTaskExecutor(CameraAdapter cameraAdapter,
                                                   CameraConfigRepository cameraConfigRepository) {
        return new CaptureTaskExecutor(cameraAdapter, cameraConfigRepository);
    }

    @Bean
    public ProcessTaskExecutor processTaskExecutor(ModelAdapter modelAdapter,
                                                   ModelConfigRepository modelConfigRepository) {
        return new ProcessTaskExecutor(modelAdapter, modelConfigRepository);
    }

    @Bean
    public TransferTaskExecutor transferTaskExecutor(TransferAdapter transferAdapter,
                                                     TransferConfigRepository transferConfigRepository) {
        return new TransferTaskExecutor(transferAdapter, transferConfigRepository);
    }

    @Bean
    public GeoBoundaryTaskExecutor geoBoundaryTaskExecutor(AssetRepository assetRepository,
                                                           TaskAssetRepository taskAssetRepository) {
        return new GeoBoundaryTaskExecutor(assetRepository, taskAssetRepository);
    }

    @Bean
    public PreviewTaskExecutor previewTaskExecutor(AssetRepository assetRepository,
                                                   TaskAssetRepository taskAssetRepository) {
        return new PreviewTaskExecutor(assetRepository, taskAssetRepository);
    }

    @Bean
    public PublishLayerTaskExecutor publishLayerTaskExecutor(AssetRepository assetRepository,
                                                             TaskAssetRepository taskAssetRepository) {
        return new PublishLayerTaskExecutor(assetRepository, taskAssetRepository);
    }

    @Bean
    public DemoPlaybackTaskExecutor demoPlaybackTaskExecutor(AssetRepository assetRepository,
                                                             TaskAssetRepository taskAssetRepository) {
        return new DemoPlaybackTaskExecutor(assetRepository, taskAssetRepository);
    }

    @Bean
    public AgentAnalysisTaskExecutor agentAnalysisTaskExecutor(AssetRepository assetRepository,
                                                               TaskAssetRepository taskAssetRepository) {
        return new AgentAnalysisTaskExecutor(assetRepository, taskAssetRepository);
    }

    @Bean
    public ReportGenerationTaskExecutor reportGenerationTaskExecutor(AssetRepository assetRepository,
                                                                     TaskAssetRepository taskAssetRepository) {
        return new ReportGenerationTaskExecutor(assetRepository, taskAssetRepository);
    }

    @Bean
    public DeviceCommandService deviceCommandService(TaskCommandRepository taskCommandRepository,
                                                     TaskEventRepository taskEventRepository,
                                                     TaskApplicationService taskApplicationService,
                                                     DeviceCommandExecutor deviceCommandExecutor) {
        return new DeviceCommandService(taskCommandRepository, taskEventRepository,
                taskApplicationService, deviceCommandExecutor);
    }

    @Bean
    public DeviceConfigApplicationService deviceConfigApplicationService(DeviceConfigRepository deviceConfigRepository) {
        return new DeviceConfigApplicationService(deviceConfigRepository);
    }

    @Bean
    public CameraConfigApplicationService cameraConfigApplicationService(CameraConfigRepository cameraConfigRepository) {
        return new CameraConfigApplicationService(cameraConfigRepository);
    }

    @Bean
    public ModelConfigApplicationService modelConfigApplicationService(ModelConfigRepository modelConfigRepository,
                                                                      ModelArtifactRepository modelArtifactRepository) {
        return new ModelConfigApplicationService(modelConfigRepository, modelArtifactRepository);
    }

    @Bean
    public TransferConfigApplicationService transferConfigApplicationService(TransferConfigRepository transferConfigRepository) {
        return new TransferConfigApplicationService(transferConfigRepository);
    }

    @Bean
    public ConfigValidationService configValidationService(DeviceConfigRepository deviceConfigRepository,
                                                           CameraConfigRepository cameraConfigRepository,
                                                           ModelConfigRepository modelConfigRepository,
                                                           TransferConfigRepository transferConfigRepository,
                                                           ConfigValidationRepository configValidationRepository) {
        return new ConfigValidationService(deviceConfigRepository, cameraConfigRepository,
                modelConfigRepository, transferConfigRepository, configValidationRepository);
    }
}
