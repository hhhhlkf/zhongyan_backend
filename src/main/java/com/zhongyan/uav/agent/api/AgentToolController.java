package com.zhongyan.uav.agent.api;

import com.zhongyan.uav.agent.api.request.ApproveToolCallRequest;
import com.zhongyan.uav.agent.api.request.InvokeAgentToolRequest;
import com.zhongyan.uav.agent.api.request.RejectToolCallRequest;
import com.zhongyan.uav.agent.api.response.AgentToolCallView;
import com.zhongyan.uav.agent.application.InvokeAgentToolCommand;
import com.zhongyan.uav.agent.application.ToolGatewayService;
import com.zhongyan.uav.common.response.ResponseResult;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Agent 工具调用 API 控制器。
 * <p>
 * 所有工具调用、审批通过和审批拒绝都从这里进入 Tool Gateway，
 * 防止 Agent 绕过审计链路直接操作任务、资产或设备。
 */
@ResponseResult
@RestController
@RequestMapping("/agent/tool-calls")
public class AgentToolController {
    private final ToolGatewayService toolGatewayService;

    public AgentToolController(ToolGatewayService toolGatewayService) {
        this.toolGatewayService = toolGatewayService;
    }

    /**
     * Agent 工具调用统一入口。Controller 只组装命令，权限、风险、审批和审计全部交给 Tool Gateway。
     */
    @PostMapping
    @PreAuthorize("hasAuthority('AGENT_CHAT')")
    public AgentToolCallView invokeTool(@Valid @RequestBody InvokeAgentToolRequest request) {
        return AgentToolCallView.fromDomain(toolGatewayService.invoke(new InvokeAgentToolCommand(
                request.sessionId(), request.messageId(), request.userId(), request.toolName(),
                request.input(), request.permissions())));
    }

    /**
     * 查询 Agent 工具调用审计记录。
     */
    @GetMapping("/{toolCallId}")
    @PreAuthorize("hasAnyAuthority('AGENT_CHAT', 'AGENT_TOOL_APPROVE')")
    public AgentToolCallView getToolCall(@PathVariable String toolCallId) {
        return AgentToolCallView.fromDomain(toolGatewayService.getToolCall(toolCallId));
    }

    /**
     * 审批通过后由 Tool Gateway 继续执行原工具调用，避免审批接口绕过审计链路。
     */
    @PostMapping("/{toolCallId}/approve")
    @PreAuthorize("hasAuthority('AGENT_TOOL_APPROVE')")
    public AgentToolCallView approveToolCall(@PathVariable String toolCallId,
                                             @Valid @RequestBody(required = false) ApproveToolCallRequest request) {
        String reviewer = request == null ? "anonymous" : request.reviewer();
        return AgentToolCallView.fromDomain(toolGatewayService.approve(toolCallId, reviewer));
    }

    /**
     * 拒绝审批会把工具调用终止在审计记录里，不再触发工具执行。
     */
    @PostMapping("/{toolCallId}/reject")
    @PreAuthorize("hasAuthority('AGENT_TOOL_APPROVE')")
    public AgentToolCallView rejectToolCall(@PathVariable String toolCallId,
                                            @Valid @RequestBody(required = false) RejectToolCallRequest request) {
        String reason = request == null ? "rejected" : request.reason();
        return AgentToolCallView.fromDomain(toolGatewayService.reject(toolCallId, reason));
    }
}
