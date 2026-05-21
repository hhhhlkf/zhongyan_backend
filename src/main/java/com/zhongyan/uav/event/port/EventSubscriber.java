package com.zhongyan.uav.event.port;

import com.zhongyan.uav.event.domain.EventEnvelope;

/**
 * 统一事件订阅端口。
 * <p>
 * 基础设施消费者收到消息后调用该端口，应用内处理器可在这里完成去重、实时推送和死信记录。
 */
public interface EventSubscriber {
    /**
     * 处理一条已经反序列化的统一事件。
     *
     * @param event 事件信封
     */
    void onEvent(EventEnvelope event);
}
