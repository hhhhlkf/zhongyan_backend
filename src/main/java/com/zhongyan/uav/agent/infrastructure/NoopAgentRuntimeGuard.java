package com.zhongyan.uav.agent.infrastructure;

import com.zhongyan.uav.agent.port.AgentRuntimeGuard;

import java.util.Map;
import java.util.Optional;

public class NoopAgentRuntimeGuard implements AgentRuntimeGuard {
    @Override
    public void assertToolCallAllowed(String sessionId, String userId, String toolName) {
    }

    @Override
    public Optional<String> reserveIdempotencyKey(String sessionId, String toolName, Map<String, Object> input,
                                                  String toolCallId) {
        return Optional.empty();
    }
}
