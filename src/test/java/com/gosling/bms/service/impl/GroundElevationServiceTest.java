package com.gosling.bms.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gosling.bms.conf.DataConfig;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GroundElevationServiceTest {

    @Test
    void shouldBuildOpenMeteoElevationUrl() {
        String url = GroundElevationService.buildElevationUrl("https://api.open-meteo.com/v1/elevation", 30.123456, 114.654321);

        assertEquals("https://api.open-meteo.com/v1/elevation?latitude=30.123456&longitude=114.654321", url);
    }

    @Test
    void shouldParseElevationMetersFromResponse() throws Exception {
        GroundElevationService service = newService(requestUrl -> "{\"elevation\":[38.0]}");

        double elevationMeters = service.parseElevationMeters("{\"elevation\":[38.0]}");

        assertEquals(38.0, elevationMeters, 1e-9);
    }

    @Test
    void shouldFetchOnlineElevationWhenEnabled() {
        DataConfig.Coordinate coordinate = coordinate(true, 0.0);
        GroundElevationService service = newService(requestUrl -> "{\"elevation\":[52.5]}");

        double elevationMeters = service.resolveGroundElevation(coordinate, 30.0, 114.0);

        assertEquals(52.5, elevationMeters, 1e-9);
    }

    @Test
    void shouldFallbackToConfiguredElevationWhenOnlineFetchFails() {
        DataConfig.Coordinate coordinate = coordinate(true, 12.0);
        GroundElevationService service = newService(requestUrl -> {
            throw new IOException("network down");
        });

        double elevationMeters = service.resolveGroundElevation(coordinate, 30.0, 114.0);

        assertEquals(12.0, elevationMeters, 1e-9);
    }

    @Test
    void shouldUseConfiguredElevationWhenOnlineDisabled() {
        DataConfig.Coordinate coordinate = coordinate(false, 18.0);
        GroundElevationService service = newService(requestUrl -> {
            throw new AssertionError("HTTP client should not be called");
        });

        double elevationMeters = service.resolveGroundElevation(coordinate, 30.0, 114.0);

        assertEquals(18.0, elevationMeters, 1e-9);
    }

    @Test
    void shouldCacheOnlineElevationByRoundedCoordinate() {
        AtomicInteger calls = new AtomicInteger();
        DataConfig.Coordinate coordinate = coordinate(true, 0.0);
        GroundElevationService service = newService(requestUrl -> {
            calls.incrementAndGet();
            return "{\"elevation\":[66.0]}";
        });

        double first = service.resolveGroundElevation(coordinate, 30.000001, 114.000001);
        double second = service.resolveGroundElevation(coordinate, 30.000002, 114.000002);

        assertEquals(66.0, first, 1e-9);
        assertEquals(66.0, second, 1e-9);
        assertEquals(1, calls.get());
    }

    private static DataConfig.Coordinate coordinate(boolean onlineEnabled, double fallbackMeters) {
        DataConfig.Coordinate coordinate = new DataConfig.Coordinate();
        coordinate.setOnlineGroundElevationEnabled(onlineEnabled);
        coordinate.setGroundElevationMeters(fallbackMeters);
        coordinate.setElevationApiUrl(GroundElevationService.DEFAULT_ELEVATION_API_URL);
        coordinate.setElevationCacheTtlSeconds(86400L);
        return coordinate;
    }

    private static GroundElevationService newService(FakeHttpClient fakeHttpClient) {
        return new GroundElevationService(new ObjectMapper(), (requestUrl, connectTimeoutMs, readTimeoutMs) ->
                fakeHttpClient.get(requestUrl), new ConcurrentHashMap<>());
    }

    private interface FakeHttpClient {
        String get(String requestUrl) throws IOException;
    }
}
