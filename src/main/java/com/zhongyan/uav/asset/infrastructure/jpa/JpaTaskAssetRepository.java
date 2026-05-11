package com.zhongyan.uav.asset.infrastructure.jpa;

import com.zhongyan.uav.asset.domain.TaskAsset;
import com.zhongyan.uav.asset.domain.TaskAssetRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Repository
@ConditionalOnProperty(prefix = "bms.repository", name = "mode", havingValue = "jpa")
@Transactional(readOnly = true)
public class JpaTaskAssetRepository implements TaskAssetRepository {
    private final TaskAssetJpaDataRepository dataRepository;

    public JpaTaskAssetRepository(TaskAssetJpaDataRepository dataRepository) {
        this.dataRepository = dataRepository;
    }

    @Override
    @Transactional
    public TaskAsset save(TaskAsset taskAsset) {
        return dataRepository.save(JpaTaskAssetEntity.fromDomain(taskAsset)).toDomain();
    }

    @Override
    public List<TaskAsset> findByTaskId(String taskId) {
        return dataRepository.findByTaskIdOrderByBoundAtAsc(taskId).stream()
                .map(JpaTaskAssetEntity::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<TaskAsset> findByAssetId(String assetId) {
        return dataRepository.findByAssetIdOrderByBoundAtAsc(assetId).stream()
                .map(JpaTaskAssetEntity::toDomain)
                .collect(Collectors.toList());
    }
}
