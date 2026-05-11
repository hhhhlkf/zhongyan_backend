package com.zhongyan.uav.configcenter.domain;

import java.util.List;
import java.util.Optional;

public interface CameraConfigRepository {
    CameraConfigVersion save(CameraConfigVersion config);

    Optional<CameraConfigVersion> findById(String cameraConfigId, int version);

    Optional<CameraConfigVersion> findLatestByConfigId(String cameraConfigId);

    List<CameraConfigVersion> findByConfigId(String cameraConfigId);

    List<CameraConfigVersion> findAllLatest();
}
