package com.zhongyan.uav.event.infrastructure.jpa;

import com.zhongyan.uav.event.domain.OutboxStatus;
import com.zhongyan.uav.event.domain.EventType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OutboxJpaDataRepository extends JpaRepository<JpaOutboxEntity, String> {
    List<JpaOutboxEntity> findByStatusOrderByCreatedAtAsc(OutboxStatus status);

    List<JpaOutboxEntity> findByAggregateTypeAndAggregateIdOrderByCreatedAtAsc(String aggregateType,
                                                                               String aggregateId);

    List<JpaOutboxEntity> findByEventTypeOrderByCreatedAtAsc(EventType eventType);
}
