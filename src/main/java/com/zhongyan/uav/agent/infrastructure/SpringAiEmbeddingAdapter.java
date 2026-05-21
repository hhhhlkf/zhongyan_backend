package com.zhongyan.uav.agent.infrastructure;

import com.zhongyan.uav.agent.port.EmbeddingPort;
import org.springframework.ai.embedding.EmbeddingModel;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class SpringAiEmbeddingAdapter implements EmbeddingPort {
    private final AgentProperties properties;
    private final EmbeddingModel embeddingModel;

    public SpringAiEmbeddingAdapter(AgentProperties properties, EmbeddingModel embeddingModel) {
        this.properties = properties;
        this.embeddingModel = embeddingModel;
    }

    @Override
    public List<Double> embed(String text) {
        if (!properties.enabled() || embeddingModel == null) {
            return Collections.emptyList();
        }
        if (text == null || text.isBlank()) {
            return Collections.emptyList();
        }
        try {
            float[] vector = embeddingModel.embed(text);
            return Arrays.stream(toDoubleArray(vector)).boxed().toList();
        } catch (RuntimeException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IllegalStateException("Spring AI embedding model call failed: " + exception.getMessage(), exception);
        }
    }

    private static double[] toDoubleArray(float[] vector) {
        double[] values = new double[vector.length];
        for (int i = 0; i < vector.length; i++) {
            values[i] = vector[i];
        }
        return values;
    }
}
