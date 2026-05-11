package com.zhongyan.uav.task.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TaskStateMachineTests {
    private final Instant now = Instant.parse("2026-04-29T00:00:00Z");

    /**
     * 验证 Task 可以从排队、运行流转到完成，并自动设置时间和进度。
     */
    @Test
    void taskCanMoveThroughQueuedRunningAndCompleted() {
        Task task = Task.draft(
                        "task-1",
                        "mission-1",
                        TaskType.CAPTURE,
                        10,
                        "device-1",
                        null,
                        Map.of("cameraType", "rgb"),
                        List.of(),
                        "operator-1",
                        now)
                .transitionTo(TaskStatus.QUEUED, now.plusSeconds(1))
                .transitionTo(TaskStatus.RUNNING, now.plusSeconds(2))
                .updateProgress(BigDecimal.valueOf(50), now.plusSeconds(3))
                .transitionTo(TaskStatus.COMPLETED, now.plusSeconds(4));

        assertThat(task.status()).isEqualTo(TaskStatus.COMPLETED);
        assertThat(task.progress()).isEqualByComparingTo(BigDecimal.valueOf(100));
        assertThat(task.startedAt()).isEqualTo(now.plusSeconds(2));
        assertThat(task.endedAt()).isEqualTo(now.plusSeconds(4));
    }

    /**
     * 验证已完成的 Task 不能再次进入重试状态。
     */
    @Test
    void completedTaskCannotRetry() {
        assertThat(TaskStateMachine.canTransition(TaskStatus.COMPLETED, TaskStatus.RETRYING)).isFalse();
        assertThatThrownBy(() -> TaskStateMachine.assertCanTransition(TaskStatus.COMPLETED, TaskStatus.RETRYING))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("COMPLETED to RETRYING");
    }

    /**
     * 验证失败的 Task 可以进入重试，再重新排队。
     */
    @Test
    void failedTaskCanRetryAndQueueAgain() {
        Task task = Task.draft(
                        "task-2",
                        "mission-1",
                        TaskType.PROCESS,
                        5,
                        null,
                        "model-1",
                        Map.of("modelType", "detect"),
                        List.of("asset-1"),
                        "operator-1",
                        now)
                .transitionTo(TaskStatus.QUEUED, now.plusSeconds(1))
                .transitionTo(TaskStatus.RUNNING, now.plusSeconds(2))
                .fail("MODEL_FAILED", "model process failed", now.plusSeconds(3))
                .transitionTo(TaskStatus.RETRYING, now.plusSeconds(4))
                .transitionTo(TaskStatus.QUEUED, now.plusSeconds(5));

        assertThat(task.status()).isEqualTo(TaskStatus.QUEUED);
        assertThat(task.errorCode()).isEqualTo("MODEL_FAILED");
    }
}
