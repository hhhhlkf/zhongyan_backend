package com.zhongyan.uav.configcenter.infrastructure.mock;

import com.zhongyan.uav.configcenter.domain.CameraConfigRepository;
import com.zhongyan.uav.configcenter.domain.CameraConfigVersion;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class InMemoryCameraConfigRepository implements CameraConfigRepository {
    private final Map<String, CameraConfigVersion> configs = new ConcurrentHashMap<>();

    @Override
    public CameraConfigVersion save(CameraConfigVersion config) {
        configs.put(key(config.cameraConfigId(), config.version()), config);
        return config;
    }

    @Override
    public Optional<CameraConfigVersion> findById(String cameraConfigId, int version) {
        return Optional.ofNullable(configs.get(key(cameraConfigId, version)));
    }

    @Override
    public Optional<CameraConfigVersion> findLatestByConfigId(String cameraConfigId) {
        return findByConfigId(cameraConfigId).stream()
                .max(Comparator.comparingInt(CameraConfigVersion::version));
    }

    @Override
    public List<CameraConfigVersion> findByConfigId(String cameraConfigId) {
        return configs.values().stream()
                .filter(config -> config.cameraConfigId().equals(cameraConfigId))
                .sorted(Comparator.comparingInt(CameraConfigVersion::version))
                .collect(Collectors.toList());
    }

    @Override
    public List<CameraConfigVersion> findAllLatest() {
        return configs.values().stream()
                .collect(Collectors.groupingBy(CameraConfigVersion::cameraConfigId))
                .values().stream()
                .map(versions -> versions.stream()
                        .max(Comparator.comparingInt(CameraConfigVersion::version))
                        .orElseThrow())
                .sorted(Comparator.comparing(CameraConfigVersion::createdAt))
                .collect(Collectors.toList());
    }

    private String key(String cameraConfigId, int version) {
        return cameraConfigId + ":" + version;
    }
}
