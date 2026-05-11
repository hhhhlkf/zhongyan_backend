package com.zhongyan.uav.asset.infrastructure.jpa;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TaskAssetJpaDataRepository extends JpaRepository<JpaTaskAssetEntity, JpaTaskAssetId> {
    List<JpaTaskAssetEntity> findByTaskIdOrderByBoundAtAsc(String taskId);

    List<JpaTaskAssetEntity> findByAssetIdOrderByBoundAtAsc(String assetId);
}
