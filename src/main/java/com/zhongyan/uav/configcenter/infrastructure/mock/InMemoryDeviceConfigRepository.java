package com.zhongyan.uav.configcenter.infrastructure.mock;

import com.zhongyan.uav.configcenter.domain.DeviceConfig;
import com.zhongyan.uav.configcenter.domain.DeviceConfigRepository;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class InMemoryDeviceConfigRepository implements DeviceConfigRepository {
    private final Map<String, DeviceConfig> configs = new ConcurrentHashMap<>();

    @Override
    public DeviceConfig save(DeviceConfig config) {
        configs.put(config.deviceId(), config);
        return config;
    }

    @Override
    public Optional<DeviceConfig> findById(String deviceId) {
        return Optional.ofNullable(configs.get(deviceId));
    }

    @Override
    public List<DeviceConfig> findAll() {
        return configs.values().stream()
                .sorted(Comparator.comparing(DeviceConfig::createdAt))
                .collect(Collectors.toList());
    }
}
