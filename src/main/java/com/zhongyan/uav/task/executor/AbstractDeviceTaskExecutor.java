package com.zhongyan.uav.task.executor;

import java.util.Map;

abstract class AbstractDeviceTaskExecutor implements TaskExecutor {
    protected String text(Map<String, Object> snapshot, String key, String fallback) {
        Object value = snapshot.get(key);
        if (value == null) {
            return fallback;
        }
        String text = value.toString();
        return text.isBlank() ? fallback : text;
    }

    protected int intValue(Map<String, Object> snapshot, String key, int fallback) {
        Object value = snapshot.get(key);
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value instanceof String text && !text.isBlank()) {
            return Integer.parseInt(text);
        }
        return fallback;
    }
}
