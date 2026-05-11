package com.zhongyan.uav.configcenter.domain;

import java.util.List;
import java.util.Optional;

public interface ConfigValidationRepository {
    ConfigValidation save(ConfigValidation validation);

    Optional<ConfigValidation> findById(String validationId);

    List<ConfigValidation> findByConfig(String configType, String configId);

    List<ConfigValidation> findAll();
}
