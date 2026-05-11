package com.zhongyan.uav.asset.infrastructure.jpa;

import com.zhongyan.uav.asset.domain.AssetStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AssetJpaDataRepository extends JpaRepository<JpaAssetEntity, String> {
    List<JpaAssetEntity> findAllByOrderByCreatedAtAsc();

    List<JpaAssetEntity> findByMissionIdOrderByCreatedAtAsc(String missionId);

    List<JpaAssetEntity> findByTaskIdOrderByCreatedAtAsc(String taskId);

    List<JpaAssetEntity> findByStatusOrderByCreatedAtAsc(AssetStatus status);
}
