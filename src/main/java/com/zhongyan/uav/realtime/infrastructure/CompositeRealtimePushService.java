package com.zhongyan.uav.realtime.infrastructure;

import com.zhongyan.uav.event.domain.EventEnvelope;
import com.zhongyan.uav.realtime.application.RealtimePushService;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public class CompositeRealtimePushService implements RealtimePushService {
    private final SseRealtimePushService sseRealtimePushService;
    private final WebSocketRealtimePushService webSocketRealtimePushService;

    public CompositeRealtimePushService(SseRealtimePushService sseRealtimePushService,
                                        WebSocketRealtimePushService webSocketRealtimePushService) {
        this.sseRealtimePushService = sseRealtimePushService;
        this.webSocketRealtimePushService = webSocketRealtimePushService;
    }

    @Override
    public SseEmitter subscribe(String topic, String key) {
        return sseRealtimePushService.subscribe(topic, key);
    }

    @Override
    public void push(EventEnvelope event) {
        sseRealtimePushService.push(event);
        webSocketRealtimePushService.push(event);
    }
}
