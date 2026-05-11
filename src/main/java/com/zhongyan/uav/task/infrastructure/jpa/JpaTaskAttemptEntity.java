package com.zhongyan.uav.task.infrastructure.jpa;

import com.zhongyan.uav.task.domain.TaskAttempt;
import com.zhongyan.uav.task.domain.TaskAttemptResult;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "task_attempt")
public class JpaTaskAttemptEntity {
    @Id
    @Column(name = "attempt_id", length = 80)
    private String attemptId;

    @Column(name = "task_id", nullable = false, length = 80)
    private String taskId;

    @Column(name = "attempt_no", nullable = false)
    private int attemptNo;

    @Column(name = "executor_node", length = 160)
    private String executorNode;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "ended_at")
    private Instant endedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "result", nullable = false, length = 32)
    private TaskAttemptResult result;

    @Column(name = "error_code", length = 120)
    private String errorCode;

    @Column(name = "error_message")
    private String errorMessage;

    @Column(name = "raw_log_object_key", length = 500)
    private String rawLogObjectKey;

    protected JpaTaskAttemptEntity() {
    }

    public static JpaTaskAttemptEntity fromDomain(TaskAttempt attempt) {
        JpaTaskAttemptEntity entity = new JpaTaskAttemptEntity();
        entity.attemptId = attempt.attemptId();
        entity.taskId = attempt.taskId();
        entity.attemptNo = attempt.attemptNo();
        entity.executorNode = attempt.executorNode();
        entity.startedAt = attempt.startedAt();
        entity.endedAt = attempt.endedAt();
        entity.result = attempt.result();
        entity.errorCode = attempt.errorCode();
        entity.errorMessage = attempt.errorMessage();
        entity.rawLogObjectKey = attempt.rawLogObjectKey();
        return entity;
    }

    public TaskAttempt toDomain() {
        return new TaskAttempt(attemptId, taskId, attemptNo, executorNode, startedAt, endedAt,
                result, errorCode, errorMessage, rawLogObjectKey);
    }
}
