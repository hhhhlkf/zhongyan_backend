package com.zhongyan.uav.configcenter.application;

import com.zhongyan.uav.configcenter.domain.CameraConfigRepository;
import com.zhongyan.uav.configcenter.domain.CameraConfigVersion;
import com.zhongyan.uav.configcenter.domain.ConfigStatus;
import com.zhongyan.uav.configcenter.domain.ConfigValidation;
import com.zhongyan.uav.configcenter.domain.ConfigValidationRepository;
import com.zhongyan.uav.configcenter.domain.DeviceConfig;
import com.zhongyan.uav.configcenter.domain.DeviceConfigRepository;
import com.zhongyan.uav.configcenter.domain.ModelConfigRepository;
import com.zhongyan.uav.configcenter.domain.ModelConfigVersion;
import com.zhongyan.uav.configcenter.domain.TransferConfigRepository;
import com.zhongyan.uav.configcenter.domain.TransferConfigVersion;

import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.UUID;

public class ConfigValidationService {
    private final DeviceConfigRepository deviceConfigRepository;
    private final CameraConfigRepository cameraConfigRepository;
    private final ModelConfigRepository modelConfigRepository;
    private final TransferConfigRepository transferConfigRepository;
    private final ConfigValidationRepository configValidationRepository;
    private final Clock clock;

    public ConfigValidationService(DeviceConfigRepository deviceConfigRepository,
                                   CameraConfigRepository cameraConfigRepository,
                                   ModelConfigRepository modelConfigRepository,
                                   TransferConfigRepository transferConfigRepository,
                                   ConfigValidationRepository configValidationRepository) {
        this(deviceConfigRepository, cameraConfigRepository, modelConfigRepository,
                transferConfigRepository, configValidationRepository, Clock.systemUTC());
    }

    public ConfigValidationService(DeviceConfigRepository deviceConfigRepository,
                                   CameraConfigRepository cameraConfigRepository,
                                   ModelConfigRepository modelConfigRepository,
                                   TransferConfigRepository transferConfigRepository,
                                   ConfigValidationRepository configValidationRepository,
                                   Clock clock) {
        this.deviceConfigRepository = Objects.requireNonNull(deviceConfigRepository, "deviceConfigRepository must not be null");
        this.cameraConfigRepository = Objects.requireNonNull(cameraConfigRepository, "cameraConfigRepository must not be null");
        this.modelConfigRepository = Objects.requireNonNull(modelConfigRepository, "modelConfigRepository must not be null");
        this.transferConfigRepository = Objects.requireNonNull(transferConfigRepository, "transferConfigRepository must not be null");
        this.configValidationRepository = Objects.requireNonNull(configValidationRepository, "configValidationRepository must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    public ConfigValidation validateDevice(String deviceId) {
        DeviceConfig config = deviceConfigRepository.findById(deviceId)
                .orElseThrow(() -> new NoSuchElementException("Device config not found: " + deviceId));
        List<String> errors = new ArrayList<>();
        if (isBlank(config.host())) {
            errors.add("host must not be blank");
        }
        if (config.status() == ConfigStatus.DISABLED) {
            errors.add("device config is disabled");
        }
        return saveValidation("DEVICE", deviceId, errors);
    }

    public ConfigValidation validateCamera(String cameraConfigId) {
        CameraConfigVersion config = cameraConfigRepository.findLatestByConfigId(cameraConfigId)
                .orElseThrow(() -> new NoSuchElementException("Camera config not found: " + cameraConfigId));
        List<String> errors = new ArrayList<>();
        if (isBlank(config.cameraType())) {
            errors.add("cameraType must not be blank");
        }
        if (config.status() == ConfigStatus.DISABLED) {
            errors.add("camera config is disabled");
        }
        return saveValidation("CAMERA", cameraConfigId, errors);
    }

    public ConfigValidation validateModel(String modelConfigId) {
        ModelConfigVersion config = modelConfigRepository.findLatestByConfigId(modelConfigId)
                .orElseThrow(() -> new NoSuchElementException("Model config not found: " + modelConfigId));
        List<String> errors = new ArrayList<>();
        if (isBlank(config.modelType())) {
            errors.add("modelType must not be blank");
        }
        if (isBlank(config.runtimeType())) {
            errors.add("runtimeType must not be blank");
        }
        if (config.status() == ConfigStatus.DISABLED) {
            errors.add("model config is disabled");
        }
        return saveValidation("MODEL", modelConfigId, errors);
    }

    public ConfigValidation validateTransfer(String transferConfigId) {
        TransferConfigVersion config = transferConfigRepository.findLatestByConfigId(transferConfigId)
                .orElseThrow(() -> new NoSuchElementException("Transfer config not found: " + transferConfigId));
        List<String> errors = new ArrayList<>();
        if (isBlank(config.transferType())) {
            errors.add("transferType must not be blank");
        }
        if (config.status() == ConfigStatus.DISABLED) {
            errors.add("transfer config is disabled");
        }
        return saveValidation("TRANSFER", transferConfigId, errors);
    }

    public ConfigValidation getValidation(String validationId) {
        return configValidationRepository.findById(validationId)
                .orElseThrow(() -> new NoSuchElementException("Config validation not found: " + validationId));
    }

    public List<ConfigValidation> listValidations(String configType, String configId) {
        if (isBlank(configType) && isBlank(configId)) {
            return configValidationRepository.findAll();
        }
        if (isBlank(configType) || isBlank(configId)) {
            return List.of();
        }
        return configValidationRepository.findByConfig(configType.toUpperCase(), configId);
    }

    private ConfigValidation saveValidation(String configType, String configId, List<String> errors) {
        String validationId = "validation-" + UUID.randomUUID();
        ConfigValidation validation = errors.isEmpty()
                ? ConfigValidation.passed(validationId, configType, configId, clock.instant())
                : ConfigValidation.failed(validationId, configType, configId, errors, clock.instant());
        return configValidationRepository.save(validation);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
