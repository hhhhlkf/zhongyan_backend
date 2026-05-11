package com.zhongyan.uav.asset.infrastructure.jpa;

import com.zhongyan.uav.asset.domain.Asset;
import com.zhongyan.uav.asset.domain.AssetRepository;
import com.zhongyan.uav.asset.domain.AssetStatus;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
@ConditionalOnProperty(prefix = "bms.repository", name = "mode", havingValue = "jpa")
@Transactional(readOnly = true)
public class JpaAssetRepository implements AssetRepository {
    private final AssetJpaDataRepository dataRepository;

    public JpaAssetRepository(AssetJpaDataRepository dataRepository) {
        this.dataRepository = dataRepository;
    }

    @Override
    @Transactional
    public Asset save(Asset asset) {
        return dataRepository.save(JpaAssetEntity.fromDomain(asset)).toDomain();
    }

    @Override
    public Optional<Asset> findById(String assetId) {
        return dataRepository.findById(assetId).map(JpaAssetEntity::toDomain);
    }

    @Override
    public List<Asset> findAll() {
        return dataRepository.findAllByOrderByCreatedAtAsc().stream()
                .map(JpaAssetEntity::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Asset> findByMissionId(String missionId) {
        return dataRepository.findByMissionIdOrderByCreatedAtAsc(missionId).stream()
                .map(JpaAssetEntity::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Asset> findByTaskId(String taskId) {
        return dataRepository.findByTaskIdOrderByCreatedAtAsc(taskId).stream()
                .map(JpaAssetEntity::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Asset> findByStatus(AssetStatus status) {
        return dataRepository.findByStatusOrderByCreatedAtAsc(status).stream()
                .map(JpaAssetEntity::toDomain)
                .collect(Collectors.toList());
    }
}
