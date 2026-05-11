package com.zhongyan.uav.configcenter.infrastructure.mock;

import com.zhongyan.uav.configcenter.domain.ConfigValidation;
import com.zhongyan.uav.configcenter.domain.ConfigValidationRepository;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class InMemoryConfigValidationRepository implements ConfigValidationRepository {
    private final Map<String, ConfigValidation> validations = new ConcurrentHashMap<>();

    @Override
    public ConfigValidation save(ConfigValidation validation) {
        validations.put(validation.validationId(), validation);
        return validation;
    }

    @Override
    public Optional<ConfigValidation> findById(String validationId) {
        return Optional.ofNullable(validations.get(validationId));
    }

    @Override
    public List<ConfigValidation> findByConfig(String configType, String configId) {
        return validations.values().stream()
                .filter(validation -> validation.configType().equals(configType))
                .filter(validation -> validation.configId().equals(configId))
                .sorted(Comparator.comparing(ConfigValidation::createdAt))
                .collect(Collectors.toList());
    }

    @Override
    public List<ConfigValidation> findAll() {
        return validations.values().stream()
                .sorted(Comparator.comparing(ConfigValidation::createdAt))
                .collect(Collectors.toList());
    }
}
