package com.zhongyan.uav.agent.port;

import java.util.Map;
import java.util.Optional;

public interface AgentRuntimeGuard {
    void assertToolCallAllowed(String sessionId, String userId, String toolName);

    Optional<String> reserveIdempotencyKey(String sessionId, String toolName, Map<String, Object> input,
                                           String toolCallId);

    default void releaseIdempotencyKey(String sessionId, String toolName, Map<String, Object> input) {
    }

    static AgentRuntimeGuard noop() {
        return new AgentRuntimeGuard() {
            @Override
            public void assertToolCallAllowed(String sessionId, String userId, String toolName) {
            }

            @Override
            public Optional<String> reserveIdempotencyKey(String sessionId, String toolName,
                                                          Map<String, Object> input, String toolCallId) {
                return Optional.empty();
            }
        };
    }
}
