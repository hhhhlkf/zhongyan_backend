package com.zhongyan.uav.configcenter.infrastructure.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ModelConfigJpaDataRepository extends JpaRepository<JpaModelConfigEntity, JpaModelConfigId> {
    Optional<JpaModelConfigEntity> findByModelConfigIdAndVersion(String modelConfigId, int version);

    Optional<JpaModelConfigEntity> findTopByModelConfigIdOrderByVersionDesc(String modelConfigId);

    List<JpaModelConfigEntity> findByModelConfigIdOrderByVersionAsc(String modelConfigId);

    @Query("""
            select config
            from JpaModelConfigEntity config
            where config.version = (
                select max(latest.version)
                from JpaModelConfigEntity latest
                where latest.modelConfigId = config.modelConfigId
            )
            order by config.createdAt asc
            """)
    List<JpaModelConfigEntity> findAllLatest();
}
