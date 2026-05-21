package com.zhongyan.uav.telemetry.infrastructure;

import com.fasterxml.jackson.databind.json.JsonMapper;
import com.zhongyan.uav.telemetry.application.IngestTelemetryCommand;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TelemetryMessageParserTests {
    private final TelemetryMessageParser parser = new TelemetryMessageParser(JsonMapper.builder()
            .findAndAddModules()
            .build());

    @Test
    void parsesRawTelemetryPayload() {
        IngestTelemetryCommand command = parser.parse(null, """
                {"uavId":"uav-parser","missionId":"mission-parser","latitude":30.1,"longitude":104.1,
                "altitudeMeters":120.0,"speedMps":12.5,"headingDegrees":90.0,
                "reportedAt":"2026-04-29T00:00:00Z"}
                """);

        assertThat(command.uavId()).isEqualTo("uav-parser");
        assertThat(command.speedMetersPerSecond()).isEqualTo(12.5);
        assertThat(command.reportedAt()).hasToString("2026-04-29T00:00:00Z");
    }

    @Test
    void parsesTelemetryPayloadFromEventEnvelope() {
        String message = """
                {"eventId":"event-1","eventType":"UAV_TELEMETRY","payload":{"uavId":"uav-envelope",
                "latitude":30.2,"longitude":104.2,"recordedAt":"2026-04-29T00:01:00Z"}}
                """;

        assertThat(parser.isEventEnvelope(message)).isTrue();
        assertThat(parser.parse(null, message).uavId()).isEqualTo("uav-envelope");
    }
}
