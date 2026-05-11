package com.zhongyan.uav.configcenter.infrastructure.jpa;

import com.zhongyan.uav.configcenter.domain.ModelArtifact;
import com.zhongyan.uav.configcenter.domain.ModelArtifactRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
@ConditionalOnProperty(prefix = "bms.repository", name = "mode", havingValue = "jpa")
@Transactional(readOnly = true)
public class JpaModelArtifactRepository implements ModelArtifactRepository {
    private final ModelArtifactJpaDataRepository dataRepository;

    public JpaModelArtifactRepository(ModelArtifactJpaDataRepository dataRepository) {
        this.dataRepository = dataRepository;
    }

    @Override
    @Transactional
    public ModelArtifact save(ModelArtifact artifact) {
        return dataRepository.save(JpaModelArtifactEntity.fromDomain(artifact)).toDomain();
    }

    @Override
    public Optional<ModelArtifact> findById(String artifactId) {
        return dataRepository.findById(artifactId).map(JpaModelArtifactEntity::toDomain);
    }

    @Override
    public List<ModelArtifact> findByModelConfigId(String modelConfigId) {
        return dataRepository.findByModelConfigIdOrderByCreatedAtAsc(modelConfigId).stream()
                .map(JpaModelArtifactEntity::toDomain)
                .collect(Collectors.toList());
    }
}
