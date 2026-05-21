package com.zhongyan.uav.geo.infrastructure;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "bms.geo.geoserver")
public record GeoServerProperties(
        boolean enabled,
        String url,
        String username,
        String password,
        String workspace,
        String datastore,
        String postgisHost,
        int postgisPort,
        String postgisDatabase,
        String postgisSchema,
        String postgisUsername,
        String postgisPassword,
        String tableName,
        String layerName,
        String styleName,
        int connectTimeoutMs,
        int readTimeoutMs,
        int retryMaxAttempts,
        long retryBackoffMs,
        int circuitFailureThreshold,
        long circuitOpenDurationMs) {
    public GeoServerProperties {
        if (enabled) {
            requireText(url, "url");
            requireText(username, "username");
            requireText(password, "password");
        }
        workspace = defaultText(workspace, "zhongyan");
        datastore = defaultText(datastore, "postgis");
        postgisHost = defaultText(postgisHost, "postgres");
        postgisPort = postgisPort <= 0 ? 5432 : postgisPort;
        postgisDatabase = defaultText(postgisDatabase, "zhongyan_uav");
        postgisSchema = defaultText(postgisSchema, "public");
        postgisUsername = defaultText(postgisUsername, "zhongyan");
        postgisPassword = defaultText(postgisPassword, "zhongyan_local");
        tableName = defaultText(tableName, "asset_geometry");
        layerName = defaultText(layerName, tableName);
        styleName = defaultText(styleName, "asset-boundary");
        connectTimeoutMs = connectTimeoutMs <= 0 ? 3000 : connectTimeoutMs;
        readTimeoutMs = readTimeoutMs <= 0 ? 10000 : readTimeoutMs;
        retryMaxAttempts = retryMaxAttempts <= 0 ? 3 : retryMaxAttempts;
        retryBackoffMs = retryBackoffMs < 0 ? 500 : retryBackoffMs;
        circuitFailureThreshold = circuitFailureThreshold <= 0 ? 3 : circuitFailureThreshold;
        circuitOpenDurationMs = circuitOpenDurationMs <= 0 ? 30000 : circuitOpenDurationMs;
    }

    public String normalizedUrl() {
        String value = defaultText(url, "http://localhost:8080/geoserver");
        while (value.endsWith("/")) {
            value = value.substring(0, value.length() - 1);
        }
        return value;
    }

    private static void requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("bms.geo.geoserver." + name + " must not be blank");
        }
    }

    private static String defaultText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
