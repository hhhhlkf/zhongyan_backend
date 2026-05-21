package com.zhongyan.uav.event.port;

import com.zhongyan.uav.event.domain.EventEnvelope;

/**
 * 统一事件发布端口。
 * <p>
 * 应用服务通过该端口发布 {@link EventEnvelope}，具体实现可以写入 Kafka、
 * 内存测试发布器或其他事件基础设施。
 */
public interface EventPublisher {
    /**
     * 发布一条统一事件。
     *
     * @param event 带主题、消息键和载荷的事件信封
     */
    void publish(EventEnvelope event);
}
