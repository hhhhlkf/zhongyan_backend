package com.zhongyan.uav.task.domain;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

public final class TaskStateMachine {
    private static final Map<TaskStatus, Set<TaskStatus>> TRANSITIONS = new EnumMap<>(TaskStatus.class);

    static {
        allow(TaskStatus.DRAFT, TaskStatus.WAITING_APPROVAL, TaskStatus.QUEUED, TaskStatus.CANCELLED);
        allow(TaskStatus.WAITING_APPROVAL, TaskStatus.QUEUED, TaskStatus.CANCELLED);
        allow(TaskStatus.QUEUED, TaskStatus.DISPATCHING, TaskStatus.RUNNING, TaskStatus.CANCELLED);
        allow(TaskStatus.DISPATCHING, TaskStatus.RUNNING, TaskStatus.FAILED, TaskStatus.TIMEOUT, TaskStatus.CANCELLED);
        allow(TaskStatus.RUNNING, TaskStatus.WAITING_ASSET, TaskStatus.POST_PROCESSING, TaskStatus.COMPLETED,
                TaskStatus.FAILED, TaskStatus.TIMEOUT, TaskStatus.CANCELLED);
        allow(TaskStatus.WAITING_ASSET, TaskStatus.POST_PROCESSING, TaskStatus.COMPLETED,
                TaskStatus.FAILED, TaskStatus.TIMEOUT, TaskStatus.CANCELLED);
        allow(TaskStatus.POST_PROCESSING, TaskStatus.COMPLETED, TaskStatus.FAILED, TaskStatus.TIMEOUT, TaskStatus.CANCELLED);
        allow(TaskStatus.FAILED, TaskStatus.RETRYING);
        allow(TaskStatus.TIMEOUT, TaskStatus.RETRYING);
        allow(TaskStatus.RETRYING, TaskStatus.WAITING_APPROVAL, TaskStatus.QUEUED, TaskStatus.CANCELLED);
        allow(TaskStatus.COMPLETED);
        allow(TaskStatus.CANCELLED);
    }

    /**
     * 工具类不允许实例化。
     */
    private TaskStateMachine() {
    }

    /**
     * 判断当前状态是否允许流转到目标状态。
     */
    public static boolean canTransition(TaskStatus current, TaskStatus next) {
        if (current == null || next == null) {
            return false;
        }
        return TRANSITIONS.getOrDefault(current, Set.of()).contains(next);
    }

    /**
     * 校验状态流转是否合法，非法流转直接抛出异常。
     */
    public static void assertCanTransition(TaskStatus current, TaskStatus next) {
        if (!canTransition(current, next)) {
            throw new IllegalStateException("Task cannot transition from " + current + " to " + next);
        }
    }

    /**
     * 注册一个状态允许到达的后续状态集合。
     */
    private static void allow(TaskStatus current, TaskStatus... nextStatuses) {
        TRANSITIONS.put(current, nextStatuses.length == 0 ? EnumSet.noneOf(TaskStatus.class) : EnumSet.of(nextStatuses[0], nextStatuses));
    }
}
