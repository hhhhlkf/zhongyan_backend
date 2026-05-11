package com.zhongyan.uav.task.domain;

import java.util.List;
import java.util.Optional;

public interface TaskCommandRepository {
    /**
     * 保存 TaskCommand，具体持久化方式由基础设施层实现。
     */
    TaskCommand save(TaskCommand command);

    /**
     * 按命令编号查找命令。
     */
    Optional<TaskCommand> findById(String commandId);

    /**
     * 按幂等键查找命令，用于避免重复创建。
     */
    Optional<TaskCommand> findByIdempotencyKey(String idempotencyKey);

    /**
     * 查询某个 Task 下的全部命令。
     */
    List<TaskCommand> findByTaskId(String taskId);

    /**
     * 按命令状态查询列表，用于审批列表或调度扫描。
     */
    List<TaskCommand> findByStatus(TaskCommandStatus status);
}
