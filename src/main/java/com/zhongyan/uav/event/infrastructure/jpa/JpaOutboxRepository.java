package com.zhongyan.uav.event.infrastructure.jpa;

import com.zhongyan.uav.event.domain.EventEnvelope;
import com.zhongyan.uav.event.domain.EventType;
import com.zhongyan.uav.event.domain.OutboxStatus;
import com.zhongyan.uav.event.port.OutboxRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
@ConditionalOnProperty(prefix = "bms.repository", name = "mode", havingValue = "jpa")
@Transactional(readOnly = true)
public class JpaOutboxRepository implements OutboxRepository {
    private final OutboxJpaDataRepository dataRepository;

    public JpaOutboxRepository(OutboxJpaDataRepository dataRepository) {
        this.dataRepository = dataRepository;
    }

    @Override
    @Transactional
    public EventEnvelope save(EventEnvelope event) {
        return dataRepository.save(JpaOutboxEntity.fromDomain(event)).toDomain();
    }

    @Override
    public Optional<EventEnvelope> findById(String eventId) {
        return dataRepository.findById(eventId).map(JpaOutboxEntity::toDomain);
    }

    @Override
    public List<EventEnvelope> findByStatus(OutboxStatus status, int limit) {
        return dataRepository.findByStatusOrderByCreatedAtAsc(status).stream()
                .limit(Math.max(limit, 0))
                .map(JpaOutboxEntity::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<EventEnvelope> findByAggregate(String aggregateType, String aggregateId) {
        return dataRepository.findByAggregateTypeAndAggregateIdOrderByCreatedAtAsc(aggregateType, aggregateId)
                .stream()
                .map(JpaOutboxEntity::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<EventEnvelope> findByEventType(EventType eventType, int limit) {
        return dataRepository.findByEventTypeOrderByCreatedAtAsc(eventType).stream()
                .limit(Math.max(limit, 0))
                .map(JpaOutboxEntity::toDomain)
                .collect(Collectors.toList());
    }
}
