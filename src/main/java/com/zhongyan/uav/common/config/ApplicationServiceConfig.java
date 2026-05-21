package com.zhongyan.uav.common.config;

import com.zhongyan.uav.agent.application.AgentApplicationService;
import com.zhongyan.uav.agent.application.RagService;
import com.zhongyan.uav.agent.application.ReportDraftService;
import com.zhongyan.uav.agent.application.ToolGatewayService;
import com.zhongyan.uav.agent.domain.AgentMessageRepository;
import com.zhongyan.uav.agent.domain.AgentSessionRepository;
import com.zhongyan.uav.agent.domain.AgentToolCallRepository;
import com.zhongyan.uav.agent.infrastructure.KafkaAgentEventPublisher;
import com.zhongyan.uav.agent.infrastructure.PgVectorStoreAdapter;
import com.zhongyan.uav.agent.infrastructure.SpringAiChatModelAdapter;
import com.zhongyan.uav.agent.infrastructure.SpringAiEmbeddingAdapter;
import com.zhongyan.uav.asset.domain.AssetRepository;
import com.zhongyan.uav.asset.domain.TaskAssetRepository;
import com.zhongyan.uav.agent.infrastructure.AgentProperties;
import com.zhongyan.uav.agent.infrastructure.tool.AssetSearchTool;
import com.zhongyan.uav.agent.infrastructure.tool.AssetStatsTool;
import com.zhongyan.uav.agent.infrastructure.tool.DiagnosisReadTaskLogTool;
import com.zhongyan.uav.agent.infrastructure.tool.MissionQueryTool;
import com.zhongyan.uav.agent.infrastructure.tool.ReportGenerateDraftTool;
import com.zhongyan.uav.agent.infrastructure.tool.TaskCommandCreateTool;
import com.zhongyan.uav.agent.infrastructure.tool.TaskCreateTool;
import com.zhongyan.uav.agent.infrastructure.tool.TaskQueryTool;
import com.zhongyan.uav.agent.infrastructure.tool.TelemetryQueryTool;
import com.zhongyan.uav.agent.port.AgentEventPublisher;
import com.zhongyan.uav.agent.port.AgentRuntimeGuard;
import com.zhongyan.uav.agent.port.AgentTool;
import com.zhongyan.uav.agent.port.ChatModelPort;
import com.zhongyan.uav.agent.port.EmbeddingPort;
import com.zhongyan.uav.agent.port.VectorStorePort;
import com.zhongyan.uav.agent.infrastructure.NoopAgentRuntimeGuard;
import com.zhongyan.uav.agent.infrastructure.RedisAgentRuntimeGuard;
import com.zhongyan.uav.asset.application.AssetApplicationService;
import com.zhongyan.uav.asset.application.AssetEventRecorder;
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
import com.zhongyan.uav.realtime.infrastructure.CompositeRealtimePushService;
import com.zhongyan.uav.realtime.infrastructure.SseRealtimePushService;
import com.zhongyan.uav.realtime.infrastructure.WebSocketRealtimePushService;
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
import com.zhongyan.uav.telemetry.application.UavTelemetryIngestService;
import com.zhongyan.uav.telemetry.application.UavTelemetryQueryService;
import com.zhongyan.uav.telemetry.domain.UavTelemetryRepository;
import com.zhongyan.uav.telemetry.infrastructure.TelemetryProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.ai.document.MetadataMode;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.ai.openai.OpenAiEmbeddingOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Clock;
import java.util.List;

