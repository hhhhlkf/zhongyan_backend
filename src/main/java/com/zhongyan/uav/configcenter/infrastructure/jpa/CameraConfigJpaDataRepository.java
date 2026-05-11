package com.zhongyan.uav.configcenter.infrastructure.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface CameraConfigJpaDataRepository extends JpaRepository<JpaCameraConfigEntity, JpaCameraConfigId> {
    Optional<JpaCameraConfigEntity> findByCameraConfigIdAndVersion(String cameraConfigId, int version);

    Optional<JpaCameraConfigEntity> findTopByCameraConfigIdOrderByVersionDesc(String cameraConfigId);

    List<JpaCameraConfigEntity> findByCameraConfigIdOrderByVersionAsc(String cameraConfigId);

    @Query("""
            select config
            from JpaCameraConfigEntity config
            where config.version = (
                select max(latest.version)
                from JpaCameraConfigEntity latest
                where latest.cameraConfigId = config.cameraConfigId
            )
            order by config.createdAt asc
            """)
    List<JpaCameraConfigEntity> findAllLatest();
}
