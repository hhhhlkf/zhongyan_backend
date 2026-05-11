package com.zhongyan.uav.mission.application;

import com.zhongyan.uav.mission.domain.Mission;
import com.zhongyan.uav.mission.domain.MissionRepository;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public class MissionApplicationService {
    private final MissionRepository missionRepository;
    private final Clock clock;

    /**
     * 使用系统时钟创建 Mission 应用服务。
     */
    public MissionApplicationService(MissionRepository missionRepository) {
        this(missionRepository, Clock.systemUTC());
    }

    /**
     * 使用指定时钟创建 Mission 应用服务，测试时可传入固定时钟。
     */
    public MissionApplicationService(MissionRepository missionRepository, Clock clock) {
        this.missionRepository = Objects.requireNonNull(missionRepository, "missionRepository must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    /**
     * 创建草稿 Mission，并保存到仓储。
     */
    public Mission createMission(CreateMissionInput input) {
        Objects.requireNonNull(input, "input must not be null");
        Instant now = clock.instant();
        Mission mission = Mission.draft(nextId("mission"), input.name(), input.scenarioType(),
                input.region(), input.priority(), input.createdBy(), now, input.description());
        return missionRepository.save(mission);
    }

    /**
     * 生成带领域前缀的本地唯一编号。
     */
    private String nextId(String prefix) {
        return prefix + "-" + UUID.randomUUID();
    }
}
