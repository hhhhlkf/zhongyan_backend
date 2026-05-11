package com.zhongyan.uav.configcenter.infrastructure.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface TransferConfigJpaDataRepository extends JpaRepository<JpaTransferConfigEntity, JpaTransferConfigId> {
    Optional<JpaTransferConfigEntity> findByTransferConfigIdAndVersion(String transferConfigId, int version);

    Optional<JpaTransferConfigEntity> findTopByTransferConfigIdOrderByVersionDesc(String transferConfigId);

    List<JpaTransferConfigEntity> findByTransferConfigIdOrderByVersionAsc(String transferConfigId);

    @Query("""
            select config
            from JpaTransferConfigEntity config
            where config.version = (
                select max(latest.version)
                from JpaTransferConfigEntity latest
                where latest.transferConfigId = config.transferConfigId
            )
            order by config.createdAt asc
            """)
    List<JpaTransferConfigEntity> findAllLatest();
}
