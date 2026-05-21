package com.zhongyan.uav.agent.port;

import com.zhongyan.uav.agent.domain.ToolRiskLevel;

import java.util.Map;
import java.util.Set;

/**
 * Tool metadata and execution boundary. Every Agent-visible capability must go through this contract.
 */
public interface AgentTool {
    String name();

    String description();

    AgentToolInputSchema inputSchema();

    ToolRiskLevel riskLevel();

    boolean approvalRequired();

    Set<String> requiredPermissions();

    AgentToolResult execute(AgentToolContext context, Map<String, Object> input);
}
