package com.zhongyan.uav.agent.port;

import reactor.core.publisher.Flux;

/**
 * Boundary for all model calls. Application code depends on this port instead of a concrete AI SDK.
 */
public interface ChatModelPort {
    AgentReply chat(AgentChatRequest request);

    Flux<AgentStreamChunk> stream(AgentChatRequest request);
}
