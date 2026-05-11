package com.zhongyan.uav.task.application;

import com.zhongyan.uav.common.error.BusinessException;
import com.zhongyan.uav.common.error.ErrorCode;
import com.zhongyan.uav.task.domain.RiskLevel;
import com.zhongyan.uav.task.domain.Task;
import com.zhongyan.uav.task.domain.TaskAttempt;
import com.zhongyan.uav.task.domain.TaskAttemptRepository;
import com.zhongyan.uav.task.domain.TaskAttemptResult;
import com.zhongyan.uav.task.domain.TaskCommand;
import com.zhongyan.uav.task.domain.TaskCommandRepository;
import com.zhongyan.uav.task.domain.TaskCommandStatus;
import com.zhongyan.uav.task.domain.TaskCommandType;
import com.zhongyan.uav.task.domain.TaskEvent;
import com.zhongyan.uav.task.domain.TaskEventRepository;
import com.zhongyan.uav.task.domain.TaskEventType;
import com.zhongyan.uav.task.domain.TaskRepository;
import com.zhongyan.uav.task.domain.TaskStatus;
import com.zhongyan.uav.task.domain.TaskType;
import com.zhongyan.uav.task.executor.TaskExecutionContext;
import com.zhongyan.uav.task.executor.TaskExecutionResult;
import com.zhongyan.uav.task.executor.TaskExecutor;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

public class TaskSchedulerService {
    private final TaskRepository taskRepository;
    private final TaskAttemptRepository taskAttemptRepository;
    private final TaskCommandRepository taskCommandRepository;
    private final TaskEventRepository taskEventRepository;
    private final TaskCommandApplicationService taskCommandApplicationService;
    private final TaskApplicationService taskApplicationService;
    private final Map<TaskType, TaskExecutor> executors;
    private final Set<String> resourceLocks = ConcurrentHashMap.newKeySet();
    private final ExecutorService executorService;
    private final Clock clock;

    public TaskSchedulerService(TaskRepository taskRepository,
                                TaskAttemptRepository taskAttemptRepository,
                                TaskCommandRepository taskCommandRepository,
                                TaskEventRepository taskEventRepository,
                                TaskCommandApplicationService taskCommandApplicationService,
                                TaskApplicationService taskApplicationService,
                                List<TaskExecutor> executors) {
        this(taskRepository, taskAttemptRepository, taskCommandRepository, taskEventRepository,
                taskCommandApplicationService, taskApplicationService, executors, Clock.systemUTC());
    }

    public TaskSchedulerService(TaskRepository taskRepository,
                                TaskAttemptRepository taskAttemptRepository,
                                TaskCommandRepository taskCommandRepository,
                                TaskEventRepository taskEventRepository,
                                TaskCommandApplicationService taskCommandApplicationService,
                                TaskApplicationService taskApplicationService,
                                List<TaskExecutor> executors,
                                Clock clock) {
        this(taskRepository, taskAttemptRepository, taskCommandRepository, taskEventRepository,
                taskCommandApplicationService, taskApplicationService, executors, clock,
                Executors.newCachedThreadPool());
    }

    public TaskSchedulerService(TaskRepository taskRepository,
                                TaskAttemptRepository taskAttemptRepository,
                                TaskCommandRepository taskCommandRepository,
                                TaskEventRepository taskEventRepository,
                                TaskCommandApplicationService taskCommandApplicationService,
                                TaskApplicationService taskApplicationService,
                                List<TaskExecutor> executors,
                                Clock clock,
                                ExecutorService executorService) {
        this.taskRepository = Objects.requireNonNull(taskRepository, "taskRepository must not be null");
        this.taskAttemptRepository = Objects.requireNonNull(taskAttemptRepository, "taskAttemptRepository must not be null");
        this.taskCommandRepository = Objects.requireNonNull(taskCommandRepository, "taskCommandRepository must not be null");
        this.taskEventRepository = Objects.requireNonNull(taskEventRepository, "taskEventRepository must not be null");
        this.taskCommandApplicationService = Objects.requireNonNull(taskCommandApplicationService, "taskCommandApplicationService must not be null");
        this.taskApplicationService = Objects.requireNonNull(taskApplicationService, "taskApplicationService must not be null");
        this.executors = Objects.requireNonNull(executors, "executors must not be null").stream()
                .collect(Collectors.toMap(this::supportedType, executor -> executor));
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
        this.executorService = Objects.requireNonNull(executorService, "executorService must not be null");
    }

