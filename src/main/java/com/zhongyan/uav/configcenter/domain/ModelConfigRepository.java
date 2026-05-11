package com.zhongyan.uav.configcenter.domain;

import java.util.List;
import java.util.Optional;

public interface ModelConfigRepository {
    ModelConfigVersion save(ModelConfigVersion config);

    Optional<ModelConfigVersion> findById(String modelConfigId, int version);

    Optional<ModelConfigVersion> findLatestByConfigId(String modelConfigId);

    List<ModelConfigVersion> findByConfigId(String modelConfigId);

    List<ModelConfigVersion> findAllLatest();
}
