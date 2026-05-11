package com.zhongyan.uav.asset.application;

import com.zhongyan.uav.asset.api.response.AssetLayerView;
import com.zhongyan.uav.asset.api.response.AssetStatsView;
import com.zhongyan.uav.asset.api.response.AssetView;
import com.zhongyan.uav.asset.domain.Asset;
import com.zhongyan.uav.asset.domain.AssetRepository;
import com.zhongyan.uav.asset.domain.AssetStatus;
import com.zhongyan.uav.asset.domain.AssetType;
import com.zhongyan.uav.asset.port.AssetStoragePort;
import com.zhongyan.uav.common.error.BusinessException;
import com.zhongyan.uav.common.error.ErrorCode;

import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class AssetQueryService {
    private static final Duration READ_URL_EXPIRY = Duration.ofMinutes(15);

    private final AssetRepository assetRepository;
    private final AssetStoragePort assetStoragePort;

    public AssetQueryService(AssetRepository assetRepository, AssetStoragePort assetStoragePort) {
        this.assetRepository = Objects.requireNonNull(assetRepository, "assetRepository must not be null");
        this.assetStoragePort = Objects.requireNonNull(assetStoragePort, "assetStoragePort must not be null");
    }

    public List<AssetView> listAssets(String missionId, String taskId, String status) {
        List<Asset> assets;
        if (missionId != null && !missionId.isBlank()) {
            assets = assetRepository.findByMissionId(missionId);
        } else if (taskId != null && !taskId.isBlank()) {
            assets = assetRepository.findByTaskId(taskId);
        } else if (status != null && !status.isBlank()) {
            assets = assetRepository.findByStatus(parseStatus(status));
        } else {
            assets = assetRepository.findAll();
        }
        return assets.stream().map(this::toView).collect(Collectors.toList());
    }

    public AssetView getAsset(String assetId) {
        return toView(findRequired(assetId));
    }

    public AssetStatsView getStats() {
        List<Asset> assets = assetRepository.findAll();
        return new AssetStatsView(
                assets.size(),
                count(assets, AssetType.IMAGE),
                count(assets, AssetType.VIDEO),
                count(assets, AssetType.MODEL_RESULT),
                count(assets, AssetType.REPORT),
                count(assets, AssetType.ATTACHMENT));
    }

    public List<AssetLayerView> listLayers(String assetId) {
        Asset asset = findRequired(assetId);
        if (asset.layerUrl() == null || asset.layerUrl().isBlank()) {
            return List.of();
        }
        return List.of(toLayerView(asset));
    }

    public AssetLayerView getLayer(String assetId, String layerId) {
        Asset asset = findRequired(assetId);
        if (asset.layerUrl() == null || asset.layerUrl().isBlank()) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "layer not found: " + layerId);
        }
        return toLayerView(asset);
    }

    public AssetView toView(Asset asset) {
        String url = readUrl(asset.objectKey());
        String previewUrl = readUrl(asset.previewObjectKey());
        return new AssetView(asset.assetId(), asset.assetType().name(), asset.status().name(), asset.name(),
                url, previewUrl, asset.geoStatus().name(), asset.layerUrl(), asset.createdAt());
    }

    private Asset findRequired(String assetId) {
        return assetRepository.findById(assetId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "asset not found: " + assetId));
    }

    private AssetLayerView toLayerView(Asset asset) {
        Object layerId = asset.metadata().getOrDefault("layerId", asset.assetId() + "-layer");
        return new AssetLayerView(asset.assetId(), layerId.toString(), asset.layerUrl(),
                asset.status().name(), asset.updatedAt());
    }

    private String readUrl(String objectKey) {
        if (objectKey == null || objectKey.isBlank()) {
            return null;
        }
        return assetStoragePort.createReadUrl(objectKey, READ_URL_EXPIRY);
    }

    private long count(List<Asset> assets, AssetType assetType) {
        return assets.stream().filter(asset -> asset.assetType() == assetType).count();
    }

    private AssetStatus parseStatus(String status) {
        try {
            return AssetStatus.valueOf(status.trim().toUpperCase());
        } catch (IllegalArgumentException exception) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "invalid asset status: " + status);
        }
    }
}
