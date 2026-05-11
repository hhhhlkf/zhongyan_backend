package com.zhongyan.uav.configcenter.infrastructure.jpa;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ConfigValidationJpaDataRepository extends JpaRepository<JpaConfigValidationEntity, String> {
    List<JpaConfigValidationEntity> findByConfigTypeAndConfigIdOrderByCreatedAtAsc(String configType, String configId);

    List<JpaConfigValidationEntity> findAllByOrderByCreatedAtAsc();
}
