package com.zhongyan.uav.agent.application;

import com.zhongyan.uav.agent.port.EmbeddingPort;
import com.zhongyan.uav.agent.port.RagDocument;
import com.zhongyan.uav.agent.port.VectorStorePort;

import java.time.Clock;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Agent RAG 应用服务。
 * <p>
 * 负责把任务摘要、时间线、设备文档、任务日志和报告材料写入知识库，
 * 并通过向量存储端口检索带来源信息的上下文材料。
 */
public class RagService {
    private static final int DEFAULT_LIMIT = 10;
    private static final int MAX_LIMIT = 50;
    private static final int SNIPPET_LENGTH = 220;

    private final EmbeddingPort embeddingPort;
    private final VectorStorePort vectorStorePort;
    private final Clock clock;

    public RagService(EmbeddingPort embeddingPort, VectorStorePort vectorStorePort) {
        this(embeddingPort, vectorStorePort, Clock.systemUTC());
    }

    public RagService(EmbeddingPort embeddingPort, VectorStorePort vectorStorePort, Clock clock) {
        this.embeddingPort = embeddingPort;
        this.vectorStorePort = vectorStorePort;
        this.clock = clock;
    }

    /**
     * 写入一条通用 RAG 文档。应用层统一补齐摘要、向量维度和索引时间，底层端口只负责存储。
     */
    public RagDocument index(RagDocument document) {
        RagDocument normalized = normalize(document);
        List<Double> embedding = embeddingPort.embed(normalized.content());
        Map<String, Object> metadata = new LinkedHashMap<>(normalized.metadata());
        metadata.put("embeddingDimension", embedding.size());
        metadata.put("indexedAt", clock.instant().toString());
        metadata.putIfAbsent("snippet", normalized.snippet());
        RagDocument enriched = new RagDocument(normalized.documentId(), normalized.sourceType(),
                normalized.sourceId(), normalized.title(), normalized.content(), normalized.snippet(), metadata);
        vectorStorePort.upsert(enriched, embedding);
        return enriched;
    }

    public List<RagDocument> search(String query, int limit, Map<String, Object> filters) {
        int effectiveLimit = limit <= 0 ? DEFAULT_LIMIT : Math.min(limit, MAX_LIMIT);
        String effectiveQuery = defaultText(query, "");
        List<Double> queryEmbedding = embeddingPort.embed(effectiveQuery);
        return vectorStorePort.search(effectiveQuery, effectiveLimit, sanitizeFilters(filters), queryEmbedding).stream()
                .limit(effectiveLimit)
                .collect(Collectors.toList());
    }

    public RagDocument indexMissionSummary(String missionId, String summary, Map<String, Object> metadata) {
        Map<String, Object> merged = merge(metadata, Map.of("missionId", requireText(missionId, "missionId")));
        return index(new RagDocument("rag-mission-summary-" + stableId(missionId), "MISSION_SUMMARY",
                missionId, "Mission " + missionId + " summary", summary, null, merged));
    }

    public RagDocument indexTaskTimeline(String missionId, String taskId, List<String> timeline,
                                         Map<String, Object> metadata) {
        String content = safeList(timeline).isEmpty()
                ? "Task timeline is empty."
                : String.join("\n", safeList(timeline));
        Map<String, Object> merged = merge(metadata, Map.of(
                "missionId", defaultText(missionId, ""),
                "taskId", requireText(taskId, "taskId")));
        return index(new RagDocument("rag-task-timeline-" + stableId(taskId), "TASK_TIMELINE",
                taskId, "Task " + taskId + " timeline", content, null, merged));
    }

    public RagDocument indexDeviceDocument(String deviceId, String title, String content,
                                           Map<String, Object> metadata) {
        Map<String, Object> merged = merge(metadata, Map.of("deviceId", requireText(deviceId, "deviceId")));
        return index(new RagDocument("rag-device-" + stableId(deviceId + "-" + defaultText(title, "")),
                "DEVICE_DOCUMENT", deviceId, defaultText(title, "Device " + deviceId), content, null, merged));
    }

    public RagDocument indexTaskLog(String taskId, String attemptId, String title, String content,
                                    Map<String, Object> metadata) {
        Map<String, Object> merged = merge(metadata, Map.of(
                "taskId", requireText(taskId, "taskId"),
                "attemptId", defaultText(attemptId, "")));
        String sourceId = defaultText(attemptId, taskId);
        return index(new RagDocument("rag-task-log-" + stableId(taskId + "-" + sourceId),
                "TASK_LOG", sourceId, defaultText(title, "Task " + taskId + " log"), content, null, merged));
    }

    public RagDocument indexReportMaterial(String sourceId, String title, String content,
                                           Map<String, Object> metadata) {
        return index(new RagDocument("rag-report-material-" + stableId(sourceId), "REPORT_MATERIAL",
                requireText(sourceId, "sourceId"), defaultText(title, "Report material"), content, null,
                metadata));
    }

    public List<RagDocument> searchMissionContext(String missionId, String query, int limit) {
        return search(query, limit, Map.of("missionId", requireText(missionId, "missionId")));
    }

    private RagDocument normalize(RagDocument document) {
        String content = requireText(document.content(), "content");
        String sourceType = requireText(document.sourceType(), "sourceType").toUpperCase(Locale.ROOT);
        String documentId = defaultText(document.documentId(), "rag-" + UUID.randomUUID());
        String title = defaultText(document.title(), sourceType + " " + defaultText(document.sourceId(), documentId));
        String snippet = defaultText(document.snippet(), snippet(content));
        return new RagDocument(documentId, sourceType, document.sourceId(), title, content, snippet,
                document.metadata());
    }

    private Map<String, Object> sanitizeFilters(Map<String, Object> filters) {
        if (filters == null || filters.isEmpty()) {
            return Map.of();
        }
        Map<String, Object> sanitized = new LinkedHashMap<>();
        filters.forEach((key, value) -> {
            if (key != null && value != null && !(value instanceof String text && text.isBlank())) {
                sanitized.put(key, value);
            }
        });
        return sanitized;
    }

    private Map<String, Object> merge(Map<String, Object> metadata, Map<String, Object> required) {
        Map<String, Object> merged = new LinkedHashMap<>();
        if (metadata != null) {
            merged.putAll(sanitizeFilters(metadata));
        }
        merged.putAll(required);
        return merged;
    }

    private List<String> safeList(List<String> values) {
        return values == null ? List.of() : values.stream()
                .filter(value -> value != null && !value.isBlank())
                .collect(Collectors.toList());
    }

    private static String snippet(String content) {
        String compact = content.replaceAll("\\s+", " ").trim();
        if (compact.length() <= SNIPPET_LENGTH) {
            return compact;
        }
        return compact.substring(0, SNIPPET_LENGTH) + "...";
    }

    private static String stableId(String value) {
        return requireText(value, "id").replaceAll("[^A-Za-z0-9._-]", "-");
    }

    private static String requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
    }

    private static String defaultText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
