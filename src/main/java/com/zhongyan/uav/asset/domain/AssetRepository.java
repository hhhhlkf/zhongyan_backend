package com.zhongyan.uav.asset.domain;

import java.util.List;
import java.util.Optional;

public interface AssetRepository {
    /**
     * 保存 Asset 元数据，真实文件内容不在领域仓储中保存。
     */
    Asset save(Asset asset);

    /**
     * 按资产编号查找 Asset。
     */
    Optional<Asset> findById(String assetId);

    List<Asset> findAll();

    /**
     * 查询某个 Mission 下的全部资产。
     */
    List<Asset> findByMissionId(String missionId);

    /**
     * 查询某个 Task 产出或绑定的全部资产。
     */
    List<Asset> findByTaskId(String taskId);

    /**
     * 按资产状态查询列表。
     */
    List<Asset> findByStatus(AssetStatus status);
}
