package com.zhongyan.uav.task.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TaskCommandTests {
    private final Instant now = Instant.parse("2026-04-29T00:00:00Z");

    /**
     * 验证高风险命令创建后必须进入待审批状态。
     */
    @Test
    void highRiskCommandStartsWaitingForApproval() {
        TaskCommand command = TaskCommand.create(
                "cmd-1",
                "task-1",
                "mission-1",
                "device-1",
                TaskCommandType.START_CAPTURE,
                Map.of("cameraType", "rgb"),
                "idem-1",
                "operator-1",
                null,
                "start capture",
                now);

        assertThat(command.requiresApproval()).isTrue();
        assertThat(command.riskLevel()).isEqualTo(RiskLevel.HIGH);
        assertThat(command.status()).isEqualTo(TaskCommandStatus.PENDING_APPROVAL);
    }

    /**
     * 验证中风险命令可以直接进入待下发状态。
     */
    @Test
    void mediumRiskCommandCanBeDispatchedWithoutApproval() {
        TaskCommand command = TaskCommand.create(
                "cmd-2",
                "task-1",
                "mission-1",
                null,
                TaskCommandType.RETRY_TASK,
                Map.of(),
                "idem-2",
                "operator-1",
                RiskLevel.MEDIUM,
                "retry",
                now);

        assertThat(command.requiresApproval()).isFalse();
        assertThat(command.status()).isEqualTo(TaskCommandStatus.PENDING_DISPATCH);
    }

    /**
     * 验证审批通过后的命令可以下发并完成。
     */
    @Test
    void approvedCommandCanBeDispatchedAndCompleted() {
        TaskCommand command = TaskCommand.create(
                        "cmd-3",
                        "task-1",
                        "mission-1",
                        "device-1",
                        TaskCommandType.STOP_DEVICE,
                        Map.of(),
                        "idem-3",
                        "operator-1",
                        null,
                        "stop device",
                        now)
                .approve("admin-1", now.plusSeconds(10))
                .markDispatched(now.plusSeconds(20))
                .complete(now.plusSeconds(30));

        assertThat(command.status()).isEqualTo(TaskCommandStatus.COMPLETED);
        assertThat(command.approvedBy()).isEqualTo("admin-1");
        assertThat(command.dispatchedAt()).isEqualTo(now.plusSeconds(20));
        assertThat(command.completedAt()).isEqualTo(now.plusSeconds(30));
    }

    /**
     * 验证需要审批的命令不能绕过审批直接下发。
     */
    @Test
    void commandCannotDispatchBeforeApprovalWhenApprovalIsRequired() {
        TaskCommand command = TaskCommand.create(
                "cmd-4",
                "task-1",
                "mission-1",
                "device-1",
                TaskCommandType.PUBLISH_LAYER,
                Map.of(),
                "idem-4",
                "operator-1",
                null,
                "publish",
                now);

        assertThatThrownBy(() -> command.markDispatched(now.plusSeconds(1)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("cannot be dispatched");
    }
}