@Configuration
@EnableConfigurationProperties({TelemetryProperties.class, AgentProperties.class})
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
                                                           TaskAssetRepository taskAssetRepository,
                                                           AssetEventRecorder assetEventRecorder) {
        return new AssetApplicationService(assetRepository, taskAssetRepository, assetEventRecorder);
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
                                                                       GroundElevationPort groundElevationPort,
                                                                       AssetEventRecorder assetEventRecorder) {
        return new GeoBoundaryApplicationService(assetRepository, groundElevationPort, assetEventRecorder);
    }

    @Bean
    public LayerPublishApplicationService layerPublishApplicationService(AssetRepository assetRepository,
                                                                         GeoServerPort geoServerPort,
                                                                         AssetEventRecorder assetEventRecorder) {
        return new LayerPublishApplicationService(assetRepository, geoServerPort, assetEventRecorder);
    }

    @Bean
    public RealtimePushService realtimePushService(WebSocketRealtimePushService webSocketRealtimePushService) {
        return new CompositeRealtimePushService(new SseRealtimePushService(), webSocketRealtimePushService);
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
    public AssetEventRecorder assetEventRecorder(OutboxPublishService outboxPublishService,
                                                 TaskEventRepository taskEventRepository) {
        return new AssetEventRecorder(outboxPublishService, taskEventRepository);
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
                                                               TaskAssetRepository taskAssetRepository,
                                                               AgentApplicationService agentApplicationService) {
        return new AgentAnalysisTaskExecutor(assetRepository, taskAssetRepository,
                agentApplicationService, Clock.systemUTC());
    }

    @Bean
    public ReportGenerationTaskExecutor reportGenerationTaskExecutor(AssetRepository assetRepository,
                                                                     TaskAssetRepository taskAssetRepository,
                                                                     AssetStoragePort assetStoragePort) {
        return new ReportGenerationTaskExecutor(assetRepository, taskAssetRepository, assetStoragePort);
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

    @Bean
    public UavTelemetryIngestService uavTelemetryIngestService(UavTelemetryRepository telemetryRepository,
                                                               OutboxPublishService outboxPublishService) {
        return new UavTelemetryIngestService(telemetryRepository, outboxPublishService);
    }

    @Bean
    public UavTelemetryQueryService uavTelemetryQueryService(UavTelemetryRepository telemetryRepository) {
        return new UavTelemetryQueryService(telemetryRepository);
    }

    @Bean
    public AgentApplicationService agentApplicationService(AgentSessionRepository agentSessionRepository,
                                                           AgentMessageRepository agentMessageRepository,
                                                           ChatModelPort chatModelPort,
                                                           ToolGatewayService toolGatewayService,
                                                           RagService ragService,
                                                           ReportDraftService reportDraftService,
                                                           AgentEventPublisher agentEventPublisher,
                                                           MissionRepository missionRepository,
                                                           TaskRepository taskRepository,
                                                           TaskEventRepository taskEventRepository,
                                                           AssetRepository assetRepository,
                                                           UavTelemetryRepository uavTelemetryRepository) {
        return new AgentApplicationService(agentSessionRepository, agentMessageRepository,
                chatModelPort, toolGatewayService, ragService, reportDraftService,
                agentEventPublisher, Clock.systemUTC(), missionRepository, taskRepository,
                taskEventRepository, assetRepository, uavTelemetryRepository);
    }

    @Bean
    public ToolGatewayService toolGatewayService(List<AgentTool> agentTools,
                                                 AgentToolCallRepository agentToolCallRepository,
                                                 AgentEventPublisher agentEventPublisher,
                                                 AgentRuntimeGuard agentRuntimeGuard) {
        return new ToolGatewayService(agentTools, agentToolCallRepository,
                agentEventPublisher, agentRuntimeGuard, Clock.systemUTC());
    }

    @Bean
    public RagService ragService(EmbeddingPort embeddingPort, VectorStorePort vectorStorePort) {
        return new RagService(embeddingPort, vectorStorePort);
    }

    @Bean
    public ReportDraftService reportDraftService() {
        return new ReportDraftService();
    }

    @Bean
    @ConditionalOnMissingBean(ChatModelPort.class)
    public ChatModelPort chatModelPort(AgentProperties agentProperties,
                                       ObjectMapper objectMapper,
                                       ObjectProvider<org.springframework.ai.chat.model.ChatModel> chatModelProvider,
                                       List<AgentTool> agentTools) {
        return new SpringAiChatModelAdapter(agentProperties, objectMapper,
                chatModelProvider.getIfAvailable(), agentTools);
    }

    @Bean
    @ConditionalOnMissingBean(EmbeddingPort.class)
    public EmbeddingPort embeddingPort(AgentProperties agentProperties,
                                       ObjectProvider<EmbeddingModel> embeddingModelProvider) {
        return new SpringAiEmbeddingAdapter(agentProperties, embeddingModelProvider.getIfAvailable());
    }

    @Bean
    @ConditionalOnProperty(prefix = "bms.agent", name = "enabled", havingValue = "true")
    @ConditionalOnMissingBean(OpenAIClient.class)
    public OpenAIClient agentOpenAiClient(AgentProperties agentProperties) {
        return OpenAIOkHttpClient.builder()
                .baseUrl(agentProperties.normalizedBaseUrl())
                .apiKey(defaultText(agentProperties.apiKey(), "unused"))
                .timeout(agentProperties.readTimeout())
                .maxRetries(0)
                .build();
    }

    @Bean
    @ConditionalOnProperty(prefix = "bms.agent", name = "enabled", havingValue = "true")
    @ConditionalOnMissingBean(org.springframework.ai.chat.model.ChatModel.class)
    public org.springframework.ai.chat.model.ChatModel springAiChatModel(OpenAIClient openAIClient,
                                                                        AgentProperties agentProperties) {
        OpenAiChatOptions options = OpenAiChatOptions.builder()
                .model(agentProperties.chatModel())
                .timeout(agentProperties.readTimeout())
                .build();
        return OpenAiChatModel.builder()
                .openAiClient(openAIClient)
                .options(options)
                .build();
    }

    @Bean
    @ConditionalOnProperty(prefix = "bms.agent", name = "enabled", havingValue = "true")
    @ConditionalOnMissingBean(EmbeddingModel.class)
    public EmbeddingModel springAiEmbeddingModel(OpenAIClient openAIClient,
                                                 AgentProperties agentProperties) {
        OpenAiEmbeddingOptions options = OpenAiEmbeddingOptions.builder()
                .model(agentProperties.embeddingModel())
                .timeout(agentProperties.readTimeout())
                .build();
        return new OpenAiEmbeddingModel(openAIClient, MetadataMode.EMBED, options);
    }

    @Bean
    @ConditionalOnMissingBean(VectorStorePort.class)
    public VectorStorePort vectorStorePort(ObjectProvider<JdbcTemplate> jdbcTemplateProvider,
                                           ObjectMapper objectMapper) {
        return new PgVectorStoreAdapter(jdbcTemplateProvider.getIfAvailable(), objectMapper);
    }

    @Bean
    @ConditionalOnMissingBean(AgentRuntimeGuard.class)
    public AgentRuntimeGuard agentRuntimeGuard(ObjectProvider<StringRedisTemplate> redisTemplateProvider,
                                               AgentProperties agentProperties) {
        StringRedisTemplate redisTemplate = redisTemplateProvider.getIfAvailable();
        if (redisTemplate != null && agentProperties.redis().enabled()) {
            return new RedisAgentRuntimeGuard(redisTemplate, agentProperties);
        }
        return new NoopAgentRuntimeGuard();
    }

    @Bean
    @ConditionalOnMissingBean(AgentEventPublisher.class)
    public AgentEventPublisher agentEventPublisher(EventPublisher eventPublisher,
                                                   RealtimePushService realtimePushService) {
        return new KafkaAgentEventPublisher(eventPublisher, realtimePushService);
    }

    @Bean
    public AgentTool missionQueryTool(MissionRepository missionRepository) {
        return new MissionQueryTool(missionRepository);
    }

    @Bean
    public AgentTool taskQueryTool(TaskRepository taskRepository,
                                   TaskEventRepository taskEventRepository,
                                   TaskAttemptRepository taskAttemptRepository,
                                   TaskCommandRepository taskCommandRepository) {
        return new TaskQueryTool(taskRepository, taskEventRepository,
                taskAttemptRepository, taskCommandRepository);
    }

    @Bean
    public AgentTool taskCreateTool(TaskApplicationService taskApplicationService) {
        return new TaskCreateTool(taskApplicationService);
    }

    @Bean
    public AgentTool taskCommandCreateTool(TaskCommandApplicationService taskCommandApplicationService) {
        return new TaskCommandCreateTool(taskCommandApplicationService);
    }

    @Bean
    public AgentTool assetSearchTool(AssetRepository assetRepository) {
        return new AssetSearchTool(assetRepository);
    }

    @Bean
    public AgentTool assetStatsTool(AssetRepository assetRepository) {
        return new AssetStatsTool(assetRepository);
    }

    @Bean
    public AgentTool telemetryQueryTool(UavTelemetryQueryService uavTelemetryQueryService) {
        return new TelemetryQueryTool(uavTelemetryQueryService);
    }

    @Bean
    public AgentTool diagnosisReadTaskLogTool(TaskAttemptRepository taskAttemptRepository,
                                              TaskEventRepository taskEventRepository) {
        return new DiagnosisReadTaskLogTool(taskAttemptRepository, taskEventRepository);
    }

    @Bean
    public AgentTool reportGenerateDraftTool(ReportDraftService reportDraftService,
                                             RagService ragService,
                                             TaskApplicationService taskApplicationService) {
        return new ReportGenerateDraftTool(reportDraftService, ragService, taskApplicationService);
    }

    private static String defaultText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
