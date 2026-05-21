package com.zhongyan.uav.agent.port;

import java.util.List;

/**
 * Boundary for embedding generation so RAG can swap local, cloud, or mock providers.
 */
public interface EmbeddingPort {
    List<Double> embed(String text);
}
