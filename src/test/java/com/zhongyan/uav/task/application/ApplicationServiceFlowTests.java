package com.zhongyan.uav.task.application;

import com.zhongyan.uav.asset.domain.Asset;
import com.zhongyan.uav.asset.domain.AssetRole;
import com.zhongyan.uav.asset.domain.AssetType;
import com.zhongyan.uav.asset.infrastructure.mock.InMemoryAssetRepository;
import com.zhongyan.uav.mission.application.CreateMissionInput;
import com.zhongyan.uav.mission.application.MissionApplicationService;
import com.zhongyan.uav.mission.application.MissionQueryService;
import com.zhongyan.uav.mission.domain.Mission;
import com.zhongyan.uav.mission.domain.MissionStatus;
import com.zhongyan.uav.mission.infrastructure.mock.InMemoryMissionRepository;
import com.zhongyan.uav.task.domain.Task;
import com.zhongyan.uav.task.domain.TaskAttempt;
import com.zhongyan.uav.task.domain.TaskAttemptResult;
import com.zhongyan.uav.task.domain.TaskCommand;
import com.zhongyan.uav.task.domain.TaskCommandStatus;
import com.zhongyan.uav.task.domain.TaskCommandType;
import com.zhongyan.uav.task.domain.TaskEventType;
import com.zhongyan.uav.task.domain.TaskStatus;
import com.zhongyan.uav.task.domain.TaskType;
import com.zhongyan.uav.task.infrastructure.mock.InMemoryTaskAttemptRepository;
import com.zhongyan.uav.task.infrastructure.mock.InMemoryTaskCommandRepository;
import com.zhongyan.uav.task.infrastructure.mock.InMemoryTaskEventRepository;
import com.zhongyan.uav.task.infrastructure.mock.InMemoryTaskRepository;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ApplicationServiceFlowTests {
    private final Clock clock = Clock.fixed(Instant.parse("2026-04-29T00:00:00Z"), ZoneOffset.UTC);

    /**
     * 验证应用服务和内存仓储可以跑通 Mission -> Task -> Command/Event -> Asset 主链路。
     */
    @Test
    void runsMissionTaskCommandEventAssetFlowWithInMemoryRepositories() {
        InMemoryMissionRepository missionRepository = new InMemoryMissionRepository();
        InMemoryTaskRepository taskRepository = new InMemoryTaskRepository();
        InMemoryTaskCommandRepository taskCommandRepository = new InMemoryTaskCommandRepository();
        InMemoryTaskEventRepository taskEventRepository = new InMemoryTaskEventRepository();
        InMemoryTaskAttemptRepository taskAttemptRepository = new InMemoryTaskAttemptRepository();
        InMemoryAssetRepository assetRepository = new InMemoryAssetRepository();

        MissionApplicationService missionApplicationService = new MissionApplicationService(missionRepository, clock);
        MissionQueryService missionQueryService = new MissionQueryService(missionRepository);
        TaskApplicationService taskApplicationService = new TaskApplicationService(
                taskRepository, missionRepository, taskEventRepository, taskAttemptRepository, clock);
        TaskCommandApplicationService taskCommandApplicationService = new TaskCommandApplicationService(
                taskRepository, taskCommandRepository, taskEventRepository, clock);
        TaskApprovalService taskApprovalService = new TaskApprovalService(
                taskCommandRepository, taskEventRepository, clock);
        TaskQueryService taskQueryService = new TaskQueryService(
                taskRepository, taskCommandRepository, taskEventRepository, taskAttemptRepository, assetRepository);

        Mission mission = missionApplicationService.createMission(new CreateMissionInput(
                "洞庭湖应急巡检",
                "emergency",
                null,
                10,
                "operator-1",
                "mock 主链路验证"));
        Task task = taskApplicationService.createTask(new CreateTaskInput(
                mission.missionId(),
                TaskType.CAPTURE,
                10,
                "device-rgb-1",
                null,
                Map.of("cameraType", "rgb"),
                List.of(),
                "operator-1"));
        Task submittedTask = taskApplicationService.submitTask(task.taskId(), "operator-1");

        TaskCommand command = taskCommandApplicationService.createCommand(task.taskId(), new CreateTaskCommandInput(
                TaskCommandType.START_CAPTURE,
                Map.of("durationSeconds", 60),
                "idem-start-capture-1",
                "operator-1",
                null,
                "device-rgb-1",
                "开始采集"));
        TaskCommand approvedCommand = taskApprovalService.approveCommand(command.commandId(), "admin-1");

        Asset asset = Asset.created("asset-1", mission.missionId(), task.taskId(), AssetType.IMAGE,
                AssetRole.OUTPUT, "rgb-001.png", "missions/m1/rgb-001.png", "image/png",
                1024, "sha256:mock", Map.of("source", "mock"), "operator-1", clock.instant())
                .markAvailable(clock.instant());
        assetRepository.save(asset);

        assertThat(mission.status()).isEqualTo(MissionStatus.DRAFT);
        assertThat(submittedTask.status()).isEqualTo(TaskStatus.QUEUED);
        assertThat(command.status()).isEqualTo(TaskCommandStatus.PENDING_APPROVAL);
        assertThat(approvedCommand.status()).isEqualTo(TaskCommandStatus.PENDING_DISPATCH);

        assertThat(missionQueryService.getMission(mission.missionId())).isEqualTo(mission);
        assertThat(taskQueryService.getTask(task.taskId()).status()).isEqualTo(TaskStatus.QUEUED);
        assertThat(taskQueryService.listTasksByMission(mission.missionId())).hasSize(1);
        assertThat(taskQueryService.listCommands(task.taskId())).extracting(TaskCommand::commandId)
                .containsExactly(command.commandId());
        assertThat(taskQueryService.listAssets(task.taskId())).containsExactly(asset);
        assertThat(taskQueryService.listEvents(task.taskId())).extracting(event -> event.eventType())
                .contains(TaskEventType.CREATED, TaskEventType.SUBMITTED,
                        TaskEventType.COMMAND_CREATED, TaskEventType.APPROVED);
    }

    @Test
    void runsTaskFailureRetryCancelFlowWithAttempts() {
        InMemoryMissionRepository missionRepository = new InMemoryMissionRepository();
        InMemoryTaskRepository taskRepository = new InMemoryTaskRepository();
        InMemoryTaskCommandRepository taskCommandRepository = new InMemoryTaskCommandRepository();
        InMemoryTaskEventRepository taskEventRepository = new InMemoryTaskEventRepository();
        InMemoryTaskAttemptRepository taskAttemptRepository = new InMemoryTaskAttemptRepository();
        InMemoryAssetRepository assetRepository = new InMemoryAssetRepository();

        Mission mission = new MissionApplicationService(missionRepository, clock)
                .createMission(new CreateMissionInput("phase1-flow", "test", null, 1, "tester", null));
        TaskApplicationService taskApplicationService = new TaskApplicationService(
                taskRepository, missionRepository, taskEventRepository, taskAttemptRepository, clock);
        TaskQueryService taskQueryService = new TaskQueryService(
                taskRepository, taskCommandRepository, taskEventRepository, taskAttemptRepository, assetRepository);

        Task task = taskApplicationService.createTask(new CreateTaskInput(
                mission.missionId(), TaskType.PROCESS, 1, null, "model-1",
                Map.of("modelType", "detect"), List.of("asset-1"), "tester"));
        Task submittedTask = taskApplicationService.submitTask(task.taskId(), "tester");
        TaskAttempt firstAttempt = taskApplicationService.startTaskAttempt(submittedTask.taskId(), "mock-node-a");
        Task failedTask = taskApplicationService.failTask(submittedTask.taskId(),
                "MODEL_FAILED", "mock model failed", "logs/attempt-1.log");
        Task retriedTask = taskApplicationService.retryTask(failedTask.taskId(), "tester");
        TaskAttempt secondAttempt = taskApplicationService.startTaskAttempt(retriedTask.taskId(), "mock-node-b");
        Task cancelledTask = taskApplicationService.cancelTask(retriedTask.taskId(), "tester");

        assertThat(firstAttempt.attemptNo()).isEqualTo(1);
        assertThat(secondAttempt.attemptNo()).isEqualTo(2);
        assertThat(failedTask.status()).isEqualTo(TaskStatus.FAILED);
        assertThat(retriedTask.status()).isEqualTo(TaskStatus.QUEUED);
        assertThat(cancelledTask.status()).isEqualTo(TaskStatus.CANCELLED);
        assertThat(taskQueryService.listAttempts(task.taskId()))
                .extracting(TaskAttempt::result)
                .containsExactly(TaskAttemptResult.FAILED, TaskAttemptResult.CANCELLED);
        assertThat(taskQueryService.listEvents(task.taskId()))
                .extracting(event -> event.eventType())
                .contains(TaskEventType.CREATED, TaskEventType.SUBMITTED, TaskEventType.STARTED,
                        TaskEventType.FAILED, TaskEventType.RETRYING, TaskEventType.CANCELLED);
    }

    /**
     * 验证相同幂等键不会重复创建 TaskCommand。
     */
    @Test
    void createCommandReturnsExistingCommandWhenIdempotencyKeyExists() {
        InMemoryMissionRepository missionRepository = new InMemoryMissionRepository();
        InMemoryTaskRepository taskRepository = new InMemoryTaskRepository();
        InMemoryTaskCommandRepository taskCommandRepository = new InMemoryTaskCommandRepository();
        InMemoryTaskEventRepository taskEventRepository = new InMemoryTaskEventRepository();
        InMemoryTaskAttemptRepository taskAttemptRepository = new InMemoryTaskAttemptRepository();

        Mission mission = new MissionApplicationService(missionRepository, clock)
                .createMission(new CreateMissionInput("测试任务", "test", null, 1, "tester", null));
        Task task = new TaskApplicationService(taskRepository, missionRepository, taskEventRepository,
                taskAttemptRepository, clock)
                .createTask(new CreateTaskInput(mission.missionId(), TaskType.PROCESS, 1,
                        null, "model-1", Map.of(), List.of(), "tester"));
        TaskCommandApplicationService taskCommandApplicationService = new TaskCommandApplicationService(
                taskRepository, taskCommandRepository, taskEventRepository, clock);

        CreateTaskCommandInput input = new CreateTaskCommandInput(TaskCommandType.RETRY_TASK,
                Map.of(), "idem-retry-1", "tester", null, null, "retry");
        TaskCommand firstCommand = taskCommandApplicationService.createCommand(task.taskId(), input);
        TaskCommand secondCommand = taskCommandApplicationService.createCommand(task.taskId(), input);

        assertThat(secondCommand.commandId()).isEqualTo(firstCommand.commandId());
        assertThat(taskCommandRepository.findByTaskId(task.taskId())).hasSize(1);
    }
}
