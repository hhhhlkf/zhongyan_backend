package com.zhongyan.uav.agent.infrastructure;

import com.zhongyan.uav.agent.domain.AgentSession;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;

@Entity
@Table(name = "agent_session")
public class JpaAgentSessionEntity {
    @Id
    @Column(name = "session_id", length = 120)
    private String sessionId;

    @Column(name = "mission_id", length = 80)
    private String missionId;

    @Column(name = "task_id", length = 80)
    private String taskId;

    @Column(name = "status", nullable = false, length = 32)
    private String status;

    @Column(name = "created_by", nullable = false, length = 120)
    private String createdBy;

    @Column(name = "user_id", nullable = false, length = 128)
    private String userId;

    @Column(name = "title")
    private String title;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> metadata = Map.of();

    protected JpaAgentSessionEntity() {
    }

    public static JpaAgentSessionEntity fromDomain(AgentSession session) {
        JpaAgentSessionEntity entity = new JpaAgentSessionEntity();
        entity.sessionId = session.sessionId();
        entity.missionId = session.missionId();
        entity.taskId = session.taskId();
        entity.status = session.status();
        entity.createdBy = session.userId();
        entity.userId = session.userId();
        entity.title = session.title();
        entity.createdAt = session.createdAt();
        entity.updatedAt = session.updatedAt();
        entity.metadata = Map.of();
        return entity;
    }

    public AgentSession toDomain() {
        return new AgentSession(sessionId, missionId, taskId, userId, title, status, createdAt, updatedAt);
    }
}
