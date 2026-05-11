package com.zhongyan.uav.asset.domain;

import java.util.List;

public interface TaskAssetRepository {
    /**
     * 保存 Task 与 Asset 的绑定关系。
     */
    TaskAsset save(TaskAsset taskAsset);

    /**
     * 查询某个 Task 绑定的全部资产关系。
     */
    List<TaskAsset> findByTaskId(String taskId);

    /**
     * 查询某个 Asset 被哪些 Task 引用。
     */
    List<TaskAsset> findByAssetId(String assetId);
}
