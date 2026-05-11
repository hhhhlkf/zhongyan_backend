package com.zhongyan.uav.configcenter.application;

import com.zhongyan.uav.configcenter.domain.ModelArtifact;
import com.zhongyan.uav.configcenter.domain.ModelArtifactRepository;
import com.zhongyan.uav.configcenter.domain.ModelConfigRepository;
import com.zhongyan.uav.configcenter.domain.ModelConfigVersion;

import java.time.Clock;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.UUID;

public class ModelConfigApplicationService {
    private final ModelConfigRepository modelConfigRepository;
    private final ModelArtifactRepository modelArtifactRepository;
    private final Clock clock;

    public ModelConfigApplicationService(ModelConfigRepository modelConfigRepository,
                                         ModelArtifactRepository modelArtifactRepository) {
        this(modelConfigRepository, modelArtifactRepository, Clock.systemUTC());
    }

    public ModelConfigApplicationService(ModelConfigRepository modelConfigRepository,
                                         ModelArtifactRepository modelArtifactRepository,
                                         Clock clock) {
        this.modelConfigRepository = Objects.requireNonNull(modelConfigRepository, "modelConfigRepository must not be null");
        this.modelArtifactRepository = Objects.requireNonNull(modelArtifactRepository, "modelArtifactRepository must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    public ModelConfigVersion createModel(String modelType, String runtimeType,
                                          Map<String, Object> parameters, String createdBy) {
        String modelConfigId = "model-config-" + UUID.randomUUID();
        return modelConfigRepository.save(ModelConfigVersion.create(modelConfigId, 1, modelType,
                runtimeType, parameters, createdBy, clock.instant()));
    }

    public List<ModelConfigVersion> listModels() {
        return modelConfigRepository.findAllLatest();
    }

    public ModelConfigVersion getModel(String modelConfigId) {
        return modelConfigRepository.findLatestByConfigId(modelConfigId)
                .orElseThrow(() -> new NoSuchElementException("Model config not found: " + modelConfigId));
    }

    public ModelConfigVersion createVersion(String modelConfigId, Integer version, String modelType,
                                            String runtimeType, Map<String, Object> parameters,
                                            String createdBy) {
        ModelConfigVersion latest = getModel(modelConfigId);
        int nextVersion = version == null ? latest.version() + 1 : version;
        if (modelConfigRepository.findById(modelConfigId, nextVersion).isPresent()) {
            throw new IllegalStateException("Model config version already exists: " + modelConfigId + "/" + nextVersion);
        }
        return modelConfigRepository.save(ModelConfigVersion.create(modelConfigId, nextVersion,
                defaultText(modelType, latest.modelType()), defaultText(runtimeType, latest.runtimeType()),
                parameters, createdBy, clock.instant()));
    }

    public ModelArtifact createArtifact(String modelConfigId, String artifactType, String objectKey,
                                        String checksum, Map<String, Object> metadata, String createdBy) {
        getModel(modelConfigId);
        return modelArtifactRepository.save(ModelArtifact.create("artifact-" + UUID.randomUUID(), modelConfigId,
                artifactType, objectKey, checksum, metadata, createdBy, clock.instant()));
    }

    public ModelConfigVersion activateModel(String modelConfigId) {
        List<ModelConfigVersion> versions = requireVersions(modelConfigId);
        ModelConfigVersion latest = versions.stream()
                .max(Comparator.comparingInt(ModelConfigVersion::version))
                .orElseThrow();
        for (ModelConfigVersion version : versions) {
            if (version.version() == latest.version()) {
                modelConfigRepository.save(version.activate(clock.instant()));
            } else if (version.status().name().equals("ACTIVE")) {
                modelConfigRepository.save(version.draft(clock.instant()));
            }
        }
        return getModel(modelConfigId);
    }

    public void disableModel(String modelConfigId) {
        for (ModelConfigVersion version : requireVersions(modelConfigId)) {
            modelConfigRepository.save(version.disable(clock.instant()));
        }
    }

    private List<ModelConfigVersion> requireVersions(String modelConfigId) {
        List<ModelConfigVersion> versions = modelConfigRepository.findByConfigId(modelConfigId);
        if (versions.isEmpty()) {
            throw new NoSuchElementException("Model config not found: " + modelConfigId);
        }
        return versions;
    }

    private String defaultText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
