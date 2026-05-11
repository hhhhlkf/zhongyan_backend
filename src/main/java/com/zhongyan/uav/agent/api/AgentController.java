package com.zhongyan.uav.agent.api;

import com.zhongyan.uav.agent.api.request.CreateAgentMessageRequest;
import com.zhongyan.uav.agent.api.request.CreateAgentSessionRequest;
import com.zhongyan.uav.agent.api.response.AgentMessageView;
import com.zhongyan.uav.agent.api.response.AgentSessionView;
import com.zhongyan.uav.common.response.ResponseResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@ResponseResult
@RestController
@RequestMapping("/agent/sessions")
public class AgentController {
    /**
     * 创建 Agent 会话空壳响应，当前不调用模型、不写会话存储。
     */
    @PostMapping
    public AgentSessionView createSession(@RequestBody CreateAgentSessionRequest request) {
        String sessionId = "agent-session-" + UUID.randomUUID();
        return AgentSessionView.placeholder(sessionId, request.missionId(), request.taskId());
    }

    /**
     * 查询 Agent 会话详情空壳，当前返回路径参数组成的占位视图。
     */
    @GetMapping("/{sessionId}")
    public AgentSessionView getSession(@PathVariable String sessionId) {
        return AgentSessionView.placeholder(sessionId, null, null);
    }

    /**
     * 发送 Agent 消息空壳响应，当前不调用模型、不触发工具。
     */
    @PostMapping("/{sessionId}/messages")
    public AgentMessageView createMessage(@PathVariable String sessionId,
                                          @RequestBody CreateAgentMessageRequest request) {
        String messageId = "agent-message-" + UUID.randomUUID();
        return AgentMessageView.placeholder(messageId, sessionId, request.role(), request.content());
    }

    /**
     * 查询 Agent 会话事件空壳，当前不读取事件流。
     */
    @GetMapping("/{sessionId}/events")
    public List<AgentMessageView> listEvents(@PathVariable String sessionId) {
        return List.of();
    }
}
