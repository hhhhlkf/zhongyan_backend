package com.zhongyan.uav.mission.infrastructure.mock;

import com.zhongyan.uav.mission.domain.Mission;
import com.zhongyan.uav.mission.domain.MissionRepository;
import com.zhongyan.uav.mission.domain.MissionStatus;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class InMemoryMissionRepository implements MissionRepository {
    private final Map<String, Mission> missions = new ConcurrentHashMap<>();

    /**
     * 将 Mission 保存到内存 Map。
     */
    @Override
    public Mission save(Mission mission) {
        missions.put(mission.missionId(), mission);
        return mission;
    }

    /**
     * 从内存中按编号查找 Mission。
     */
    @Override
    public Optional<Mission> findById(String missionId) {
        return Optional.ofNullable(missions.get(missionId));
    }

    /**
     * 从内存中按状态筛选 Mission，并按创建时间排序。
     */
    @Override
    public List<Mission> findByStatus(MissionStatus status) {
        return missions.values().stream()
                .filter(mission -> mission.status() == status)
                .sorted(Comparator.comparing(Mission::createdAt))
                .collect(Collectors.toList());
    }

    /**
     * 判断内存仓储中是否存在指定 Mission。
     */
    @Override
    public boolean existsById(String missionId) {
        return missions.containsKey(missionId);
    }
}
