package com.zhongyan.uav.configcenter.domain;

import java.util.List;
import java.util.Optional;

public interface DeviceConfigRepository {
    DeviceConfig save(DeviceConfig config);

    Optional<DeviceConfig> findById(String deviceId);

    List<DeviceConfig> findAll();
}
