package com.zhongyan.uav.geo.infrastructure.mock;

import com.zhongyan.uav.geo.port.GroundElevationPort;

public class MockGroundElevationPort implements GroundElevationPort {
    @Override
    public double elevationMeters(double latitude, double longitude) {
        return 0d;
    }
}
