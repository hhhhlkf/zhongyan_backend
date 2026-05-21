package com.zhongyan.uav.task.application;

import com.zhongyan.uav.configcenter.infrastructure.mock.InMemoryCameraConfigRepository;
import com.zhongyan.uav.configcenter.infrastructure.mock.InMemoryModelConfigRepository;
import com.zhongyan.uav.configcenter.infrastructure.mock.InMemoryTransferConfigRepository;
import com.zhongyan.uav.asset.domain.Asset;
import com.zhongyan.uav.asset.domain.AssetRole;
import com.zhongyan.uav.asset.domain.AssetType;
import com.zhongyan.uav.asset.infrastructure.mock.InMemoryAssetRepository;
import com.zhongyan.uav.asset.infrastructure.mock.InMemoryAssetStorage;
import com.zhongyan.uav.asset.infrastructure.mock.InMemoryTaskAssetRepository;
import com.zhongyan.uav.device.infrastructure.mock.MockCameraAdapter;
import com.zhongyan.uav.device.infrastructure.mock.MockDeviceCommandExecutor;
import com.zhongyan.uav.device.infrastructure.mock.MockModelAdapter;
import com.zhongyan.uav.device.infrastructure.mock.MockTransferAdapter;
import com.zhongyan.uav.mission.application.CreateMissionInput;
import com.zhongyan.uav.mission.application.MissionApplicationService;
import com.zhongyan.uav.mission.domain.Mission;
import com.zhongyan.uav.mission.infrastructure.mock.InMemoryMissionRepository;
import com.zhongyan.uav.task.domain.Task;
import com.zhongyan.uav.task.domain.TaskAttemptResult;
import com.zhongyan.uav.task.domain.TaskCommand;
import com.zhongyan.uav.task.domain.TaskCommandStatus;
import com.zhongyan.uav.task.domain.TaskEventType;
import com.zhongyan.uav.task.domain.TaskStatus;
import com.zhongyan.uav.task.domain.TaskType;
import com.zhongyan.uav.task.executor.AgentAnalysisTaskExecutor;
import com.zhongyan.uav.task.executor.CaptureTaskExecutor;
import com.zhongyan.uav.task.executor.DemoPlaybackTaskExecutor;
import com.zhongyan.uav.task.executor.GeoBoundaryTaskExecutor;
import com.zhongyan.uav.task.executor.PreviewTaskExecutor;
import com.zhongyan.uav.task.executor.ProcessTaskExecutor;
import com.zhongyan.uav.task.executor.PublishLayerTaskExecutor;
import com.zhongyan.uav.task.executor.ReportGenerationTaskExecutor;
import com.zhongyan.uav.task.executor.TaskExecutionContext;
import com.zhongyan.uav.task.executor.TaskExecutionResult;
import com.zhongyan.uav.task.executor.TaskExecutor;
import com.zhongyan.uav.task.executor.TransferTaskExecutor;
import com.zhongyan.uav.task.infrastructure.mock.InMemoryTaskAttemptRepository;
import com.zhongyan.uav.task.infrastructure.mock.InMemoryTaskCommandRepository;
import com.zhongyan.uav.task.infrastructure.mock.InMemoryTaskEventRepository;
import com.zhongyan.uav.task.infrastructure.mock.InMemoryTaskRepository;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;

class TaskSchedulerServiceTests {
    private final Clock clock = Clock.fixed(Instant.parse("2026-04-29T00:00:00Z"), ZoneOffset.UTC);

