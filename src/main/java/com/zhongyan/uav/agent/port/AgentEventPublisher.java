package com.zhongyan.uav.agent.port;

/**
 * Agent 审计事件发布端口。
 * <p>
 * 应用层只依赖该端口发布会话、消息、工具、审批和报告草稿事件；
 * 具体实现负责写入事件总线并同步通知实时推送通道。
 */
public interface AgentEventPublisher {
    /**
     * 发布一条 Agent 审计事件。
     *
     * @param event 已包含 sessionId、事件类型、载荷和发生时间的事件对象
     */
    void publish(AgentEvent event);
}
