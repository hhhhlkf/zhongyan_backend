package com.zhongyan.uav.mission.domain;

public record MissionRegion(
        String administrativeCode,
        String administrativeName,
        Double centerLongitude,
        Double centerLatitude,
        String boundaryGeoJson) {
    /**
     * 校验中心点经纬度范围，允许区域信息暂时为空。
     */
    public MissionRegion {
        if (centerLongitude != null && (centerLongitude < -180 || centerLongitude > 180)) {
            throw new IllegalArgumentException("centerLongitude must be between -180 and 180");
        }
        if (centerLatitude != null && (centerLatitude < -90 || centerLatitude > 90)) {
            throw new IllegalArgumentException("centerLatitude must be between -90 and 90");
        }
    }
}
