package com.zhongyan.uav.mission.domain;

import java.util.List;
import java.util.Optional;

public interface MissionRepository {
    /**
     * 保存 Mission 聚合，具体持久化方式由基础设施层实现。
     */
    Mission save(Mission mission);

    /**
     * 按 Mission 编号查找聚合。
     */
    Optional<Mission> findById(String missionId);

    /**
     * 按 Mission 状态查询列表，用于调度或后台管理。
     */
    List<Mission> findByStatus(MissionStatus status);

    /**
     * 判断 Mission 是否存在，避免重复创建或引用不存在的 Mission。
     */
    boolean existsById(String missionId);
}
