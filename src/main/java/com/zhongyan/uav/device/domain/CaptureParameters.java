package com.zhongyan.uav.device.domain;

import java.util.Map;

public record CaptureParameters(Map<String, Object> values) {
    public CaptureParameters {
        values = values == null ? Map.of() : Map.copyOf(values);
    }
}
