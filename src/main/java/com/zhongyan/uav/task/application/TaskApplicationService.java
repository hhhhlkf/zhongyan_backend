package com.zhongyan.uav.task.application;

import com.zhongyan.uav.mission.domain.MissionRepository;
import com.zhongyan.uav.task.domain.Task;
import com.zhongyan.uav.task.domain.TaskAttempt;
import com.zhongyan.uav.task.domain.TaskAttemptRepository;
import com.zhongyan.uav.task.domain.TaskAttemptResult;
import com.zhongyan.uav.task.domain.TaskEvent;
import com.zhongyan.uav.task.domain.TaskEventRepository;
import com.zhongyan.uav.task.domain.TaskEventType;
import com.zhongyan.uav.task.domain.TaskRepository;
import com.zhongyan.uav.task.domain.TaskStatus;

import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public class TaskApplicationService {
    private final TaskRepository taskRepository;
    private final MissionRepository missionRepository;
    private final TaskEventRepository taskEventRepository;
    private final TaskAttemptRepository taskAttemptRepository;
    private final Clock clock;

    /**
     * 使用系统时钟创建 Task 应用服务。
     */
    public TaskApplicationService(TaskRepository taskRepository, MissionRepository missionRepository,
                                  TaskEventRepository taskEventRepository,
                                  TaskAttemptRepository taskAttemptRepository) {
        this(taskRepository, missionRepository, taskEventRepository, taskAttemptRepository, Clock.systemUTC());
    }

    /**
     * 使用指定时钟创建 Task 应用服务，测试时可传入固定时钟。
     */
    public TaskApplicationService(TaskRepository taskRepository, MissionRepository missionRepository,
                                  TaskEventRepository taskEventRepository,
                                  TaskAttemptRepository taskAttemptRepository,
                                  Clock clock) {
        this.taskRepository = Objects.requireNonNull(taskRepository, "taskRepository must not be null");
        this.missionRepository = Objects.requireNonNull(missionRepository, "missionRepository must not be null");
        this.taskEventRepository = Objects.requireNonNull(taskEventRepository, "taskEventRepository must not be null");
        this.taskAttemptRepository = Objects.requireNonNull(taskAttemptRepository, "taskAttemptRepository must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    /**
     * 创建草稿 Task，并记录 Task 创建事件。
     */
    public Task createTask(CreateTaskInput input) {
        Objects.requireNonNull(input, "input must not be null");
        if (!missionRepository.existsById(input.missionId())) {
            throw new NoSuchElementException("Mission not found: " + input.missionId());
        }

        Instant now = clock.instant();
        Task task = Task.draft(nextId("task"), input.missionId(), input.taskType(), input.priority(),
                input.deviceId(), input.modelId(), input.configSnapshot(), input.inputAssetIds(),
                input.createdBy(), now);
        Task savedTask = taskRepository.save(task);
        recordEvent(savedTask.taskId(), TaskEventType.CREATED, null, savedTask.status(),
                Map.of("createdBy", savedTask.createdBy()));
        return savedTask;
    }

    /**
     * 提交草稿 Task，使其进入排队状态，并记录提交事件。
     */
    public Task submitTask(String taskId, String submittedBy) {
        Task task = getRequiredTask(taskId);
        TaskStatus statusBefore = task.status();
        Task submittedTask = task.transitionTo(TaskStatus.QUEUED, clock.instant());
        Task savedTask = taskRepository.save(submittedTask);
        recordEvent(savedTask.taskId(), TaskEventType.SUBMITTED, statusBefore, savedTask.status(),
                Map.of("submittedBy", defaultText(submittedBy, "mock-user")));
        return savedTask;
    }

    public TaskAttempt startTaskAttempt(String taskId, String executorNode) {
        Task task = getRequiredTask(taskId);
        if (findRunningAttempt(taskId).isPresent()) {
            throw new IllegalStateException("Task already has a running attempt: " + taskId);
        }

        Instant now = clock.instant();
        TaskStatus statusBefore = task.status();
        Task runningTask = task.status() == TaskStatus.RUNNING
                ? task
                : task.transitionTo(TaskStatus.RUNNING, now);
        Task savedTask = taskRepository.save(runningTask);

        TaskAttempt attempt = TaskAttempt.started(nextId("attempt"), taskId, nextAttemptNo(taskId),
                defaultText(executorNode, "mock-executor"), now);
        TaskAttempt savedAttempt = taskAttemptRepository.save(attempt);
        recordEvent(savedTask.taskId(), TaskEventType.STARTED, statusBefore, savedTask.status(),
                Map.of("attemptId", savedAttempt.attemptId(),
                        "attemptNo", savedAttempt.attemptNo(),
                        "executorNode", savedAttempt.executorNode()));
        return savedAttempt;
    }

    public Task failTask(String taskId, String errorCode, String errorMessage, String rawLogObjectKey) {
        Task task = getRequiredTask(taskId);
        Instant now = clock.instant();
        TaskStatus statusBefore = task.status();
        String nextErrorCode = defaultText(errorCode, "TASK_FAILED");
        String nextErrorMessage = defaultText(errorMessage, "Task failed");

        Optional<TaskAttempt> runningAttempt = findRunningAttempt(taskId);
        runningAttempt
                .map(attempt -> attempt.fail(now, nextErrorCode, nextErrorMessage, rawLogObjectKey))
                .ifPresent(taskAttemptRepository::save);

        Task failedTask = taskRepository.save(task.fail(nextErrorCode, nextErrorMessage, now));
        Map<String, Object> payload = new java.util.LinkedHashMap<>();
        payload.put("errorCode", nextErrorCode);
        payload.put("errorMessage", nextErrorMessage);
        runningAttempt.map(TaskAttempt::attemptId).ifPresent(attemptId -> payload.put("attemptId", attemptId));
        if (rawLogObjectKey != null && !rawLogObjectKey.isBlank()) {
            payload.put("rawLogObjectKey", rawLogObjectKey);
        }
        recordEvent(failedTask.taskId(), TaskEventType.FAILED, statusBefore, failedTask.status(), payload);
        return failedTask;
    }

    public Task completeTask(String taskId, String rawLogObjectKey) {
        Task task = getRequiredTask(taskId);
        Instant now = clock.instant();
        TaskStatus statusBefore = task.status();

        Optional<TaskAttempt> runningAttempt = findRunningAttempt(taskId);
        runningAttempt
                .map(attempt -> attempt.complete(now, rawLogObjectKey))
                .ifPresent(taskAttemptRepository::save);

        Task completedTask = taskRepository.save(task.transitionTo(TaskStatus.COMPLETED, now));
        Map<String, Object> payload = new java.util.LinkedHashMap<>();
        runningAttempt.map(TaskAttempt::attemptId).ifPresent(attemptId -> payload.put("attemptId", attemptId));
        if (rawLogObjectKey != null && !rawLogObjectKey.isBlank()) {
            payload.put("rawLogObjectKey", rawLogObjectKey);
        }
        recordEvent(completedTask.taskId(), TaskEventType.COMPLETED, statusBefore, completedTask.status(), payload);
        return completedTask;
    }

    public Task timeoutTask(String taskId, String errorMessage, String rawLogObjectKey) {
        Task task = getRequiredTask(taskId);
        Instant now = clock.instant();
        TaskStatus statusBefore = task.status();
        String nextErrorMessage = defaultText(errorMessage, "Task timed out");

        Optional<TaskAttempt> runningAttempt = findRunningAttempt(taskId);
        runningAttempt
                .map(attempt -> attempt.timeout(now, "TASK_TIMEOUT", nextErrorMessage, rawLogObjectKey))
                .ifPresent(taskAttemptRepository::save);

        Task timeoutTask = taskRepository.save(task.transitionTo(TaskStatus.TIMEOUT, now));
        Map<String, Object> payload = new java.util.LinkedHashMap<>();
        payload.put("errorCode", "TASK_TIMEOUT");
        payload.put("errorMessage", nextErrorMessage);
        runningAttempt.map(TaskAttempt::attemptId).ifPresent(attemptId -> payload.put("attemptId", attemptId));
        if (rawLogObjectKey != null && !rawLogObjectKey.isBlank()) {
            payload.put("rawLogObjectKey", rawLogObjectKey);
        }
        recordEvent(timeoutTask.taskId(), TaskEventType.TIMEOUT, statusBefore, timeoutTask.status(), payload);
        return timeoutTask;
    }

    public Task retryTask(String taskId, String requestedBy) {
        Task task = getRequiredTask(taskId);
        Instant now = clock.instant();
        TaskStatus statusBefore = task.status();
        Task retryingTask = task.transitionTo(TaskStatus.RETRYING, now);
        Task queuedTask = taskRepository.save(retryingTask.transitionTo(TaskStatus.QUEUED, now));
        recordEvent(queuedTask.taskId(), TaskEventType.RETRYING, statusBefore, queuedTask.status(),
                Map.of("requestedBy", defaultText(requestedBy, "mock-user")));
        return queuedTask;
    }

    public Task cancelTask(String taskId, String cancelledBy) {
        Task task = getRequiredTask(taskId);
        Instant now = clock.instant();
        TaskStatus statusBefore = task.status();
        findRunningAttempt(taskId)
                .map(attempt -> attempt.cancel(now, null))
                .ifPresent(taskAttemptRepository::save);

        Task cancelledTask = taskRepository.save(task.transitionTo(TaskStatus.CANCELLED, now));
        recordEvent(cancelledTask.taskId(), TaskEventType.CANCELLED, statusBefore, cancelledTask.status(),
                Map.of("cancelledBy", defaultText(cancelledBy, "mock-user")));
        return cancelledTask;
    }

    /**
     * 按编号读取 Task，不存在时抛出异常。
     */
    private Task getRequiredTask(String taskId) {
        return taskRepository.findById(taskId)
                .orElseThrow(() -> new NoSuchElementException("Task not found: " + taskId));
    }

    private int nextAttemptNo(String taskId) {
        return taskAttemptRepository.findByTaskId(taskId).stream()
                .mapToInt(TaskAttempt::attemptNo)
                .max()
                .orElse(0) + 1;
    }

    private Optional<TaskAttempt> findRunningAttempt(String taskId) {
        return taskAttemptRepository.findByTaskId(taskId).stream()
                .filter(attempt -> attempt.result() == TaskAttemptResult.RUNNING)
                .reduce((first, second) -> second);
    }

    /**
     * 写入 Task 事件。
     */
    private void recordEvent(String taskId, TaskEventType eventType, TaskStatus statusBefore,
                             TaskStatus statusAfter, Map<String, Object> payload) {
        TaskEvent event = new TaskEvent(nextId("event"), taskId, eventType, statusBefore,
                statusAfter, payload, clock.instant());
        taskEventRepository.save(event);
    }

    /**
     * 生成带领域前缀的本地唯一编号。
     */
    private String nextId(String prefix) {
        return prefix + "-" + UUID.randomUUID();
    }

    private String defaultText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
