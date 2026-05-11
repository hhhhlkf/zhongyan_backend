package com.zhongyan.uav.configcenter.domain;

import java.util.List;
import java.util.Optional;

public interface ModelArtifactRepository {
    ModelArtifact save(ModelArtifact artifact);

    Optional<ModelArtifact> findById(String artifactId);

    List<ModelArtifact> findByModelConfigId(String modelConfigId);
}
