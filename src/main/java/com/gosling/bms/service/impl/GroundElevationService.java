package com.gosling.bms.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gosling.bms.conf.DataConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class GroundElevationService {

    static final String DEFAULT_ELEVATION_API_URL = "https://api.open-meteo.com/v1/elevation";
    private static final int DEFAULT_CONNECT_TIMEOUT_MS = 2000;
    private static final int DEFAULT_READ_TIMEOUT_MS = 3000;
    private static final long DEFAULT_CACHE_TTL_SECONDS = 86400L;

    private final ObjectMapper objectMapper;
    private final ElevationHttpClient httpClient;
    private final Map<String, CachedElevation> cache;

    public GroundElevationService() {
        this(new ObjectMapper(), new UrlConnectionElevationHttpClient(), new ConcurrentHashMap<>());
    }

    GroundElevationService(ObjectMapper objectMapper, ElevationHttpClient httpClient, Map<String, CachedElevation> cache) {
        this.objectMapper = objectMapper;
        this.httpClient = httpClient;
        this.cache = cache;
    }

    public double resolveGroundElevation(DataConfig.Coordinate coordinateConfig, double latitude, double longitude) {
        if (coordinateConfig == null) {
            throw new IllegalArgumentException("Coordinate config is required");
        }

        if (!Boolean.TRUE.equals(coordinateConfig.getOnlineGroundElevationEnabled())) {
            return requireGroundElevationFallback(coordinateConfig);
        }

        try {
            return fetchGroundElevation(coordinateConfig, latitude, longitude);
        } catch (Exception e) {
            double fallback = requireGroundElevationFallback(coordinateConfig);
            log.warn("Failed to fetch ground elevation online, fallback to configured value. lat={}, lon={}, fallback={}",
                    latitude, longitude, fallback, e);
            return fallback;
        }
    }

    double fetchGroundElevation(DataConfig.Coordinate coordinateConfig, double latitude, double longitude) throws IOException {
        String cacheKey = buildCacheKey(latitude, longitude);
        long cacheTtlSeconds = resolveCacheTtlSeconds(coordinateConfig);
        CachedElevation cachedElevation = getCachedElevation(cacheKey, cacheTtlSeconds);
        if (cachedElevation != null) {
            return cachedElevation.getElevationMeters();
        }

        String apiUrl = resolveApiUrl(coordinateConfig);
        String requestUrl = buildElevationUrl(apiUrl, latitude, longitude);
        String responseBody = httpClient.get(requestUrl,
                resolveConnectTimeoutMs(coordinateConfig),
                resolveReadTimeoutMs(coordinateConfig));
        double elevationMeters = parseElevationMeters(responseBody);
        putCachedElevation(cacheKey, elevationMeters, cacheTtlSeconds);
        return elevationMeters;
    }

    static String buildElevationUrl(String apiUrl, double latitude, double longitude) {
        String separator = apiUrl.contains("?") ? "&" : "?";
        return apiUrl + separator + "latitude=" + Double.toString(latitude)
                + "&longitude=" + Double.toString(longitude);
    }

    double parseElevationMeters(String responseBody) throws IOException {
        JsonNode root = objectMapper.readTree(responseBody);
        JsonNode elevationNode = root.path("elevation");
        if (!elevationNode.isArray() || elevationNode.size() == 0 || !elevationNode.get(0).isNumber()) {
            throw new IOException("Elevation API response is missing elevation[0]");
        }
        return elevationNode.get(0).asDouble();
    }

    private double requireGroundElevationFallback(DataConfig.Coordinate coordinateConfig) {
        Double fallback = coordinateConfig.getGroundElevationMeters();
        if (fallback == null) {
            throw new IllegalArgumentException("Coordinate config field is required: groundElevationMeters");
        }
        return fallback;
    }

    private String resolveApiUrl(DataConfig.Coordinate coordinateConfig) {
        String apiUrl = coordinateConfig.getElevationApiUrl();
        if (apiUrl == null || apiUrl.trim().isEmpty()) {
            return DEFAULT_ELEVATION_API_URL;
        }
        return apiUrl.trim();
    }

    private int resolveConnectTimeoutMs(DataConfig.Coordinate coordinateConfig) {
        Integer timeout = coordinateConfig.getElevationConnectTimeoutMs();
        return timeout == null || timeout <= 0 ? DEFAULT_CONNECT_TIMEOUT_MS : timeout;
    }

    private int resolveReadTimeoutMs(DataConfig.Coordinate coordinateConfig) {
        Integer timeout = coordinateConfig.getElevationReadTimeoutMs();
        return timeout == null || timeout <= 0 ? DEFAULT_READ_TIMEOUT_MS : timeout;
    }

    private long resolveCacheTtlSeconds(DataConfig.Coordinate coordinateConfig) {
        Long ttl = coordinateConfig.getElevationCacheTtlSeconds();
        return ttl == null ? DEFAULT_CACHE_TTL_SECONDS : ttl;
    }

    private CachedElevation getCachedElevation(String cacheKey, long cacheTtlSeconds) {
        if (cacheTtlSeconds <= 0) {
            return null;
        }
        CachedElevation cachedElevation = cache.get(cacheKey);
        if (cachedElevation == null) {
            return null;
        }
        long ageMillis = System.currentTimeMillis() - cachedElevation.getCreatedAtMillis();
        if (ageMillis <= cacheTtlSeconds * 1000L) {
            return cachedElevation;
        }
        cache.remove(cacheKey);
        return null;
    }

    private void putCachedElevation(String cacheKey, double elevationMeters, long cacheTtlSeconds) {
        if (cacheTtlSeconds <= 0) {
            return;
        }
        cache.put(cacheKey, new CachedElevation(elevationMeters, System.currentTimeMillis()));
    }

    static String buildCacheKey(double latitude, double longitude) {
        return String.format(Locale.ROOT, "%.5f,%.5f", latitude, longitude);
    }

    interface ElevationHttpClient {
        String get(String requestUrl, int connectTimeoutMs, int readTimeoutMs) throws IOException;
    }

    static class UrlConnectionElevationHttpClient implements ElevationHttpClient {

        @Override
        public String get(String requestUrl, int connectTimeoutMs, int readTimeoutMs) throws IOException {
            HttpURLConnection connection = (HttpURLConnection) new URL(requestUrl).openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(connectTimeoutMs);
            connection.setReadTimeout(readTimeoutMs);
            try {
                int responseCode = connection.getResponseCode();
                if (responseCode < 200 || responseCode >= 300) {
                    throw new IOException("Elevation API HTTP " + responseCode + ": " + readBody(connection.getErrorStream()));
                }
                return readBody(connection.getInputStream());
            } finally {
                connection.disconnect();
            }
        }

        private static String readBody(InputStream inputStream) throws IOException {
            if (inputStream == null) {
                return "";
            }
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    static class CachedElevation {
        private final double elevationMeters;
        private final long createdAtMillis;

        CachedElevation(double elevationMeters, long createdAtMillis) {
            this.elevationMeters = elevationMeters;
            this.createdAtMillis = createdAtMillis;
        }

        double getElevationMeters() {
            return elevationMeters;
        }

        long getCreatedAtMillis() {
            return createdAtMillis;
        }
    }
}
