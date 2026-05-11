package com.zhongyan.uav.asset.application;

import com.zhongyan.uav.asset.domain.Asset;
import com.zhongyan.uav.asset.domain.AssetRepository;
import com.zhongyan.uav.asset.domain.AssetRole;
import com.zhongyan.uav.asset.domain.AssetStatus;
import com.zhongyan.uav.asset.domain.AssetType;
import com.zhongyan.uav.asset.domain.TaskAsset;
import com.zhongyan.uav.asset.domain.TaskAssetRepository;
import com.zhongyan.uav.common.error.BusinessException;
import com.zhongyan.uav.common.error.ErrorCode;

import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class AssetApplicationService {
    private final AssetRepository assetRepository;
    private final TaskAssetRepository taskAssetRepository;
    private final Clock clock;

    public AssetApplicationService(AssetRepository assetRepository,
                                   TaskAssetRepository taskAssetRepository) {
        this(assetRepository, taskAssetRepository, Clock.systemUTC());
    }

    public AssetApplicationService(AssetRepository assetRepository,
                                   TaskAssetRepository taskAssetRepository,
                                   Clock clock) {
        this.assetRepository = Objects.requireNonNull(assetRepository, "assetRepository must not be null");
        this.taskAssetRepository = Objects.requireNonNull(taskAssetRepository, "taskAssetRepository must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    public Asset createAsset(CreateAssetInput input) {
        Objects.requireNonNull(input, "input must not be null");
        Instant now = clock.instant();
        Asset asset = Asset.created("asset-" + UUID.randomUUID(),
                required(input.missionId(), "missionId"),
                blankToNull(input.taskId()),
                parseEnum(AssetType.class, input.assetType(), "assetType"),
                parseRole(input.role()),
                required(input.name(), "name"),
                input.objectKey(),
                input.contentType(),
                input.sizeBytes(),
                input.checksum(),
                input.metadata() == null ? Map.of() : input.metadata(),
                defaultText(input.createdBy(), "system"),
                now);
        Asset saved = assetRepository.save(asset.markAvailable(now));
        if (saved.taskId() != null && !saved.taskId().isBlank()) {
            taskAssetRepository.save(new TaskAsset(saved.taskId(), saved.assetId(), saved.assetRole(), now));
        }
        return saved;
    }

    public Asset markDeleted(String assetId) {
        Asset asset = findRequired(assetId);
        if (asset.status() == AssetStatus.DELETED) {
            return asset;
        }
        return assetRepository.save(asset.delete(clock.instant()));
    }

    public Asset findRequired(String assetId) {
        return assetRepository.findById(assetId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "asset not found: " + assetId));
    }

    private AssetRole parseRole(String value) {
        if (value == null || value.isBlank()) {
            return AssetRole.OUTPUT;
        }
        return parseEnum(AssetRole.class, value, "role");
    }

    private <T extends Enum<T>> T parseEnum(Class<T> enumType, String value, String fieldName) {
        try {
            return Enum.valueOf(enumType, required(value, fieldName).trim().toUpperCase());
        } catch (IllegalArgumentException exception) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "invalid " + fieldName + ": " + value);
        }
    }

    private String required(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, fieldName + " must not be blank");
        }
        return value;
    }

    private String defaultText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
