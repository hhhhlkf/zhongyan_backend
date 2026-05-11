package com.zhongyan.uav.mission.application;

import com.zhongyan.uav.mission.domain.Mission;
import com.zhongyan.uav.mission.domain.MissionRepository;
import com.zhongyan.uav.mission.domain.MissionStatus;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;

public class MissionQueryService {
    private final MissionRepository missionRepository;

    /**
     * 创建 Mission 查询服务。
     */
    public MissionQueryService(MissionRepository missionRepository) {
        this.missionRepository = Objects.requireNonNull(missionRepository, "missionRepository must not be null");
    }

    /**
     * 按 Mission 编号查询，找不到时抛出异常。
     */
    public Mission getMission(String missionId) {
        return missionRepository.findById(missionId)
                .orElseThrow(() -> new NoSuchElementException("Mission not found: " + missionId));
    }

    /**
     * 按状态查询 Mission 列表。
     */
    public List<Mission> listByStatus(MissionStatus status) {
        return missionRepository.findByStatus(status);
    }
}
