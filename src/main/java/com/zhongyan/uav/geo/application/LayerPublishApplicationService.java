package com.zhongyan.uav.geo.application;

import com.zhongyan.uav.asset.domain.Asset;
import com.zhongyan.uav.asset.domain.AssetRepository;
import com.zhongyan.uav.asset.application.AssetEventRecorder;
import com.zhongyan.uav.common.error.BusinessException;
import com.zhongyan.uav.common.error.ErrorCode;
import com.zhongyan.uav.geo.domain.LayerPublishRecord;
import com.zhongyan.uav.geo.domain.LayerStatus;
import com.zhongyan.uav.geo.port.GeoServerPort;

import java.time.Clock;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public class LayerPublishApplicationService {
    private final AssetRepository assetRepository;
    private final GeoServerPort geoServerPort;
    private final AssetEventRecorder assetEventRecorder;
    private final Clock clock;

    public LayerPublishApplicationService(AssetRepository assetRepository,
                                          GeoServerPort geoServerPort) {
        this(assetRepository, geoServerPort, AssetEventRecorder.noop(), Clock.systemUTC());
    }

    public LayerPublishApplicationService(AssetRepository assetRepository,
                                          GeoServerPort geoServerPort,
                                          AssetEventRecorder assetEventRecorder) {
        this(assetRepository, geoServerPort, assetEventRecorder, Clock.systemUTC());
    }

    public LayerPublishApplicationService(AssetRepository assetRepository,
                                          GeoServerPort geoServerPort,
                                          Clock clock) {
        this(assetRepository, geoServerPort, AssetEventRecorder.noop(), clock);
    }

    public LayerPublishApplicationService(AssetRepository assetRepository,
                                          GeoServerPort geoServerPort,
                                          AssetEventRecorder assetEventRecorder,
                                          Clock clock) {
        this.assetRepository = Objects.requireNonNull(assetRepository, "assetRepository must not be null");
        this.geoServerPort = Objects.requireNonNull(geoServerPort, "geoServerPort must not be null");
        this.assetEventRecorder = Objects.requireNonNull(assetEventRecorder, "assetEventRecorder must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    public LayerPublishRecord publishLayer(String assetId, String requestedBy, Map<String, Object> parameters) {
        Map<String, Object> safeParameters = parameters == null ? Map.of() : parameters;
        Asset asset = assetRepository.findById(assetId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "asset not found: " + assetId));
        if (!Boolean.TRUE.equals(safeParameters.get("approved"))) {
            LayerPublishRecord record = new LayerPublishRecord(assetId, assetId + "-layer", asset.layerUrl(),
                    LayerStatus.PENDING_APPROVAL, Map.of("requestedBy", defaultText(requestedBy, "system")), null);
            assetEventRecorder.recordLayerPendingApproval(asset, record);
            return record;
        }
        LayerPublishRecord record = geoServerPort.publishLayer(asset, safeParameters);
        Map<String, Object> metadata = new LinkedHashMap<>(asset.metadata());
        metadata.put("layerId", record.layerId());
        metadata.put("layerPublishedAt", clock.instant().toString());
        metadata.put("layerRequestedBy", defaultText(requestedBy, "system"));
        Asset saved = assetRepository.save(asset.withMetadata(metadata, clock.instant())
                .publishLayer(record.layerUrl(), clock.instant()));
        assetEventRecorder.recordLayerPublished(saved, record);
        return record;
    }

    private String defaultText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