    @Test
    void queuedCaptureTaskWaitsForApprovalThenExecutesWithAttemptLog() {
        Fixture fixture = new Fixture(clock);
        Mission mission = fixture.missionApplicationService.createMission(
                new CreateMissionInput("scheduler-capture", "test", null, 1, "tester", null));
        Task task = fixture.taskApplicationService.createTask(new CreateTaskInput(
                mission.missionId(), TaskType.CAPTURE, 10, "camera-rgb-1", null,
                Map.of("cameraType", "rgb", "durationSeconds", 30), List.of(), "tester"));
        fixture.taskApplicationService.submitTask(task.taskId(), "tester");

        TaskScheduleResult waiting = fixture.schedulerService.scheduleTask(task.taskId());

        assertThat(waiting.status()).isEqualTo(TaskScheduleStatus.WAITING_APPROVAL);
        TaskCommand command = fixture.commandRepository.findById(waiting.commandId()).orElseThrow();
        assertThat(command.status()).isEqualTo(TaskCommandStatus.PENDING_APPROVAL);
        assertThat(fixture.taskRepository.findById(task.taskId())).get()
                .extracting(Task::status)
                .isEqualTo(TaskStatus.QUEUED);

        fixture.taskApprovalService.approveCommand(command.commandId(), "admin");
        TaskScheduleResult executed = fixture.schedulerService.scheduleTask(task.taskId());

        assertThat(executed.status()).isEqualTo(TaskScheduleStatus.EXECUTED);
        assertThat(fixture.commandRepository.findById(command.commandId())).get()
                .extracting(TaskCommand::status)
                .isEqualTo(TaskCommandStatus.COMPLETED);
        assertThat(fixture.taskRepository.findById(task.taskId())).get()
                .extracting(Task::status)
                .isEqualTo(TaskStatus.COMPLETED);
        assertThat(fixture.attemptRepository.findByTaskId(task.taskId()))
                .singleElement()
                .satisfies(attempt -> {
                    assertThat(attempt.result()).isEqualTo(TaskAttemptResult.SUCCESS);
                    assertThat(attempt.rawLogObjectKey()).contains(command.commandId());
                });
        assertThat(fixture.eventRepository.findByTaskId(task.taskId()))
                .extracting(event -> event.eventType())
                .contains(TaskEventType.COMMAND_CREATED, TaskEventType.APPROVED,
                        TaskEventType.DISPATCHED, TaskEventType.STARTED, TaskEventType.COMPLETED);
    }

    @Test
    void schedulerSupportsFirstBatchProcessAndTransferExecutors() {
        Fixture fixture = new Fixture(clock);
        Mission mission = fixture.missionApplicationService.createMission(
                new CreateMissionInput("scheduler-batch", "test", null, 1, "tester", null));

        Task processTask = createSubmittedTask(fixture, mission, TaskType.PROCESS, null, "model-detect-1",
                Map.of("modelType", "detect", "runtimeType", "mock"));
        Task transferTask = createSubmittedTask(fixture, mission, TaskType.TRANSFER, "transfer-1", null,
                Map.of("transferType", "share"));

        executeAfterApproval(fixture, processTask);
        executeAfterApproval(fixture, transferTask);

        assertThat(fixture.taskRepository.findById(processTask.taskId())).get()
                .extracting(Task::status)
                .isEqualTo(TaskStatus.COMPLETED);
        assertThat(fixture.taskRepository.findById(transferTask.taskId())).get()
                .extracting(Task::status)
                .isEqualTo(TaskStatus.COMPLETED);
    }

    @Test
    void schedulerSupportsRemainingTaskExecutors() {
        Fixture fixture = new Fixture(clock);
        Mission mission = fixture.missionApplicationService.createMission(
                new CreateMissionInput("scheduler-remaining", "test", null, 1, "tester", null));
        Asset source = Asset.created("asset-source", mission.missionId(), null, AssetType.IMAGE,
                AssetRole.INPUT, "source.jpg", "source/source.jpg", "image/jpeg",
                100, "sha256:source", Map.of(), "tester", clock.instant()).markAvailable(clock.instant());
        fixture.assetRepository.save(source);

        for (TaskType taskType : List.of(TaskType.CALCULATE_GEO_BOUNDARY, TaskType.GENERATE_PREVIEW,
                TaskType.PUBLISH_LAYER, TaskType.DEMO_PLAYBACK, TaskType.AGENT_ANALYSIS,
                TaskType.REPORT_GENERATION)) {
            Task task = createSubmittedTask(fixture, mission, taskType, "device-" + taskType.name(),
                    "model-" + taskType.name(), Map.of("layerUrl", "/v2/layers/test"));
            task = new Task(task.taskId(), task.missionId(), task.taskType(), task.status(), task.priority(),
                    task.deviceId(), task.modelId(), task.configSnapshot(), List.of(source.assetId()),
                    task.outputAssetIds(), task.progress(), task.createdBy(), task.createdAt(),
                    task.updatedAt(), task.startedAt(), task.endedAt(), task.errorCode(), task.errorMessage());
            fixture.taskRepository.save(task);

            executeAfterApproval(fixture, task);

            assertThat(fixture.taskRepository.findById(task.taskId())).get()
                    .extracting(Task::status)
                    .isEqualTo(TaskStatus.COMPLETED);
            assertThat(fixture.assetRepository.findByTaskId(task.taskId())).isNotEmpty();
        }
    }

