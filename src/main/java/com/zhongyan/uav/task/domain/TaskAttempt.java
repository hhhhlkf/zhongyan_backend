package com.zhongyan.uav.task.domain;

import java.time.Instant;
import java.util.Objects;

public record TaskAttempt(
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
    /**
     * 校验 Attempt 必填字段，并给执行结果设置默认运行中状态。
     */
    public TaskAttempt {
        requireText(attemptId, "attemptId");
        requireText(taskId, "taskId");
        if (attemptNo < 1) {
            throw new IllegalArgumentException("attemptNo must be greater than 0");
        }
        Objects.requireNonNull(startedAt, "startedAt must not be null");
        result = result == null ? TaskAttemptResult.RUNNING : result;
    }

    /**
     * 创建一次已经开始的 Task 执行尝试。
     */
    public static TaskAttempt started(String attemptId, String taskId, int attemptNo,
                                      String executorNode, Instant startedAt) {
        return new TaskAttempt(attemptId, taskId, attemptNo, executorNode, startedAt, null,
                TaskAttemptResult.RUNNING, null, null, null);
    }

    /**
     * 将 Attempt 标记为成功结束，并绑定原始日志对象 key。
     */
    public TaskAttempt complete(Instant endedAt, String rawLogObjectKey) {
        return finish(TaskAttemptResult.SUCCESS, endedAt, null, null, rawLogObjectKey);
    }

    /**
     * 将 Attempt 标记为失败结束，并记录错误信息和原始日志对象 key。
     */
    public TaskAttempt fail(Instant endedAt, String errorCode, String errorMessage, String rawLogObjectKey) {
        return finish(TaskAttemptResult.FAILED, endedAt, errorCode, errorMessage, rawLogObjectKey);
    }

    public TaskAttempt cancel(Instant endedAt, String rawLogObjectKey) {
        return finish(TaskAttemptResult.CANCELLED, endedAt, null, null, rawLogObjectKey);
    }

    public TaskAttempt timeout(Instant endedAt, String errorCode, String errorMessage, String rawLogObjectKey) {
        return finish(TaskAttemptResult.TIMEOUT, endedAt, errorCode, errorMessage, rawLogObjectKey);
    }

    /**
     * 统一处理 Attempt 结束时的字段复制。
     */
    private TaskAttempt finish(TaskAttemptResult nextResult, Instant nextEndedAt, String nextErrorCode,
                               String nextErrorMessage, String nextRawLogObjectKey) {
        Objects.requireNonNull(nextEndedAt, "endedAt must not be null");
        return new TaskAttempt(attemptId, taskId, attemptNo, executorNode, startedAt, nextEndedAt,
                nextResult, nextErrorCode, nextErrorMessage, nextRawLogObjectKey);
    }

    /**
     * 校验文本字段必须有实际内容。
     */
    private static void requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
    }
}
