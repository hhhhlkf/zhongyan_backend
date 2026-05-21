package com.zhongyan.uav.realtime.application;

import com.zhongyan.uav.event.domain.EventEnvelope;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * 实时推送应用端口。
 * <p>
 * 业务模块和事件消费者通过该端口把统一事件推送给在线订阅者，API 层通过该端口
 * 创建 SSE 订阅连接。后续替换为 WebSocket 时也应保持该端口稳定。
 */
public interface RealtimePushService {
    /**
     * 创建指定主题和消息键的 SSE 订阅。
     *
     * @param topic 事件主题，例如 task-events、uav-telemetry、agent-events
     * @param key   主题内的聚合键，例如 taskId、uavId、sessionId
     * @return 已注册的 SSE emitter
     */
    SseEmitter subscribe(String topic, String key);

    /**
     * 向匹配主题和消息键的在线订阅者推送事件。
     *
     * @param event 已发布或待旁路推送的统一事件信封
     */
    void push(EventEnvelope event);
}
