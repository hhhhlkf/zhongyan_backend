package com.zhongyan.uav.asset.application;

import com.zhongyan.uav.asset.domain.Asset;
import com.zhongyan.uav.event.application.OutboxPublishService;
import com.zhongyan.uav.event.domain.EventType;
import com.zhongyan.uav.geo.domain.LayerPublishRecord;
import com.zhongyan.uav.task.domain.TaskEvent;
import com.zhongyan.uav.task.domain.TaskEventRepository;
import com.zhongyan.uav.task.domain.TaskEventType;

import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class AssetEventRecorder {
    private final OutboxPublishService outboxPublishService;
    private final TaskEventRepository taskEventRepository;
    private final Clock clock;

    public AssetEventRecorder(OutboxPublishService outboxPublishService,
                              TaskEventRepository taskEventRepository) {
        this(outboxPublishService, taskEventRepository, Clock.systemUTC());
    }

    public AssetEventRecorder(OutboxPublishService outboxPublishService,
                              TaskEventRepository taskEventRepository,
                              Clock clock) {
        this.outboxPublishService = Objects.requireNonNull(outboxPublishService,
                "outboxPublishService must not be null");
        this.taskEventRepository = Objects.requireNonNull(taskEventRepository,
                "taskEventRepository must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    private AssetEventRecorder() {
        this.outboxPublishService = null;
        this.taskEventRepository = null;
        this.clock = Clock.systemUTC();
    }

    public static AssetEventRecorder noop() {
        return new AssetEventRecorder();
    }

    public void recordAssetCreated(Asset asset) {
        record(asset, TaskEventType.ASSET_CREATED, "CREATED", Map.of());
    }

    public void recordGeoCalculated(Asset asset) {
        record(asset, TaskEventType.GEO_CALCULATED, "GEO_CALCULATED", Map.of());
    }

    public void recordGeoFailed(Asset asset, String errorMessage) {
        Map<String, Object> details = new LinkedHashMap<>();
        if (errorMessage != null && !errorMessage.isBlank()) {
            details.put("errorMessage", errorMessage);
        }
        record(asset, TaskEventType.GEO_FAILED, "GEO_FAILED", details);
    }

    public void recordLayerPendingApproval(Asset asset, LayerPublishRecord record) {
        record(asset, TaskEventType.LAYER_PENDING_APPROVAL, "LAYER_PENDING_APPROVAL", layerDetails(record));
    }

    public void recordLayerPublished(Asset asset, LayerPublishRecord record) {
        record(asset, TaskEventType.LAYER_PUBLISHED, "LAYER_PUBLISHED", layerDetails(record));
    }

    private void record(Asset asset, TaskEventType taskEventType, String action, Map<String, Object> details) {
        if (outboxPublishService == null || taskEventRepository == null) {
            return;
        }
        Instant now = clock.instant();
        Map<String, Object> payload = payload(asset, action, details);
        outboxPublishService.enqueue("ASSET", asset.assetId(), EventType.ASSET_EVENT, payload,
                Map.of("source", "asset-gis"));
        if (asset.taskId() != null && !asset.taskId().isBlank()) {
            taskEventRepository.save(new TaskEvent("event-" + UUID.randomUUID(), asset.taskId(),
                    taskEventType, null, null, payload, now));
        }
    }

    private Map<String, Object> payload(Asset asset, String action, Map<String, Object> details) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("action", action);
        payload.put("assetId", asset.assetId());
        payload.put("missionId", asset.missionId());
        putIfPresent(payload, "taskId", asset.taskId());
        payload.put("assetType", asset.assetType().name());
        payload.put("assetRole", asset.assetRole().name());
        payload.put("status", asset.status().name());
        payload.put("geoStatus", asset.geoStatus().name());
        payload.put("name", asset.name());
        putIfPresent(payload, "objectKey", asset.objectKey());
        putIfPresent(payload, "previewObjectKey", asset.previewObjectKey());
        putIfPresent(payload, "layerUrl", asset.layerUrl());
        payload.put("metadata", asset.metadata());
        if (details != null) {
            details.forEach((key, value) -> putIfPresent(payload, key, value));
        }
        return payload;
    }

    private Map<String, Object> layerDetails(LayerPublishRecord record) {
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("layerId", record.layerId());
        putIfPresent(details, "layerUrl", record.layerUrl());
        details.put("layerStatus", record.status().name());
        details.put("layerMetadata", record.metadata());
        if (record.publishedAt() != null) {
            details.put("publishedAt", record.publishedAt().toString());
        }
        return details;
    }

    private void putIfPresent(Map<String, Object> target, String key, Object value) {
        if (value != null) {
            target.put(key, value);
        }
    }
}
