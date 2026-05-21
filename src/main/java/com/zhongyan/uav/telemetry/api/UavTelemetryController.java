package com.zhongyan.uav.telemetry.api;

import com.zhongyan.uav.common.response.ResponseResult;
import com.zhongyan.uav.telemetry.api.request.IngestTelemetryRequest;
import com.zhongyan.uav.telemetry.api.response.UavTelemetryView;
import com.zhongyan.uav.telemetry.api.response.UavTrackView;
import com.zhongyan.uav.telemetry.application.IngestTelemetryCommand;
import com.zhongyan.uav.telemetry.application.UavTelemetryIngestService;
import com.zhongyan.uav.telemetry.application.UavTelemetryQueryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@ResponseResult
@RestController
@RequestMapping("/uavs/{uavId}")
public class UavTelemetryController {
    private final UavTelemetryIngestService ingestService;
    private final UavTelemetryQueryService queryService;

    public UavTelemetryController(UavTelemetryIngestService ingestService,
                                  UavTelemetryQueryService queryService) {
        this.ingestService = ingestService;
        this.queryService = queryService;
    }

    @PostMapping("/telemetry")
    public UavTelemetryView ingestTelemetry(@PathVariable String uavId,
                                            @RequestBody IngestTelemetryRequest request) {
        return UavTelemetryView.fromDomain(ingestService.ingest(new IngestTelemetryCommand(uavId,
                request.missionId(), request.taskId(), request.latitude(), request.longitude(),
                request.altitudeMeters(), request.speedMetersPerSecond(), request.headingDegrees(),
                request.reportedAt(), request.rawPayload())));
    }

    @GetMapping("/telemetry/latest")
    public UavTelemetryView getLatestTelemetry(@PathVariable String uavId) {
        return UavTelemetryView.fromDomain(queryService.latest(uavId));
    }

    @GetMapping("/telemetry")
    public List<UavTelemetryView> listTelemetry(@PathVariable String uavId,
                                                @RequestParam(required = false) Instant from,
                                                @RequestParam(required = false) Instant to,
                                                @RequestParam(required = false) Integer limit) {
        return queryService.range(uavId, from, to, limit).stream()
                .map(UavTelemetryView::fromDomain)
                .collect(Collectors.toList());
    }

    @GetMapping("/track")
    public UavTrackView getTrack(@PathVariable String uavId,
                                 @RequestParam(required = false) String missionId,
                                 @RequestParam(required = false) Integer limit) {
        return UavTrackView.fromTelemetry(uavId, missionId, queryService.track(uavId, missionId, limit));
    }
}
