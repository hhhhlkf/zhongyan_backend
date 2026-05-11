package com.zhongyan.uav.mission.domain;

import java.time.Instant;
import java.util.Objects;

public record Mission(
        String missionId,
        String name,
        String scenarioType,
        MissionRegion region,
        MissionStatus status,
        int priority,
        String createdBy,
        Instant createdAt,
        Instant startedAt,
        Instant endedAt,
        String description) {
    /**
     * 校验 Mission 聚合的必填字段，避免创建无效任务流程。
     */
    public Mission {
        requireText(missionId, "missionId");
        requireText(name, "name");
        requireText(scenarioType, "scenarioType");
        Objects.requireNonNull(status, "status must not be null");
        requireText(createdBy, "createdBy");
        Objects.requireNonNull(createdAt, "createdAt must not be null");
    }

    /**
     * 创建草稿状态的 Mission，用于后续提交、启动或取消。
     */
    public static Mission draft(String missionId, String name, String scenarioType,
                                MissionRegion region, int priority, String createdBy, Instant now,
                                String description) {
        return new Mission(missionId, name, scenarioType, region, MissionStatus.DRAFT, priority,
                createdBy, now, null, null, description);
    }

    /**
     * 启动 Mission，只允许从草稿或暂停状态进入激活状态。
     */
    public Mission start(Instant now) {
        if (status != MissionStatus.DRAFT && status != MissionStatus.PAUSED) {
            throw new IllegalStateException("Mission can only start from DRAFT or PAUSED");
        }
        return new Mission(missionId, name, scenarioType, region, MissionStatus.ACTIVE, priority,
                createdBy, createdAt, startedAt == null ? now : startedAt, null, description);
    }

    /**
     * 暂停 Mission，只允许正在激活的 Mission 暂停。
     */
    public Mission pause() {
        if (status != MissionStatus.ACTIVE) {
            throw new IllegalStateException("Mission can only pause from ACTIVE");
        }
        return new Mission(missionId, name, scenarioType, region, MissionStatus.PAUSED, priority,
                createdBy, createdAt, startedAt, endedAt, description);
    }

    /**
     * 完成 Mission，并记录结束时间。
     */
    public Mission complete(Instant now) {
        if (status != MissionStatus.ACTIVE && status != MissionStatus.PAUSED) {
            throw new IllegalStateException("Mission can only complete from ACTIVE or PAUSED");
        }
        return finish(MissionStatus.COMPLETED, now);
    }

    /**
     * 取消未终态的 Mission，并记录结束时间。
     */
    public Mission cancel(Instant now) {
        if (status.isTerminal()) {
            throw new IllegalStateException("Terminal mission cannot be cancelled");
        }
        return finish(MissionStatus.CANCELLED, now);
    }

    /**
     * 将 Mission 标记为失败，并记录结束时间。
     */
    public Mission fail(Instant now) {
        if (status.isTerminal()) {
            throw new IllegalStateException("Terminal mission cannot fail again");
        }
        return finish(MissionStatus.FAILED, now);
    }

    /**
     * 统一处理 Mission 进入终态时的字段复制和结束时间设置。
     */
    private Mission finish(MissionStatus nextStatus, Instant now) {
        Objects.requireNonNull(now, "now must not be null");
        return new Mission(missionId, name, scenarioType, region, nextStatus, priority,
                createdBy, createdAt, startedAt, now, description);
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
