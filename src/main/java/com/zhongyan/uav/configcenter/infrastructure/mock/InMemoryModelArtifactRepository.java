package com.zhongyan.uav.configcenter.infrastructure.mock;

import com.zhongyan.uav.configcenter.domain.ModelArtifact;
import com.zhongyan.uav.configcenter.domain.ModelArtifactRepository;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class InMemoryModelArtifactRepository implements ModelArtifactRepository {
    private final Map<String, ModelArtifact> artifacts = new ConcurrentHashMap<>();

    @Override
    public ModelArtifact save(ModelArtifact artifact) {
        artifacts.put(artifact.artifactId(), artifact);
        return artifact;
    }

    @Override
    public Optional<ModelArtifact> findById(String artifactId) {
        return Optional.ofNullable(artifacts.get(artifactId));
    }

    @Override
    public List<ModelArtifact> findByModelConfigId(String modelConfigId) {
        return artifacts.values().stream()
                .filter(artifact -> artifact.modelConfigId().equals(modelConfigId))
                .sorted(Comparator.comparing(ModelArtifact::createdAt))
                .collect(Collectors.toList());
    }
}
