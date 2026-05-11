package com.zhongyan.uav.configcenter.application;

import com.zhongyan.uav.configcenter.domain.TransferConfigRepository;
import com.zhongyan.uav.configcenter.domain.TransferConfigVersion;

import java.time.Clock;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.UUID;

public class TransferConfigApplicationService {
    private final TransferConfigRepository transferConfigRepository;
    private final Clock clock;

    public TransferConfigApplicationService(TransferConfigRepository transferConfigRepository) {
        this(transferConfigRepository, Clock.systemUTC());
    }

    public TransferConfigApplicationService(TransferConfigRepository transferConfigRepository, Clock clock) {
        this.transferConfigRepository = Objects.requireNonNull(transferConfigRepository, "transferConfigRepository must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    public TransferConfigVersion createTransfer(String transferType, Map<String, Object> endpoint,
                                                Map<String, Object> parameters, String createdBy) {
        String transferConfigId = "transfer-config-" + UUID.randomUUID();
        return transferConfigRepository.save(TransferConfigVersion.create(transferConfigId, 1,
                transferType, endpoint, parameters, createdBy, clock.instant()));
    }

    public List<TransferConfigVersion> listTransfers() {
        return transferConfigRepository.findAllLatest();
    }

    public TransferConfigVersion getTransfer(String transferConfigId) {
        return transferConfigRepository.findLatestByConfigId(transferConfigId)
                .orElseThrow(() -> new NoSuchElementException("Transfer config not found: " + transferConfigId));
    }

    public TransferConfigVersion createVersion(String transferConfigId, Integer version, String transferType,
                                               Map<String, Object> endpoint, Map<String, Object> parameters,
                                               String createdBy) {
        TransferConfigVersion latest = getTransfer(transferConfigId);
        int nextVersion = version == null ? latest.version() + 1 : version;
        if (transferConfigRepository.findById(transferConfigId, nextVersion).isPresent()) {
            throw new IllegalStateException("Transfer config version already exists: " + transferConfigId + "/" + nextVersion);
        }
        return transferConfigRepository.save(TransferConfigVersion.create(transferConfigId, nextVersion,
                defaultText(transferType, latest.transferType()), endpoint, parameters, createdBy, clock.instant()));
    }

    public TransferConfigVersion activateTransfer(String transferConfigId) {
        List<TransferConfigVersion> versions = requireVersions(transferConfigId);
        TransferConfigVersion latest = versions.stream()
                .max(Comparator.comparingInt(TransferConfigVersion::version))
                .orElseThrow();
        for (TransferConfigVersion version : versions) {
            if (version.version() == latest.version()) {
                transferConfigRepository.save(version.activate(clock.instant()));
            } else if (version.status().name().equals("ACTIVE")) {
                transferConfigRepository.save(version.draft(clock.instant()));
            }
        }
        return getTransfer(transferConfigId);
    }

    public void disableTransfer(String transferConfigId) {
        for (TransferConfigVersion version : requireVersions(transferConfigId)) {
            transferConfigRepository.save(version.disable(clock.instant()));
        }
    }

    private List<TransferConfigVersion> requireVersions(String transferConfigId) {
        List<TransferConfigVersion> versions = transferConfigRepository.findByConfigId(transferConfigId);
        if (versions.isEmpty()) {
            throw new NoSuchElementException("Transfer config not found: " + transferConfigId);
        }
        return versions;
    }

    private String defaultText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