    public List<TaskScheduleResult> scheduleQueuedTasks(int limit) {
        return taskRepository.findByStatus(TaskStatus.QUEUED).stream()
                .sorted(Comparator.comparing(Task::createdAt).thenComparing(Task::priority).reversed())
                .limit(Math.max(limit, 0))
                .map(task -> scheduleTask(task.taskId()))
                .collect(Collectors.toList());
    }

    public TaskScheduleResult scheduleTask(String taskId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new java.util.NoSuchElementException("Task not found: " + taskId));
        if (task.status() != TaskStatus.QUEUED) {
            return new TaskScheduleResult(task.taskId(), null, TaskScheduleStatus.SKIPPED,
                    "Task is not queued: " + task.status());
        }

        TaskExecutor executor = executors.get(task.taskType());
        if (executor == null) {
            return new TaskScheduleResult(task.taskId(), null, TaskScheduleStatus.SKIPPED,
                    "No executor for task type: " + task.taskType());
        }

        TaskCommandType commandType = commandTypeFor(task.taskType());
        Optional<TaskCommand> dispatchableCommand = findCommand(task.taskId(), commandType,
                TaskCommandStatus.PENDING_DISPATCH);
        if (dispatchableCommand.isEmpty()) {
            TaskCommand command = ensureCommand(task, commandType);
            TaskScheduleStatus status = command.status() == TaskCommandStatus.PENDING_APPROVAL
                    ? TaskScheduleStatus.WAITING_APPROVAL
                    : TaskScheduleStatus.COMMAND_CREATED;
            return new TaskScheduleResult(task.taskId(), command.commandId(), status,
                    "Task command is not ready for dispatch: " + command.status());
        }

