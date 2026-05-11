package com.zhongyan.uav.task.api.response;

import com.zhongyan.uav.task.domain.TaskAttempt;
import com.zhongyan.uav.task.domain.TaskAttemptResult;

import java.time.Instant;

public record TaskAttemptView(
        String attemptId,
        String taskId,
        int attemptNo,
        String executorNode,
        Instant startedAt,
        Instant endedAt,
        TaskAttemptResult result,
        String errorCode,
        String errorMessage,
        String rawLogObjectKey) {
    public static TaskAttemptView from(TaskAttempt attempt) {
        return new TaskAttemptView(attempt.attemptId(), attempt.taskId(), attempt.attemptNo(),
                attempt.executorNode(), attempt.startedAt(), attempt.endedAt(), attempt.result(),
                attempt.errorCode(), attempt.errorMessage(), attempt.rawLogObjectKey());
    }
}
