package com.zhongyan.uav.configcenter.application;

import com.zhongyan.uav.configcenter.domain.DeviceConfig;
import com.zhongyan.uav.configcenter.domain.DeviceConfigRepository;

import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.UUID;

public class DeviceConfigApplicationService {
    private final DeviceConfigRepository deviceConfigRepository;
    private final Clock clock;

    public DeviceConfigApplicationService(DeviceConfigRepository deviceConfigRepository) {
        this(deviceConfigRepository, Clock.systemUTC());
    }

    public DeviceConfigApplicationService(DeviceConfigRepository deviceConfigRepository, Clock clock) {
        this.deviceConfigRepository = Objects.requireNonNull(deviceConfigRepository, "deviceConfigRepository must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    public DeviceConfig createDevice(String deviceId, String deviceName, String deviceType, String host,
                                     Map<String, Object> connection, Map<String, Object> capabilities,
                                     String createdBy) {
        String nextDeviceId = defaultText(deviceId, "device-" + UUID.randomUUID());
        if (deviceConfigRepository.findById(nextDeviceId).isPresent()) {
            throw new IllegalStateException("Device config already exists: " + nextDeviceId);
        }
        return deviceConfigRepository.save(DeviceConfig.create(nextDeviceId, deviceName, deviceType, host,
                connection, capabilities, createdBy, clock.instant()));
    }

    public List<DeviceConfig> listDevices() {
        return deviceConfigRepository.findAll();
    }

    public DeviceConfig getDevice(String deviceId) {
        return deviceConfigRepository.findById(deviceId)
                .orElseThrow(() -> new NoSuchElementException("Device config not found: " + deviceId));
    }

    public DeviceConfig disableDevice(String deviceId) {
        DeviceConfig config = getDevice(deviceId);
        return deviceConfigRepository.save(config.disable(clock.instant()));
    }

    private String defaultText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