        return executeWithLock(task, dispatchableCommand.get(), executor);
    }

    public List<TaskScheduleResult> recoverStaleTasks(Duration staleAfter, int limit) {
        Duration threshold = staleAfter == null ? Duration.ofMinutes(30) : staleAfter;
        List<TaskScheduleResult> results = new ArrayList<>();
        for (TaskStatus status : List.of(TaskStatus.DISPATCHING, TaskStatus.RUNNING)) {
            for (Task task : taskRepository.findByStatus(status)) {
                if (results.size() >= limit) {
                    return results;
                }
                findRunningAttempt(task.taskId())
                        .filter(attempt -> isStale(attempt.startedAt(), threshold))
                        .ifPresent(attempt -> results.add(timeoutStaleTask(task, attempt)));
            }
        }
        if (results.size() < limit) {
            results.addAll(scheduleQueuedTasks(limit - results.size()));
        }
        return results;
    }

    private TaskScheduleResult executeWithLock(Task task, TaskCommand command, TaskExecutor executor) {
        String resourceKey = resourceKey(task);
        if (!resourceLocks.add(resourceKey)) {
            return new TaskScheduleResult(task.taskId(), command.commandId(), TaskScheduleStatus.SKIPPED,
                    "Resource is locked: " + resourceKey);
        }
        TaskCommand dispatched = command;
        try {
            dispatched = taskCommandRepository.save(command.markDispatched(clock.instant()));
            recordEvent(dispatched, TaskEventType.DISPATCHED, Map.of("commandId", dispatched.commandId(),
                    "commandType", dispatched.commandType().name(), "resourceKey", resourceKey));
            taskApplicationService.startTaskAttempt(task.taskId(), "task-scheduler");
            TaskCommand commandToExecute = dispatched;
            TaskExecutionResult result = executeWithTimeout(task,
                    () -> executor.execute(new TaskExecutionContext(task, commandToExecute)));
            if (result.success()) {
                taskCommandRepository.save(dispatched.complete(result.completedAt()));
                taskApplicationService.completeTask(task.taskId(), result.rawLogObjectKey());
                return new TaskScheduleResult(task.taskId(), dispatched.commandId(),
                        TaskScheduleStatus.EXECUTED, "Task completed");
            }
            taskCommandRepository.save(dispatched.fail(result.completedAt()));
            taskApplicationService.failTask(task.taskId(), result.errorCode(), result.errorMessage(),
                    result.rawLogObjectKey());
            return retryOrFinish(task, dispatched.commandId(), result.errorMessage());
        } catch (TimeoutException ex) {
            if (dispatched.status() == TaskCommandStatus.DISPATCHED) {
                taskCommandRepository.save(dispatched.fail(clock.instant()));
            }
            taskApplicationService.timeoutTask(task.taskId(), "Task execution timed out", null);
            return retryOrTimeout(task, dispatched.commandId());
        } catch (RuntimeException ex) {
            taskApplicationService.failTask(task.taskId(), "TASK_EXECUTOR_ERROR", ex.getMessage(), null);
            return retryOrFinish(task, dispatched.commandId(), ex.getMessage());
        } finally {
            resourceLocks.remove(resourceKey);
        }
    }

    private TaskCommand ensureCommand(Task task, TaskCommandType commandType) {
        return findActiveCommand(task.taskId(), commandType)
                .orElseGet(() -> taskCommandApplicationService.createCommand(task.taskId(),
                        new CreateTaskCommandInput(commandType, task.configSnapshot(),
                                idempotencyKey(task, commandType),
                                "task-scheduler", commandType.defaultRiskLevel(), task.deviceId(),
                                "auto-created by task scheduler")));
    }

    private Optional<TaskCommand> findActiveCommand(String taskId, TaskCommandType commandType) {
        return taskCommandRepository.findByTaskId(taskId).stream()
                .filter(command -> command.commandType() == commandType)
                .filter(command -> !command.status().isTerminal())
                .findFirst();
    }

    private Optional<TaskCommand> findCommand(String taskId, TaskCommandType commandType, TaskCommandStatus status) {
        return taskCommandRepository.findByTaskId(taskId).stream()
                .filter(command -> command.commandType() == commandType)
                .filter(command -> command.status() == status)
                .findFirst();
    }

    private TaskCommandType commandTypeFor(TaskType taskType) {
        return switch (taskType) {
            case CAPTURE -> TaskCommandType.START_CAPTURE;
            case PROCESS -> TaskCommandType.START_PROCESS;
            case TRANSFER -> TaskCommandType.START_TRANSFER;
            case CALCULATE_GEO_BOUNDARY -> TaskCommandType.CALCULATE_GEO_BOUNDARY;
            case GENERATE_PREVIEW -> TaskCommandType.GENERATE_PREVIEW;
            case PUBLISH_LAYER -> TaskCommandType.PUBLISH_LAYER;
            case DEMO_PLAYBACK -> TaskCommandType.START_DEMO_PLAYBACK;
            case AGENT_ANALYSIS -> TaskCommandType.START_AGENT_ANALYSIS;
            case REPORT_GENERATION -> TaskCommandType.GENERATE_REPORT;
        };
    }

    private String resourceKey(Task task) {
        return switch (task.taskType()) {
            case PROCESS -> defaultText(task.modelId(), task.taskType().name());
            case CALCULATE_GEO_BOUNDARY, GENERATE_PREVIEW, PUBLISH_LAYER ->
                    "asset:" + String.join(",", task.inputAssetIds());
            case AGENT_ANALYSIS -> "agent:" + defaultText(task.modelId(), task.taskId());
            case REPORT_GENERATION -> "report:" + task.missionId();
            default -> defaultText(task.deviceId(), task.taskType().name());
        };
    }

    private TaskType supportedType(TaskExecutor executor) {
        return EnumSet.allOf(TaskType.class).stream()
                .filter(executor::supports)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Executor must support one known task type"));
    }

    private TaskExecutionResult executeWithTimeout(Task task, Callable<TaskExecutionResult> callable)
            throws TimeoutException {
        Future<TaskExecutionResult> future = executorService.submit(callable);
        Duration timeout = timeout(task);
        try {
            return future.get(timeout.toMillis(), TimeUnit.MILLISECONDS);
        } catch (TimeoutException ex) {
            future.cancel(true);
            throw ex;
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Task execution interrupted", ex);
        } catch (ExecutionException ex) {
            Throwable cause = ex.getCause();
            if (cause instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw new IllegalStateException("Task execution failed", cause);
        }
    }

    private TaskScheduleResult retryOrFinish(Task task, String commandId, String message) {
        if (shouldRetry(task.taskId())) {
            taskApplicationService.retryTask(task.taskId(), "task-scheduler");
            return new TaskScheduleResult(task.taskId(), commandId, TaskScheduleStatus.RETRY_QUEUED,
                    defaultText(message, "Task failed and was queued for retry"));
        }
        return new TaskScheduleResult(task.taskId(), commandId, TaskScheduleStatus.FAILED,
                defaultText(message, "Task failed"));
    }

    private TaskScheduleResult retryOrTimeout(Task task, String commandId) {
        if (shouldRetry(task.taskId())) {
            taskApplicationService.retryTask(task.taskId(), "task-scheduler");
            return new TaskScheduleResult(task.taskId(), commandId, TaskScheduleStatus.RETRY_QUEUED,
                    "Task timed out and was queued for retry");
        }
        return new TaskScheduleResult(task.taskId(), commandId, TaskScheduleStatus.TIMEOUT,
                "Task timed out");
    }

    private TaskScheduleResult timeoutStaleTask(Task task, TaskAttempt attempt) {
        taskApplicationService.timeoutTask(task.taskId(), "Recovered stale running task", attempt.rawLogObjectKey());
        return new TaskScheduleResult(task.taskId(), null, TaskScheduleStatus.RECOVERED,
                "Recovered stale task attempt: " + attempt.attemptId());
    }

    private boolean shouldRetry(String taskId) {
        List<TaskAttempt> attempts = taskAttemptRepository.findByTaskId(taskId);
        int maxRetries = attempts.stream()
                .findFirst()
                .map(attempt -> maxRetries(taskRepository.findById(taskId).orElseThrow()))
                .orElse(0);
        long finishedFailures = attempts.stream()
                .filter(attempt -> attempt.result() == TaskAttemptResult.FAILED || attempt.result() == TaskAttemptResult.TIMEOUT)
                .count();
        return finishedFailures <= maxRetries;
    }

    private Optional<TaskAttempt> findRunningAttempt(String taskId) {
        return taskAttemptRepository.findByTaskId(taskId).stream()
                .filter(attempt -> attempt.result() == TaskAttemptResult.RUNNING)
                .reduce((first, second) -> second);
    }

    private boolean isStale(Instant startedAt, Duration threshold) {
        return startedAt.plus(threshold).isBefore(clock.instant()) || startedAt.plus(threshold).equals(clock.instant());
    }

    private String idempotencyKey(Task task, TaskCommandType commandType) {
        long existingCommands = taskCommandRepository.findByTaskId(task.taskId()).stream()
                .filter(command -> command.commandType() == commandType)
                .count();
        return "scheduler:" + task.taskId() + ":" + commandType.name() + ":" + (existingCommands + 1);
    }

    private Duration timeout(Task task) {
        Object value = task.configSnapshot().get("timeoutMs");
        if (value instanceof Number number) {
            return Duration.ofMillis(Math.max(1, number.longValue()));
        }
        if (value instanceof String text && !text.isBlank()) {
            return Duration.ofMillis(Math.max(1, Long.parseLong(text)));
        }
        return Duration.ofSeconds(30);
    }

    private int maxRetries(Task task) {
        Object value = task.configSnapshot().get("maxRetries");
        if (value instanceof Number number) {
            return Math.max(0, number.intValue());
        }
        if (value instanceof String text && !text.isBlank()) {
            return Math.max(0, Integer.parseInt(text));
        }
        return 0;
    }

    private void recordEvent(TaskCommand command, TaskEventType eventType, Map<String, Object> payload) {
        taskEventRepository.save(new TaskEvent("event-" + UUID.randomUUID(), command.taskId(),
                eventType, null, TaskStatus.RUNNING, payload, clock.instant()));
    }

    private String defaultText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
