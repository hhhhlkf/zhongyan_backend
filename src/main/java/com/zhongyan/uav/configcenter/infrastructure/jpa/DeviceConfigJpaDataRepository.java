package com.zhongyan.uav.configcenter.infrastructure.jpa;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DeviceConfigJpaDataRepository extends JpaRepository<JpaDeviceConfigEntity, String> {
    List<JpaDeviceConfigEntity> findAllByOrderByCreatedAtAsc();
}
