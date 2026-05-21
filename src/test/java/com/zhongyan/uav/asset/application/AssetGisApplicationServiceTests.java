package com.zhongyan.uav.asset.application;

import com.zhongyan.uav.asset.domain.Asset;
import com.zhongyan.uav.asset.domain.AssetRole;
import com.zhongyan.uav.asset.domain.AssetType;
import com.zhongyan.uav.asset.domain.GeoStatus;
import com.zhongyan.uav.asset.infrastructure.mock.InMemoryAssetRepository;
import com.zhongyan.uav.asset.infrastructure.mock.InMemoryTaskAssetRepository;
import com.zhongyan.uav.asset.port.PreviewResult;
import com.zhongyan.uav.event.application.EventReplayService;
import com.zhongyan.uav.event.application.OutboxPublishService;
import com.zhongyan.uav.event.domain.EventEnvelope;
import com.zhongyan.uav.event.domain.EventType;
import com.zhongyan.uav.event.infrastructure.mock.InMemoryEventPublisher;
import com.zhongyan.uav.event.infrastructure.mock.InMemoryOutboxRepository;
import com.zhongyan.uav.geo.application.GeoBoundaryApplicationService;
import com.zhongyan.uav.geo.application.LayerPublishApplicationService;
import com.zhongyan.uav.geo.domain.LayerStatus;
import com.zhongyan.uav.geo.infrastructure.mock.MockGeoServerPort;
import com.zhongyan.uav.geo.infrastructure.mock.MockGroundElevationPort;
import com.zhongyan.uav.realtime.infrastructure.SseRealtimePushService;
import com.zhongyan.uav.task.domain.TaskEventType;
import com.zhongyan.uav.task.infrastructure.mock.InMemoryTaskEventRepository;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AssetGisApplicationServiceTests {
    private final Clock clock = Clock.fixed(Instant.parse("2026-05-17T00:00:00Z"), ZoneOffset.UTC);

    @Test
    void assetPreviewGeoAndLayerFlowKeepsApprovalBoundary() {
        InMemoryAssetRepository assetRepository = new InMemoryAssetRepository();
        AssetApplicationService assetApplicationService = new AssetApplicationService(
                assetRepository, new InMemoryTaskAssetRepository(), clock);
        PreviewApplicationService previewApplicationService = new PreviewApplicationService(
                assetRepository,
                request -> new PreviewResult(request.previewObjectKey(), "image/jpeg", 512, "sha256:preview"),
                clock);
        GeoBoundaryApplicationService geoBoundaryApplicationService = new GeoBoundaryApplicationService(
                assetRepository, new MockGroundElevationPort(), clock);
        LayerPublishApplicationService layerPublishApplicationService = new LayerPublishApplicationService(
                assetRepository, new MockGeoServerPort(), clock);

        Asset created = assetApplicationService.createAsset(new CreateAssetInput(
                "mission-asset-gis",
                "task-asset-gis",
                "IMAGE",
                "OUTPUT",
                "rgb-001.jpg",
                "raw/rgb-001.jpg",
                "image/jpeg",
                1024,
                "sha256:raw",
                Map.of("latitude", 30.1, "longitude", 120.1),
                "tester"));

        Asset previewed = previewApplicationService.generatePreview(created.assetId(), "tester", Map.of());
        Asset geoCalculated = geoBoundaryApplicationService.calculateBoundary(previewed.assetId(), Map.of());
        var pending = layerPublishApplicationService.publishLayer(geoCalculated.assetId(), "tester", Map.of());
        var published = layerPublishApplicationService.publishLayer(geoCalculated.assetId(), "tester",
                Map.of("approved", true));

        assertThat(created.geoStatus()).isEqualTo(GeoStatus.PENDING);
        assertThat(previewed.previewObjectKey()).isEqualTo("previews/" + created.assetId() + ".jpg");
        assertThat(geoCalculated.geoStatus()).isEqualTo(GeoStatus.CALCULATED);
        assertThat(geoCalculated.metadata()).containsKey("geometry");
        assertThat(pending.status()).isEqualTo(LayerStatus.PENDING_APPROVAL);
        assertThat(pending.publishedAt()).isNull();
        assertThat(published.status()).isEqualTo(LayerStatus.PUBLISHED);
        assertThat(assetRepository.findById(created.assetId())).isPresent()
                .get()
                .extracting(Asset::layerUrl)
                .isEqualTo(published.layerUrl());
    }

    @Test
    void geoCalculationFailureMarksGeoFailedWithoutBlockingAvailableAsset() {
        InMemoryAssetRepository assetRepository = new InMemoryAssetRepository();
        Asset asset = Asset.created("asset-geo-failed", "mission-geo", null, AssetType.IMAGE, AssetRole.OUTPUT,
                "broken-geo.jpg", "raw/broken-geo.jpg", "image/jpeg", 1, "sha256:broken",
                Map.of("latitude", "bad-number", "longitude", 120.1), "tester", clock.instant())
                .markAvailable(clock.instant());
        assetRepository.save(asset);

        GeoBoundaryApplicationService service = new GeoBoundaryApplicationService(
                assetRepository,
                (latitude, longitude) -> 0d,
                clock);

        Asset result = service.calculateBoundary(asset.assetId(), Map.of());

        assertThat(result.status()).isEqualTo(asset.status());
        assertThat(result.geoStatus()).isEqualTo(GeoStatus.FAILED);
        assertThat(result.metadata()).containsKey("geoError");
    }

    @Test
    void assetGisActionsWriteAssetEventsAndTaskEventsForReplay() {
        InMemoryAssetRepository assetRepository = new InMemoryAssetRepository();
        InMemoryOutboxRepository outboxRepository = new InMemoryOutboxRepository();
        InMemoryTaskEventRepository taskEventRepository = new InMemoryTaskEventRepository(outboxRepository);
        OutboxPublishService outboxPublishService = new OutboxPublishService(outboxRepository,
                new InMemoryEventPublisher(), new SseRealtimePushService(), clock);
        AssetEventRecorder eventRecorder = new AssetEventRecorder(outboxPublishService, taskEventRepository, clock);
        AssetApplicationService assetApplicationService = new AssetApplicationService(
                assetRepository, new InMemoryTaskAssetRepository(), eventRecorder, clock);
        GeoBoundaryApplicationService geoBoundaryApplicationService = new GeoBoundaryApplicationService(
                assetRepository, new MockGroundElevationPort(), eventRecorder, clock);
        LayerPublishApplicationService layerPublishApplicationService = new LayerPublishApplicationService(
                assetRepository, new MockGeoServerPort(), eventRecorder, clock);

        Asset created = assetApplicationService.createAsset(new CreateAssetInput(
                "mission-event-gis",
                "task-event-gis",
                "IMAGE",
                "OUTPUT",
                "rgb-event.jpg",
                "raw/rgb-event.jpg",
                "image/jpeg",
                2048,
                "sha256:event",
                Map.of("latitude", 30.1, "longitude", 120.1),
                "tester"));
        Asset geoCalculated = geoBoundaryApplicationService.calculateBoundary(created.assetId(), Map.of());
        layerPublishApplicationService.publishLayer(geoCalculated.assetId(), "tester", Map.of());
        layerPublishApplicationService.publishLayer(geoCalculated.assetId(), "tester", Map.of("approved", true));

        assertThat(outboxRepository.findByEventType(EventType.ASSET_EVENT, 10))
                .extracting(event -> event.payload().get("action"))
                .containsExactlyInAnyOrder("CREATED", "GEO_CALCULATED",
                        "LAYER_PENDING_APPROVAL", "LAYER_PUBLISHED");
        assertThat(taskEventRepository.findByTaskId("task-event-gis"))
                .extracting(event -> event.eventType())
                .containsExactlyInAnyOrder(TaskEventType.ASSET_CREATED, TaskEventType.GEO_CALCULATED,
                        TaskEventType.LAYER_PENDING_APPROVAL, TaskEventType.LAYER_PUBLISHED);

        EventReplayService replayService = new EventReplayService(outboxRepository, new SseRealtimePushService());
        assertThat(replayService.replayAggregate("ASSET", created.assetId()))
                .extracting(EventEnvelope::eventType)
                .containsExactlyInAnyOrder(EventType.ASSET_EVENT, EventType.ASSET_EVENT,
                        EventType.ASSET_EVENT, EventType.ASSET_EVENT);
    }
}
