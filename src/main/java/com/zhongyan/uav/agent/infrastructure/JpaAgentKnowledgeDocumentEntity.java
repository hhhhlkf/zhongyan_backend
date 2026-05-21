package com.zhongyan.uav.agent.infrastructure;

import com.zhongyan.uav.agent.port.RagDocument;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;

@Entity
@Table(name = "agent_knowledge_document")
public class JpaAgentKnowledgeDocumentEntity {
    @Id
    @Column(name = "document_id", length = 64)
    private String documentId;

    @Column(name = "source_type", nullable = false, length = 64)
    private String sourceType;

    @Column(name = "source_id", length = 128)
    private String sourceId;

    @Column(name = "title")
    private String title;

    @Column(name = "content", nullable = false)
    private String content;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> metadata = Map.of();

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected JpaAgentKnowledgeDocumentEntity() {
    }

    public static JpaAgentKnowledgeDocumentEntity fromRagDocument(RagDocument document, Instant now) {
        JpaAgentKnowledgeDocumentEntity entity = new JpaAgentKnowledgeDocumentEntity();
        entity.documentId = document.documentId();
        entity.sourceType = document.sourceType();
        entity.sourceId = document.sourceId();
        entity.title = document.title();
        entity.content = document.content();
        entity.metadata = document.metadata();
        entity.createdAt = now;
        entity.updatedAt = now;
        return entity;
    }

    public RagDocument toRagDocument() {
        Object snippet = metadata == null ? null : metadata.get("snippet");
        return new RagDocument(documentId, sourceType, sourceId, title, content,
                snippet == null ? content : String.valueOf(snippet), metadata);
    }
}
