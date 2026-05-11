package com.zhongyan.uav.event.port;

import com.zhongyan.uav.event.domain.EventEnvelope;

public interface EventSubscriber {
    void onEvent(EventEnvelope event);
}
