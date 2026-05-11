package com.zhongyan.uav.configcenter.infrastructure.mock;

import com.zhongyan.uav.configcenter.domain.ModelConfigRepository;
import com.zhongyan.uav.configcenter.domain.ModelConfigVersion;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class InMemoryModelConfigRepository implements ModelConfigRepository {
    private final Map<String, ModelConfigVersion> configs = new ConcurrentHashMap<>();

    @Override
    public ModelConfigVersion save(ModelConfigVersion config) {
        configs.put(key(config.modelConfigId(), config.version()), config);
        return config;
    }

    @Override
    public Optional<ModelConfigVersion> findById(String modelConfigId, int version) {
        return Optional.ofNullable(configs.get(key(modelConfigId, version)));
    }

    @Override
    public Optional<ModelConfigVersion> findLatestByConfigId(String modelConfigId) {
        return findByConfigId(modelConfigId).stream()
                .max(Comparator.comparingInt(ModelConfigVersion::version));
    }

    @Override
    public List<ModelConfigVersion> findByConfigId(String modelConfigId) {
        return configs.values().stream()
                .filter(config -> config.modelConfigId().equals(modelConfigId))
                .sorted(Comparator.comparingInt(ModelConfigVersion::version))
                .collect(Collectors.toList());
    }

    @Override
    public List<ModelConfigVersion> findAllLatest() {
        return configs.values().stream()
                .collect(Collectors.groupingBy(ModelConfigVersion::modelConfigId))
                .values().stream()
                .map(versions -> versions.stream()
                        .max(Comparator.comparingInt(ModelConfigVersion::version))
                        .orElseThrow())
                .sorted(Comparator.comparing(ModelConfigVersion::createdAt))
                .collect(Collectors.toList());
    }

    private String key(String modelConfigId, int version) {
        return modelConfigId + ":" + version;
    }
}
