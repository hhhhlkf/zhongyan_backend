package com.zhongyan.uav.agent.infrastructure;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

class AgentPropertiesTests {
    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(TestConfig.class);

    @Test
    void bindsLocalAgentProperties() {
        contextRunner
                .withPropertyValues(
                        "bms.agent.enabled=true",
                        "bms.agent.base-url=http://localhost:11434/v1/",
                        "bms.agent.api-key=local-dev-key",
                        "bms.agent.chat-model=qwen3:8b",
                        "bms.agent.embedding-model=qwen3-embedding:0.6b",
                        "bms.agent.max-tool-depth=8",
                        "bms.agent.stream-enabled=true",
                        "bms.agent.approval-required-for-write=true",
                        "bms.agent.events-topic=agent-events",
                        "bms.agent.connect-timeout=PT2S",
                        "bms.agent.read-timeout=PT30S")
                .run(context -> {
                    AgentProperties properties = context.getBean(AgentProperties.class);

                    assertThat(properties.enabled()).isTrue();
                    assertThat(properties.normalizedBaseUrl()).isEqualTo("http://localhost:11434/v1");
                    assertThat(properties.chatModel()).isEqualTo("qwen3:8b");
                    assertThat(properties.embeddingModel()).isEqualTo("qwen3-embedding:0.6b");
                    assertThat(properties.maxToolDepth()).isEqualTo(8);
                    assertThat(properties.eventsTopic()).isEqualTo("agent-events");
                });
    }

    @Test
    void suppliesSafeDefaultsWhenAgentDisabled() {
        contextRunner
                .withPropertyValues("bms.agent.enabled=false")
                .run(context -> {
                    AgentProperties properties = context.getBean(AgentProperties.class);

                    assertThat(properties.enabled()).isFalse();
                    assertThat(properties.normalizedBaseUrl()).isEqualTo("http://localhost:11434/v1");
                    assertThat(properties.maxToolDepth()).isEqualTo(5);
                    assertThat(properties.eventsTopic()).isEqualTo("agent-events");
                });
    }

    @Test
    void requiresModelConnectionDetailsWhenAgentEnabled() {
        contextRunner
                .withPropertyValues(
                        "bms.agent.enabled=true",
                        "bms.agent.base-url=",
                        "bms.agent.chat-model=",
                        "bms.agent.embedding-model=")
                .run(context -> assertThat(context).hasFailed());
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(AgentProperties.class)
    static class TestConfig {
    }
}
