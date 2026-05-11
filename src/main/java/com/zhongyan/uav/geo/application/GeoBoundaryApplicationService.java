package com.zhongyan.uav.geo.application;

import com.zhongyan.uav.asset.domain.Asset;
import com.zhongyan.uav.asset.domain.AssetRepository;
import com.zhongyan.uav.common.error.BusinessException;
import com.zhongyan.uav.common.error.ErrorCode;
import com.zhongyan.uav.geo.domain.GeoBoundary;
import com.zhongyan.uav.geo.port.GroundElevationPort;

import java.time.Clock;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class GeoBoundaryApplicationService {
    private static final double DEFAULT_HALF_SIZE_DEGREES = 0.0005d;

    private final AssetRepository assetRepository;
    private final GroundElevationPort groundElevationPort;
    private final Clock clock;

    public GeoBoundaryApplicationService(AssetRepository assetRepository,
                                         GroundElevationPort groundElevationPort) {
        this(assetRepository, groundElevationPort, Clock.systemUTC());
    }

    public GeoBoundaryApplicationService(AssetRepository assetRepository,
                                         GroundElevationPort groundElevationPort,
                                         Clock clock) {
        this.assetRepository = Objects.requireNonNull(assetRepository, "assetRepository must not be null");
        this.groundElevationPort = Objects.requireNonNull(groundElevationPort, "groundElevationPort must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    public Asset calculateBoundary(String assetId, Map<String, Object> parameters) {
        Asset asset = assetRepository.findById(assetId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "asset not found: " + assetId));
        try {
            GeoBoundary boundary = boundary(asset, parameters == null ? Map.of() : parameters);
            Map<String, Object> metadata = new LinkedHashMap<>(asset.metadata());
            metadata.put("geometry", boundary.toGeoJson());
            metadata.put("geoCalculatedAt", clock.instant().toString());
            metadata.put("geoSource", "application-service");
            return assetRepository.save(asset.withMetadata(metadata, clock.instant()).markGeoCalculated(clock.instant()));
        } catch (RuntimeException exception) {
            Map<String, Object> metadata = new LinkedHashMap<>(asset.metadata());
            metadata.put("geoError", exception.getMessage());
            return assetRepository.save(asset.withMetadata(metadata, clock.instant()).markGeoFailed(clock.instant()));
        }
    }

    private GeoBoundary boundary(Asset asset, Map<String, Object> parameters) {
        double latitude = doubleValue(parameters, "latitude", doubleValue(asset.metadata(), "latitude", 0d));
        double longitude = doubleValue(parameters, "longitude", doubleValue(asset.metadata(), "longitude", 0d));
        double halfSize = doubleValue(parameters, "halfSizeDegrees", DEFAULT_HALF_SIZE_DEGREES);
        double elevation = groundElevationPort.elevationMeters(latitude, longitude);
        List<List<Double>> coordinates = List.of(
                List.of(longitude - halfSize, latitude - halfSize),
                List.of(longitude + halfSize, latitude - halfSize),
                List.of(longitude + halfSize, latitude + halfSize),
                List.of(longitude - halfSize, latitude + halfSize),
                List.of(longitude - halfSize, latitude - halfSize));
        return new GeoBoundary(asset.assetId(), "Polygon", coordinates,
                Map.of("assetId", asset.assetId(), "groundElevationMeters", elevation));
    }

    private double doubleValue(Map<String, Object> values, String name, double fallback) {
        Object value = values.get(name);
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        if (value instanceof String text && !text.isBlank()) {
            return Double.parseDouble(text);
        }
        return fallback;
    }
}