    @Test
    void failedTaskIsQueuedForRetryWhenRetryBudgetRemains() {
        Fixture fixture = new Fixture(clock, List.of(new FailingExecutor(TaskType.DEMO_PLAYBACK)));
        Mission mission = fixture.missionApplicationService.createMission(
                new CreateMissionInput("scheduler-retry", "test", null, 1, "tester", null));
        Task task = createSubmittedTask(fixture, mission, TaskType.DEMO_PLAYBACK, "demo", null,
                Map.of("maxRetries", 1));

        TaskScheduleResult waiting = fixture.schedulerService.scheduleTask(task.taskId());
        approveIfNeeded(fixture, waiting);
        TaskScheduleResult result = fixture.schedulerService.scheduleTask(task.taskId());

        assertThat(result.status()).isEqualTo(TaskScheduleStatus.RETRY_QUEUED);
        assertThat(fixture.taskRepository.findById(task.taskId())).get()
                .extracting(Task::status)
                .isEqualTo(TaskStatus.QUEUED);
        assertThat(fixture.attemptRepository.findByTaskId(task.taskId()))
                .singleElement()
                .extracting(attempt -> attempt.result())
                .isEqualTo(TaskAttemptResult.FAILED);
    }

    @Test
    void timedOutTaskIsMarkedTimeoutWhenRetryBudgetIsExhausted() {
        Fixture fixture = new Fixture(clock, List.of(new SlowExecutor(TaskType.REPORT_GENERATION)));
        Mission mission = fixture.missionApplicationService.createMission(
                new CreateMissionInput("scheduler-timeout", "test", null, 1, "tester", null));
        Task task = createSubmittedTask(fixture, mission, TaskType.REPORT_GENERATION, "report", null,
                Map.of("timeoutMs", 1));

        TaskScheduleResult waiting = fixture.schedulerService.scheduleTask(task.taskId());
        approveIfNeeded(fixture, waiting);
        TaskScheduleResult result = fixture.schedulerService.scheduleTask(task.taskId());

        assertThat(result.status()).isEqualTo(TaskScheduleStatus.TIMEOUT);
        assertThat(fixture.taskRepository.findById(task.taskId())).get()
                .extracting(Task::status)
                .isEqualTo(TaskStatus.TIMEOUT);
        assertThat(fixture.attemptRepository.findByTaskId(task.taskId()))
                .singleElement()
                .extracting(attempt -> attempt.result())
                .isEqualTo(TaskAttemptResult.TIMEOUT);
    }

    @Test
    void recoveryScanTimesOutStaleRunningAttempts() {
        Fixture fixture = new Fixture(clock);
        Mission mission = fixture.missionApplicationService.createMission(
                new CreateMissionInput("scheduler-recovery", "test", null, 1, "tester", null));
        Task task = createSubmittedTask(fixture, mission, TaskType.CAPTURE, "camera", null, Map.of());
        fixture.taskApplicationService.startTaskAttempt(task.taskId(), "node-a");

        List<TaskScheduleResult> results = fixture.schedulerService.recoverStaleTasks(Duration.ZERO, 10);

        assertThat(results).extracting(TaskScheduleResult::status).contains(TaskScheduleStatus.RECOVERED);
        assertThat(fixture.taskRepository.findById(task.taskId())).get()
                .extracting(Task::status)
                .isEqualTo(TaskStatus.TIMEOUT);
    }

    private Task createSubmittedTask(Fixture fixture, Mission mission, TaskType taskType,
                                     String deviceId, String modelId, Map<String, Object> snapshot) {
        Task task = fixture.taskApplicationService.createTask(new CreateTaskInput(
                mission.missionId(), taskType, 1, deviceId, modelId, snapshot, List.of(), "tester"));
        return fixture.taskApplicationService.submitTask(task.taskId(), "tester");
    }

    private void executeAfterApproval(Fixture fixture, Task task) {
        TaskScheduleResult waiting = fixture.schedulerService.scheduleTask(task.taskId());
        approveIfNeeded(fixture, waiting);
        assertThat(fixture.schedulerService.scheduleTask(task.taskId()).status())
                .isEqualTo(TaskScheduleStatus.EXECUTED);
    }

