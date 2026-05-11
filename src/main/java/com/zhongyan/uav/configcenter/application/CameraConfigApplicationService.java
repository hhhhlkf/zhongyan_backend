package com.zhongyan.uav.configcenter.application;

import com.zhongyan.uav.configcenter.domain.CameraConfigRepository;
import com.zhongyan.uav.configcenter.domain.CameraConfigVersion;

import java.time.Clock;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.UUID;

public class CameraConfigApplicationService {
    private final CameraConfigRepository cameraConfigRepository;
    private final Clock clock;

    public CameraConfigApplicationService(CameraConfigRepository cameraConfigRepository) {
        this(cameraConfigRepository, Clock.systemUTC());
    }

    public CameraConfigApplicationService(CameraConfigRepository cameraConfigRepository, Clock clock) {
        this.cameraConfigRepository = Objects.requireNonNull(cameraConfigRepository, "cameraConfigRepository must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    public CameraConfigVersion createCamera(String cameraType, Map<String, Object> fov,
                                            Map<String, Object> parameters, String createdBy) {
        String cameraConfigId = "camera-config-" + UUID.randomUUID();
        return cameraConfigRepository.save(CameraConfigVersion.create(cameraConfigId, 1, cameraType,
                fov, parameters, createdBy, clock.instant()));
    }

    public List<CameraConfigVersion> listCameras() {
        return cameraConfigRepository.findAllLatest();
    }

    public CameraConfigVersion getCamera(String cameraConfigId) {
        return cameraConfigRepository.findLatestByConfigId(cameraConfigId)
                .orElseThrow(() -> new NoSuchElementException("Camera config not found: " + cameraConfigId));
    }

    public CameraConfigVersion createVersion(String cameraConfigId, Integer version, String cameraType,
                                             Map<String, Object> fov, Map<String, Object> parameters,
                                             String createdBy) {
        CameraConfigVersion latest = getCamera(cameraConfigId);
        int nextVersion = version == null ? latest.version() + 1 : version;
        if (cameraConfigRepository.findById(cameraConfigId, nextVersion).isPresent()) {
            throw new IllegalStateException("Camera config version already exists: " + cameraConfigId + "/" + nextVersion);
        }
        return cameraConfigRepository.save(CameraConfigVersion.create(cameraConfigId, nextVersion,
                defaultText(cameraType, latest.cameraType()), fov, parameters, createdBy, clock.instant()));
    }

    public CameraConfigVersion activateCamera(String cameraConfigId) {
        List<CameraConfigVersion> versions = requireVersions(cameraConfigId);
        CameraConfigVersion latest = versions.stream()
                .max(Comparator.comparingInt(CameraConfigVersion::version))
                .orElseThrow();
        for (CameraConfigVersion version : versions) {
            if (version.version() == latest.version()) {
                cameraConfigRepository.save(version.activate(clock.instant()));
            } else if (version.status().name().equals("ACTIVE")) {
                cameraConfigRepository.save(version.draft(clock.instant()));
            }
        }
        return getCamera(cameraConfigId);
    }

    public void disableCamera(String cameraConfigId) {
        for (CameraConfigVersion version : requireVersions(cameraConfigId)) {
            cameraConfigRepository.save(version.disable(clock.instant()));
        }
    }

    private List<CameraConfigVersion> requireVersions(String cameraConfigId) {
        List<CameraConfigVersion> versions = cameraConfigRepository.findByConfigId(cameraConfigId);
        if (versions.isEmpty()) {
            throw new NoSuchElementException("Camera config not found: " + cameraConfigId);
        }
        return versions;
    }

    private String defaultText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
