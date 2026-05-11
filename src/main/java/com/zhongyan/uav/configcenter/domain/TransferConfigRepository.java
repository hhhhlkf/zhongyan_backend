package com.zhongyan.uav.configcenter.domain;

import java.util.List;
import java.util.Optional;

public interface TransferConfigRepository {
    TransferConfigVersion save(TransferConfigVersion config);

    Optional<TransferConfigVersion> findById(String transferConfigId, int version);

    Optional<TransferConfigVersion> findLatestByConfigId(String transferConfigId);

    List<TransferConfigVersion> findByConfigId(String transferConfigId);

    List<TransferConfigVersion> findAllLatest();
}
