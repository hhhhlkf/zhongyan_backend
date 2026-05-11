package com.zhongyan.uav.agent.api;

import com.zhongyan.uav.agent.api.request.ApproveAgentToolCallRequest;
import com.zhongyan.uav.agent.api.response.AgentToolCallView;
import com.zhongyan.uav.common.response.ResponseResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@ResponseResult
@RestController
@RequestMapping("/agent/tool-calls")
public class AgentToolController {
    /**
     * 查询 Agent 工具调用详情空壳，当前不读取审计记录。
     */
    @GetMapping("/{toolCallId}")
    public AgentToolCallView getToolCall(@PathVariable String toolCallId) {
        return AgentToolCallView.placeholder(toolCallId, null, "PLACEHOLDER");
    }

    /**
     * 审批通过 Agent 工具调用空壳，当前不执行真实工具。
     */
    @PostMapping("/{toolCallId}/approve")
    public AgentToolCallView approveToolCall(@PathVariable String toolCallId,
                                             @RequestBody(required = false) ApproveAgentToolCallRequest request) {
        return AgentToolCallView.placeholder(toolCallId, null, "APPROVED");
    }

    /**
     * 拒绝 Agent 工具调用空壳，当前不执行真实工具。
     */
    @PostMapping("/{toolCallId}/reject")
    public AgentToolCallView rejectToolCall(@PathVariable String toolCallId,
                                            @RequestBody(required = false) ApproveAgentToolCallRequest request) {
        return AgentToolCallView.placeholder(toolCallId, null, "REJECTED");
    }
}
