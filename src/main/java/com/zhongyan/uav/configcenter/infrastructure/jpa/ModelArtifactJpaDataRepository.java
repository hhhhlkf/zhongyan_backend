package com.zhongyan.uav.configcenter.infrastructure.jpa;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ModelArtifactJpaDataRepository extends JpaRepository<JpaModelArtifactEntity, String> {
    List<JpaModelArtifactEntity> findByModelConfigIdOrderByCreatedAtAsc(String modelConfigId);
}
