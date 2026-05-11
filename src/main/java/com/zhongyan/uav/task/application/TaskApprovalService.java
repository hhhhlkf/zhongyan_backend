package com.zhongyan.uav.task.application;

import com.zhongyan.uav.task.domain.TaskCommand;
import com.zhongyan.uav.task.domain.TaskCommandRepository;
import com.zhongyan.uav.task.domain.TaskCommandStatus;
import com.zhongyan.uav.task.domain.TaskEvent;
import com.zhongyan.uav.task.domain.TaskEventRepository;
import com.zhongyan.uav.task.domain.TaskEventType;

import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.UUID;

public class TaskApprovalService {
    private final TaskCommandRepository taskCommandRepository;
    private final TaskEventRepository taskEventRepository;
    private final Clock clock;

    /**
     * 使用系统时钟创建审批服务。
     */
    public TaskApprovalService(TaskCommandRepository taskCommandRepository,
                               TaskEventRepository taskEventRepository) {
        this(taskCommandRepository, taskEventRepository, Clock.systemUTC());
    }

    /**
     * 使用指定时钟创建审批服务，测试时可传入固定时钟。
     */
    public TaskApprovalService(TaskCommandRepository taskCommandRepository,
                               TaskEventRepository taskEventRepository, Clock clock) {
        this.taskCommandRepository = Objects.requireNonNull(taskCommandRepository, "taskCommandRepository must not be null");
        this.taskEventRepository = Objects.requireNonNull(taskEventRepository, "taskEventRepository must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    /**
     * 查询待审批命令列表。
     */
    public List<TaskCommand> listPendingApprovals() {
        return taskCommandRepository.findByStatus(TaskCommandStatus.PENDING_APPROVAL);
    }

    /**
     * 按命令编号查询命令详情。
     */
    public TaskCommand getCommand(String commandId) {
        return getRequiredCommand(commandId);
    }

    /**
     * 审批通过命令，并记录审批事件。
     */
    public TaskCommand approveCommand(String commandId, String approver) {
        TaskCommand command = getRequiredCommand(commandId);
        TaskCommand approvedCommand = taskCommandRepository.save(command.approve(approver, clock.instant()));
        recordApprovalEvent(approvedCommand, TaskEventType.APPROVED, approver);
        return approvedCommand;
    }

    /**
     * 拒绝命令，并记录审批事件。
     */
    public TaskCommand rejectCommand(String commandId, String approver) {
        TaskCommand command = getRequiredCommand(commandId);
        TaskCommand rejectedCommand = taskCommandRepository.save(command.reject(approver, clock.instant()));
        recordApprovalEvent(rejectedCommand, TaskEventType.CANCELLED, approver);
        return rejectedCommand;
    }

    /**
     * 按编号读取命令，不存在时抛出异常。
     */
    private TaskCommand getRequiredCommand(String commandId) {
        return taskCommandRepository.findById(commandId)
                .orElseThrow(() -> new NoSuchElementException("Task command not found: " + commandId));
    }

    /**
     * 写入命令审批事件。
     */
    private void recordApprovalEvent(TaskCommand command, TaskEventType eventType, String approver) {
        TaskEvent event = new TaskEvent(nextId("event"), command.taskId(), eventType, null,
                null, Map.of("commandId", command.commandId(), "approver", approver,
                "commandStatus", command.status().name()), clock.instant());
        taskEventRepository.save(event);
    }

    /**
     * 生成带领域前缀的本地唯一编号。
     */
    private String nextId(String prefix) {
        return prefix + "-" + UUID.randomUUID();
    }
}
