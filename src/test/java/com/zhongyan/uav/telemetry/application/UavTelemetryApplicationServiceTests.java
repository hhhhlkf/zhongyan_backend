package com.zhongyan.uav.telemetry.application;

import com.zhongyan.uav.event.application.OutboxPublishService;
import com.zhongyan.uav.event.domain.EventType;
import com.zhongyan.uav.event.infrastructure.mock.InMemoryEventPublisher;
import com.zhongyan.uav.event.infrastructure.mock.InMemoryOutboxRepository;
import com.zhongyan.uav.realtime.infrastructure.SseRealtimePushService;
import com.zhongyan.uav.telemetry.domain.UavTelemetry;
import com.zhongyan.uav.telemetry.infrastructure.mock.InMemoryUavTelemetryRepository;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class UavTelemetryApplicationServiceTests {
    private static final Instant BASE_TIME = Instant.parse("2026-04-29T00:00:00Z");

    private final InMemoryUavTelemetryRepository telemetryRepository = new InMemoryUavTelemetryRepository();
    private final InMemoryOutboxRepository outboxRepository = new InMemoryOutboxRepository();
    private final UavTelemetryIngestService ingestService = new UavTelemetryIngestService(telemetryRepository,
            new OutboxPublishService(outboxRepository, new InMemoryEventPublisher(), new SseRealtimePushService(),
                    Clock.fixed(BASE_TIME, ZoneOffset.UTC)));
    private final UavTelemetryQueryService queryService = new UavTelemetryQueryService(telemetryRepository,
            Clock.fixed(BASE_TIME.plusSeconds(3600), ZoneOffset.UTC));

    @Test
    void ingestsAndQueriesLatestRangeAndTrack() {
        ingest("uav-telemetry-test", "mission-a", BASE_TIME.plusSeconds(20), 30.2, 104.2);
        ingest("uav-telemetry-test", "mission-a", BASE_TIME.plusSeconds(30), 30.3, 104.3);
        UavTelemetry latest = ingest("uav-telemetry-test", "mission-b", BASE_TIME.plusSeconds(40), null, null);

        assertThat(queryService.latest("uav-telemetry-test")).isEqualTo(latest);
        assertThat(queryService.range("uav-telemetry-test", BASE_TIME.plusSeconds(10),
                BASE_TIME.plusSeconds(35), null)).extracting(UavTelemetry::latitude)
                .containsExactly(30.2, 30.3);
        assertThat(queryService.track("uav-telemetry-test", "mission-a", null))
                .extracting(UavTelemetry::longitude)
                .containsExactly(104.2, 104.3);
        assertThat(outboxRepository.findByEventType(EventType.UAV_TELEMETRY, 10)).hasSize(3);
    }

    @Test
    void handlesHighFrequencyTelemetryInMemory() {
        for (int index = 0; index < 2000; index++) {
            ingest("uav-load-test", "mission-load", BASE_TIME.plusMillis(index),
                    30.0 + index * 0.00001, 104.0 + index * 0.00001);
        }

        List<UavTelemetry> range = queryService.range("uav-load-test", BASE_TIME,
                BASE_TIME.plusSeconds(1), 5000);
        assertThat(range).hasSize(1001);
        assertThat(queryService.track("uav-load-test", "mission-load", 5000)).hasSize(2000);
        assertThat(queryService.latest("uav-load-test").latitude()).isEqualTo(30.0 + 1999 * 0.00001);
    }

    private UavTelemetry ingest(String uavId, String missionId, Instant reportedAt,
                                Double latitude, Double longitude) {
        return ingestService.ingest(new IngestTelemetryCommand(uavId, missionId, null,
                latitude, longitude, 120.0, 15.0, 88.0, reportedAt,
                Map.of("source", "test")));
    }
}
