package com.zhongyan.uav.configcenter.infrastructure.mock;

import com.zhongyan.uav.configcenter.domain.TransferConfigRepository;
import com.zhongyan.uav.configcenter.domain.TransferConfigVersion;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class InMemoryTransferConfigRepository implements TransferConfigRepository {
    private final Map<String, TransferConfigVersion> configs = new ConcurrentHashMap<>();

    @Override
    public TransferConfigVersion save(TransferConfigVersion config) {
        configs.put(key(config.transferConfigId(), config.version()), config);
        return config;
    }

    @Override
    public Optional<TransferConfigVersion> findById(String transferConfigId, int version) {
        return Optional.ofNullable(configs.get(key(transferConfigId, version)));
    }

    @Override
    public Optional<TransferConfigVersion> findLatestByConfigId(String transferConfigId) {
        return findByConfigId(transferConfigId).stream()
                .max(Comparator.comparingInt(TransferConfigVersion::version));
    }

    @Override
    public List<TransferConfigVersion> findByConfigId(String transferConfigId) {
        return configs.values().stream()
                .filter(config -> config.transferConfigId().equals(transferConfigId))
                .sorted(Comparator.comparingInt(TransferConfigVersion::version))
                .collect(Collectors.toList());
    }

    @Override
    public List<TransferConfigVersion> findAllLatest() {
        return configs.values().stream()
                .collect(Collectors.groupingBy(TransferConfigVersion::transferConfigId))
                .values().stream()
                .map(versions -> versions.stream()
                        .max(Comparator.comparingInt(TransferConfigVersion::version))
                        .orElseThrow())
                .sorted(Comparator.comparing(TransferConfigVersion::createdAt))
                .collect(Collectors.toList());
    }

    private String key(String transferConfigId, int version) {
        return transferConfigId + ":" + version;
    }
}
