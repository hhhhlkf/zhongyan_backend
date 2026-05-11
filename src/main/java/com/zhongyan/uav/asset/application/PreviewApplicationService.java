package com.zhongyan.uav.asset.application;

import com.zhongyan.uav.asset.domain.Asset;
import com.zhongyan.uav.asset.domain.AssetRepository;
import com.zhongyan.uav.asset.port.PreviewGeneratorPort;
import com.zhongyan.uav.asset.port.PreviewRequest;
import com.zhongyan.uav.asset.port.PreviewResult;
import com.zhongyan.uav.common.error.BusinessException;
import com.zhongyan.uav.common.error.ErrorCode;

import java.time.Clock;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public class PreviewApplicationService {
    private final AssetRepository assetRepository;
    private final PreviewGeneratorPort previewGeneratorPort;
    private final Clock clock;

    public PreviewApplicationService(AssetRepository assetRepository,
                                     PreviewGeneratorPort previewGeneratorPort) {
        this(assetRepository, previewGeneratorPort, Clock.systemUTC());
    }

    public PreviewApplicationService(AssetRepository assetRepository,
                                     PreviewGeneratorPort previewGeneratorPort,
                                     Clock clock) {
        this.assetRepository = Objects.requireNonNull(assetRepository, "assetRepository must not be null");
        this.previewGeneratorPort = Objects.requireNonNull(previewGeneratorPort, "previewGeneratorPort must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    public Asset generatePreview(String assetId, String requestedBy, Map<String, Object> parameters) {
        Asset asset = assetRepository.findById(assetId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "asset not found: " + assetId));
        PreviewResult result = previewGeneratorPort.generate(new PreviewRequest(
                asset.assetId(),
                asset.objectKey(),
                defaultText(asset.previewObjectKey(), "previews/" + asset.assetId() + ".jpg"),
                parameters == null ? Map.of() : parameters));
        Map<String, Object> metadata = new LinkedHashMap<>(asset.metadata());
        metadata.put("previewObjectKey", result.previewObjectKey());
        metadata.put("previewContentType", result.contentType());
        metadata.put("previewSizeBytes", result.sizeBytes());
        metadata.put("previewChecksum", result.checksum());
        metadata.put("previewRequestedBy", defaultText(requestedBy, "system"));
        return assetRepository.save(asset.attachPreview(result.previewObjectKey(), clock.instant())
                .withMetadata(metadata, clock.instant()));
    }

    private String defaultText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
