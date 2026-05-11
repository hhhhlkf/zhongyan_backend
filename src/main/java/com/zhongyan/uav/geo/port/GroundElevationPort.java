package com.zhongyan.uav.geo.port;

public interface GroundElevationPort {
    double elevationMeters(double latitude, double longitude);
}
