package com.zhongyan.uav.configcenter.infrastructure.jpa;

import com.zhongyan.uav.configcenter.domain.TransferConfigRepository;
import com.zhongyan.uav.configcenter.domain.TransferConfigVersion;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
@ConditionalOnProperty(prefix = "bms.repository", name = "mode", havingValue = "jpa")
@Transactional(readOnly = true)
public class JpaTransferConfigRepository implements TransferConfigRepository {
    private final TransferConfigJpaDataRepository dataRepository;

    public JpaTransferConfigRepository(TransferConfigJpaDataRepository dataRepository) {
        this.dataRepository = dataRepository;
    }

    @Override
    @Transactional
    public TransferConfigVersion save(TransferConfigVersion config) {
        return dataRepository.save(JpaTransferConfigEntity.fromDomain(config)).toDomain();
    }

    @Override
    public Optional<TransferConfigVersion> findById(String transferConfigId, int version) {
        return dataRepository.findByTransferConfigIdAndVersion(transferConfigId, version)
                .map(JpaTransferConfigEntity::toDomain);
    }

    @Override
    public Optional<TransferConfigVersion> findLatestByConfigId(String transferConfigId) {
        return dataRepository.findTopByTransferConfigIdOrderByVersionDesc(transferConfigId)
                .map(JpaTransferConfigEntity::toDomain);
    }

    @Override
    public List<TransferConfigVersion> findByConfigId(String transferConfigId) {
        return dataRepository.findByTransferConfigIdOrderByVersionAsc(transferConfigId).stream()
                .map(JpaTransferConfigEntity::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<TransferConfigVersion> findAllLatest() {
        return dataRepository.findAllLatest().stream()
                .map(JpaTransferConfigEntity::toDomain)
                .collect(Collectors.toList());
    }
}
