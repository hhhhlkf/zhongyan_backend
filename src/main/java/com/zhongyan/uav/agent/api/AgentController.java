package com.zhongyan.uav.agent.api;

import com.zhongyan.uav.agent.api.request.CreateAgentSessionRequest;
import com.zhongyan.uav.agent.api.request.SendAgentMessageRequest;
import com.zhongyan.uav.agent.api.response.AgentConversationView;
import com.zhongyan.uav.agent.api.response.AgentEventView;
import com.zhongyan.uav.agent.api.response.AgentMessageView;
import com.zhongyan.uav.agent.api.response.AgentSessionView;
import com.zhongyan.uav.agent.application.AgentApplicationService;
import com.zhongyan.uav.agent.application.CreateAgentSessionCommand;
import com.zhongyan.uav.agent.application.SendAgentMessageCommand;
import com.zhongyan.uav.common.response.ResponseResult;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Agent 会话 API 控制器。
 * <p>
 * 提供创建会话、查询会话、发送消息和查询会话事件的 REST 入口；
 * 控制器只做参数校验、权限声明和 DTO 转换，具体编排交给 {@link AgentApplicationService}。
 */
@ResponseResult
@RestController
@RequestMapping("/agent/sessions")
public class AgentController {
    private final AgentApplicationService agentApplicationService;

    public AgentController(AgentApplicationService agentApplicationService) {
        this.agentApplicationService = agentApplicationService;
    }

    /**
     * 创建 Agent 会话，只建立编排上下文，不在该入口直接执行工具。
     */
    @PostMapping
    @PreAuthorize("hasAuthority('AGENT_CHAT')")
    public AgentSessionView createSession(@Valid @RequestBody CreateAgentSessionRequest request) {
        return AgentSessionView.fromDomain(agentApplicationService.createSession(new CreateAgentSessionCommand(
                request.missionId(), request.taskId(), request.createdBy(), request.title(), request.context())));
    }

    /**
     * 查询 Agent 会话详情，返回已持久化的会话状态。
     */
    @GetMapping("/{sessionId}")
    @PreAuthorize("hasAuthority('AGENT_CHAT')")
    public AgentSessionView getSession(@PathVariable String sessionId) {
        return agentApplicationService.findSession(sessionId)
                .map(AgentSessionView::fromDomain)
                .orElseThrow(() -> new IllegalArgumentException("Agent session not found: " + sessionId));
    }

    /**
     * 发送用户消息并触发一次完整 Agent 编排：模型回复、工具计划和审计都在应用服务内完成。
     */
    @PostMapping("/{sessionId}/messages")
    @PreAuthorize("hasAuthority('AGENT_CHAT')")
    public AgentConversationView createMessage(@PathVariable String sessionId,
                                               @Valid @RequestBody SendAgentMessageRequest request) {
        return AgentConversationView.fromResult(agentApplicationService.sendMessageAndReply(
                new SendAgentMessageCommand(sessionId, request.createdBy(), request.content(), request.attachments())));
    }

    /**
     * 查询会话消息列表，用于前端恢复对话窗口。
     */
    @GetMapping("/{sessionId}/messages")
    @PreAuthorize("hasAuthority('AGENT_CHAT')")
    public List<AgentMessageView> listMessages(@PathVariable String sessionId) {
        return agentApplicationService.listMessages(sessionId).stream()
                .map(AgentMessageView::fromDomain)
                .collect(Collectors.toList());
    }

    /**
     * 查询由消息和工具调用重建的轻量事件时间线；实时流仍走 /realtime/agent/sessions/{sessionId}/events。
     */
    @GetMapping("/{sessionId}/events")
    @PreAuthorize("hasAuthority('AGENT_CHAT')")
    public List<AgentEventView> listEvents(@PathVariable String sessionId) {
        return agentApplicationService.listSessionEvents(sessionId).stream()
                .map(AgentEventView::fromEvent)
                .collect(Collectors.toList());
    }
}