    private void approveIfNeeded(Fixture fixture, TaskScheduleResult waiting) {
        if (waiting.status() == TaskScheduleStatus.WAITING_APPROVAL) {
            fixture.taskApprovalService.approveCommand(waiting.commandId(), "admin");
        }
    }

    private static class Fixture {
        private final InMemoryTaskRepository taskRepository = new InMemoryTaskRepository();
        private final InMemoryTaskCommandRepository commandRepository = new InMemoryTaskCommandRepository();
        private final InMemoryTaskEventRepository eventRepository = new InMemoryTaskEventRepository();
        private final InMemoryTaskAttemptRepository attemptRepository = new InMemoryTaskAttemptRepository();
        private final InMemoryAssetRepository assetRepository = new InMemoryAssetRepository();
        private final InMemoryTaskAssetRepository taskAssetRepository = new InMemoryTaskAssetRepository();
        private final MissionApplicationService missionApplicationService;
        private final TaskApplicationService taskApplicationService;
        private final TaskApprovalService taskApprovalService;
        private final TaskSchedulerService schedulerService;

        private Fixture(Clock clock) {
            this(clock, List.of());
        }

        private Fixture(Clock clock, List<TaskExecutor> overrideExecutors) {
            InMemoryMissionRepository missionRepository = new InMemoryMissionRepository();
            missionApplicationService = new MissionApplicationService(missionRepository, clock);
            taskApplicationService = new TaskApplicationService(taskRepository, missionRepository,
                    eventRepository, attemptRepository, clock);
            TaskCommandApplicationService commandApplicationService = new TaskCommandApplicationService(
                    taskRepository, commandRepository, eventRepository, clock);
            taskApprovalService = new TaskApprovalService(commandRepository, eventRepository, clock);
            MockDeviceCommandExecutor commandExecutor = new MockDeviceCommandExecutor(clock);
            List<TaskExecutor> executors = overrideExecutors.isEmpty() ? List.of(
                    new CaptureTaskExecutor(new MockCameraAdapter(commandExecutor, clock),
                            new InMemoryCameraConfigRepository()),
                    new ProcessTaskExecutor(new MockModelAdapter(commandExecutor, clock),
                            new InMemoryModelConfigRepository()),
                    new TransferTaskExecutor(new MockTransferAdapter(commandExecutor, clock),
                            new InMemoryTransferConfigRepository()),
                    new GeoBoundaryTaskExecutor(assetRepository, taskAssetRepository, clock),
                    new PreviewTaskExecutor(assetRepository, taskAssetRepository, clock),
                    new PublishLayerTaskExecutor(assetRepository, taskAssetRepository, clock),
                    new DemoPlaybackTaskExecutor(assetRepository, taskAssetRepository, clock),
                    new AgentAnalysisTaskExecutor(assetRepository, taskAssetRepository, clock),
                    new ReportGenerationTaskExecutor(assetRepository, taskAssetRepository,
                            new InMemoryAssetStorage(), clock))
                    : overrideExecutors;
            schedulerService = new TaskSchedulerService(taskRepository, attemptRepository,
                    commandRepository, eventRepository, commandApplicationService, taskApplicationService,
                    executors, clock, Executors.newSingleThreadExecutor());
        }
    }

    private static class FailingExecutor implements TaskExecutor {
        private final TaskType taskType;

        private FailingExecutor(TaskType taskType) {
            this.taskType = taskType;
        }

        @Override
        public boolean supports(TaskType taskType) {
            return this.taskType == taskType;
        }

        @Override
        public TaskExecutionResult execute(TaskExecutionContext context) {
            return new TaskExecutionResult(false, "MOCK_FAILED", "mock failed",
                    "logs/mock-failed.log", Map.of(), Instant.parse("2026-04-29T00:00:00Z"));
        }
    }

    private static class SlowExecutor implements TaskExecutor {
        private final TaskType taskType;

        private SlowExecutor(TaskType taskType) {
            this.taskType = taskType;
        }

        @Override
        public boolean supports(TaskType taskType) {
            return this.taskType == taskType;
        }

        @Override
        public TaskExecutionResult execute(TaskExecutionContext context) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
            return new TaskExecutionResult(true, "0", "slow completed",
                    "logs/slow.log", Map.of(), Instant.parse("2026-04-29T00:00:00Z"));
        }
    }
}
