package com.zhongyan.uav.device.application;

import com.zhongyan.uav.device.infrastructure.mock.MockDeviceCommandExecutor;
import com.zhongyan.uav.mission.application.CreateMissionInput;
import com.zhongyan.uav.mission.application.MissionApplicationService;
import com.zhongyan.uav.mission.domain.Mission;
import com.zhongyan.uav.mission.infrastructure.mock.InMemoryMissionRepository;
import com.zhongyan.uav.task.application.CreateTaskCommandInput;
import com.zhongyan.uav.task.application.CreateTaskInput;
import com.zhongyan.uav.task.application.TaskApplicationService;
import com.zhongyan.uav.task.application.TaskApprovalService;
import com.zhongyan.uav.task.application.TaskCommandApplicationService;
import com.zhongyan.uav.task.domain.Task;
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

class DeviceCommandServiceTests {
    private final Clock clock = Clock.fixed(Instant.parse("2026-04-29T00:00:00Z"), ZoneOffset.UTC);

    @Test
    void dispatchesApprovedCommandAndStoresAttemptLogReference() {
        InMemoryMissionRepository missionRepository = new InMemoryMissionRepository();
        InMemoryTaskRepository taskRepository = new InMemoryTaskRepository();
        InMemoryTaskCommandRepository commandRepository = new InMemoryTaskCommandRepository();
        InMemoryTaskEventRepository eventRepository = new InMemoryTaskEventRepository();
        InMemoryTaskAttemptRepository attemptRepository = new InMemoryTaskAttemptRepository();

        Mission mission = new MissionApplicationService(missionRepository, clock)
                .createMission(new CreateMissionInput("device-flow", "test", null, 1, "tester", null));
        TaskApplicationService taskApplicationService = new TaskApplicationService(
                taskRepository, missionRepository, eventRepository, attemptRepository, clock);
        Task task = taskApplicationService.createTask(new CreateTaskInput(
                mission.missionId(), TaskType.CAPTURE, 1, "device-rgb-1", null,
                Map.of("cameraType", "RGB"), List.of(), "tester"));
        taskApplicationService.submitTask(task.taskId(), "tester");

        TaskCommand command = new TaskCommandApplicationService(taskRepository, commandRepository, eventRepository, clock)
                .createCommand(task.taskId(), new CreateTaskCommandInput(
                        TaskCommandType.START_CAPTURE, Map.of("durationSeconds", 30),
                        "idem-device-command-1", "tester", null, "device-rgb-1", "start capture"));
        new TaskApprovalService(commandRepository, eventRepository, clock)
                .approveCommand(command.commandId(), "admin");

        DeviceCommandService service = new DeviceCommandService(commandRepository, eventRepository,
                taskApplicationService, new MockDeviceCommandExecutor(clock), clock);
        service.dispatch(command.commandId());

        assertThat(commandRepository.findById(command.commandId())).get()
                .extracting(TaskCommand::status)
                .isEqualTo(TaskCommandStatus.COMPLETED);
        assertThat(taskRepository.findById(task.taskId())).get()
                .extracting(Task::status)
                .isEqualTo(TaskStatus.COMPLETED);
        assertThat(attemptRepository.findByTaskId(task.taskId()))
                .singleElement()
                .satisfies(attempt -> {
                    assertThat(attempt.result()).isEqualTo(TaskAttemptResult.SUCCESS);
                    assertThat(attempt.rawLogObjectKey()).contains(command.commandId());
                });
        assertThat(eventRepository.findByTaskId(task.taskId()))
                .extracting(event -> event.eventType())
                .contains(TaskEventType.DISPATCHED, TaskEventType.COMPLETED);
    }
}
