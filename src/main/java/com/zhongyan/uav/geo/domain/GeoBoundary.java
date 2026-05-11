package com.zhongyan.uav.geo.domain;

import java.util.List;
import java.util.Map;

public record GeoBoundary(
        String assetId,
        String type,
        List<List<Double>> coordinates,
        Map<String, Object> properties) {
    public GeoBoundary {
        if (assetId == null || assetId.isBlank()) {
            throw new IllegalArgumentException("assetId must not be blank");
        }
        type = type == null || type.isBlank() ? "Polygon" : type;
        coordinates = coordinates == null ? List.of() : List.copyOf(coordinates);
        properties = properties == null ? Map.of() : Map.copyOf(properties);
    }

    public Map<String, Object> toGeoJson() {
        return Map.of(
                "type", "Feature",
                "geometry", Map.of("type", type, "coordinates", List.of(coordinates)),
                "properties", properties);
    }
}
