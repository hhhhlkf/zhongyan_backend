package com.zhongyan.uav.device.domain;

import java.util.Map;

public record TransferParameters(Map<String, Object> values) {
    public TransferParameters {
        values = values == null ? Map.of() : Map.copyOf(values);
    }
}
