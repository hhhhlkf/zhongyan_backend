package com.zhongyan.uav.device.infrastructure;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

public final class DevicePayloads {
    private DevicePayloads() {
    }

    public static Map<String, Object> merge(Map<String, Object> first, Map<String, Object> second) {
        Map<String, Object> values = new LinkedHashMap<>();
        if (first != null) {
            values.putAll(first);
        }
        if (second != null) {
            values.putAll(second);
        }
        return Map.copyOf(values);
    }

    public static Map<String, Object> merge(Map<String, Object> first, Map<String, Object> second,
                                            Map<String, Object> third) {
        return merge(merge(first, second), third);
    }

    public static String text(Map<String, Object> values, String fallback, String... keys) {
        if (values == null) {
            return fallback;
        }
        for (String key : keys) {
            Object value = values.get(key);
            if (value != null && !value.toString().isBlank()) {
                return value.toString();
            }
        }
        return fallback;
    }

    public static int integer(Map<String, Object> values, int fallback, String... keys) {
        if (values == null) {
            return fallback;
        }
        for (String key : keys) {
            Object value = values.get(key);
            if (value instanceof Number number) {
                return number.intValue();
            }
            if (value instanceof String text && !text.isBlank()) {
                return Integer.parseInt(text);
            }
        }
        return fallback;
    }

    public static Duration duration(Map<String, Object> values, Duration fallback, String... keys) {
        if (values == null) {
            return fallback;
        }
        for (String key : keys) {
            Object value = values.get(key);
            if (value instanceof Number number) {
                return Duration.ofMillis(number.longValue());
            }
            if (value instanceof String text && !text.isBlank()) {
                return Duration.ofMillis(Long.parseLong(text));
            }
        }
        return fallback;
    }

    public static Map<String, Object> stringMap(Object value) {
        if (!(value instanceof Map<?, ?> source)) {
            return Map.of();
        }
        Map<String, Object> result = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : source.entrySet()) {
            if (entry.getKey() instanceof String key) {
                result.put(key, entry.getValue());
            }
        }
        return Map.copyOf(result);
    }
}
