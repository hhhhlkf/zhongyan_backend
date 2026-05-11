package com.zhongyan.uav.device.domain;

import java.util.Map;

public record ProcessParameters(Map<String, Object> values) {
    public ProcessParameters {
        values = values == null ? Map.of() : Map.copyOf(values);
    }
}
