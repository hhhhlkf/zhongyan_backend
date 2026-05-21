package com.zhongyan.uav.common.resilience;

import org.springframework.retry.support.RetryTemplate;
import org.springframework.retry.support.RetryTemplateBuilder;

import java.time.Clock;
import java.util.Objects;
import java.util.function.Supplier;

public class ExternalCallGuard {
    private final String name;
    private final RetryTemplate retryTemplate;
    private final Clock clock;
    private final int failureThreshold;
    private final long openDurationMs;
    private int consecutiveFailures;
    private long openUntilMs;

    public ExternalCallGuard(String name, int maxAttempts, long backoffMs,
                             int failureThreshold, long openDurationMs) {
        this(name, maxAttempts, backoffMs, failureThreshold, openDurationMs, RuntimeException.class);
    }

    @SafeVarargs
    public ExternalCallGuard(String name, int maxAttempts, long backoffMs,
                             int failureThreshold, long openDurationMs,
                             Class<? extends Throwable>... retryOn) {
        this.name = requireText(name, "name");
        RetryTemplateBuilder builder = RetryTemplate.builder()
                .maxAttempts(Math.max(1, maxAttempts))
                .fixedBackoff(Math.max(1, backoffMs));
        if (retryOn == null || retryOn.length == 0) {
            builder.retryOn(RuntimeException.class);
        } else {
            for (Class<? extends Throwable> retryType : retryOn) {
                builder.retryOn(retryType);
            }
        }
        this.retryTemplate = builder.build();
        this.clock = Clock.systemUTC();
        this.failureThreshold = Math.max(1, failureThreshold);
        this.openDurationMs = Math.max(1, openDurationMs);
    }

    public <T> T execute(String operation, Supplier<T> supplier) {
        Objects.requireNonNull(supplier, "supplier must not be null");
        assertClosed(operation);
        try {
            T result = retryTemplate.execute(context -> supplier.get());
            markSuccess();
            return result;
        } catch (RuntimeException ex) {
            markFailure();
            throw ex;
        }
    }

    private synchronized void assertClosed(String operation) {
        long now = clock.millis();
        if (openUntilMs > now) {
            throw new IllegalStateException("circuit open for " + name + "." + operation);
        }
        if (openUntilMs > 0) {
            openUntilMs = 0;
            consecutiveFailures = 0;
        }
    }

    private synchronized void markSuccess() {
        consecutiveFailures = 0;
        openUntilMs = 0;
    }

    private synchronized void markFailure() {
        consecutiveFailures++;
        if (consecutiveFailures >= failureThreshold) {
            openUntilMs = clock.millis() + openDurationMs;
        }
    }

    private static String requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
    }
}
