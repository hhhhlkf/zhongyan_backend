package com.zhongyan.uav.realtime.application;

import com.zhongyan.uav.event.domain.EventEnvelope;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public interface RealtimePushService {
    SseEmitter subscribe(String topic, String key);

    void push(EventEnvelope event);
}
