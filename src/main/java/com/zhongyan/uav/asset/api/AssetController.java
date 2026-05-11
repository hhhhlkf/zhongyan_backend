package com.zhongyan.uav.asset.api;

import com.zhongyan.uav.asset.api.request.AssetActionRequest;
import com.zhongyan.uav.asset.api.request.CreateAssetRequest;
import com.zhongyan.uav.asset.api.response.AssetActionView;
import com.zhongyan.uav.asset.api.response.AssetStatsView;
import com.zhongyan.uav.asset.api.response.AssetView;
import com.zhongyan.uav.asset.application.AssetApplicationService;
import com.zhongyan.uav.asset.application.AssetQueryService;
import com.zhongyan.uav.asset.application.CreateAssetInput;
import com.zhongyan.uav.asset.application.PreviewApplicationService;
import com.zhongyan.uav.common.response.ResponseResult;
import com.zhongyan.uav.geo.application.GeoBoundaryApplicationService;
import com.zhongyan.uav.geo.application.LayerPublishApplicationService;
import com.zhongyan.uav.geo.domain.LayerPublishRecord;
import com.zhongyan.uav.geo.domain.LayerStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@ResponseResult
@RestController
@RequestMapping("/assets")
public class AssetController {
    private final AssetApplicationService assetApplicationService;
    private final AssetQueryService assetQueryService;
    private final PreviewApplicationService previewApplicationService;
    private final GeoBoundaryApplicationService geoBoundaryApplicationService;
    private final LayerPublishApplicationService layerPublishApplicationService;

    public AssetController(AssetApplicationService assetApplicationService,
                           AssetQueryService assetQueryService,
                           PreviewApplicationService previewApplicationService,
                           GeoBoundaryApplicationService geoBoundaryApplicationService,
                           LayerPublishApplicationService layerPublishApplicationService) {
        this.assetApplicationService = assetApplicationService;
        this.assetQueryService = assetQueryService;
        this.previewApplicationService = previewApplicationService;
        this.geoBoundaryApplicationService = geoBoundaryApplicationService;
        this.layerPublishApplicationService = layerPublishApplicationService;
    }

    @PostMapping
    public AssetView createAsset(@RequestBody CreateAssetRequest request) {
        return assetQueryService.toView(assetApplicationService.createAsset(new CreateAssetInput(
                request.missionId(), request.taskId(), request.assetType(), request.role(), request.name(),
                request.objectKey(), request.contentType(), request.sizeBytes() == null ? 0 : request.sizeBytes(),
                request.checksum(), request.metadata(), request.createdBy())));
    }

    @GetMapping
    public List<AssetView> listAssets(@RequestParam(required = false) String missionId,
                                      @RequestParam(required = false) String taskId,
                                      @RequestParam(required = false) String status) {
        return assetQueryService.listAssets(missionId, taskId, status);
    }

    @GetMapping("/{assetId}")
    public AssetView getAsset(@PathVariable String assetId) {
        return assetQueryService.getAsset(assetId);
    }

    @PostMapping("/{assetId}/preview")
    public AssetActionView generatePreview(@PathVariable String assetId,
                                           @RequestBody(required = false) AssetActionRequest request) {
        previewApplicationService.generatePreview(assetId, requestedBy(request), parameters(request));
        return AssetActionView.accepted(assetId, "GENERATE_PREVIEW");
    }

    @PostMapping("/{assetId}/geo-boundary")
    public AssetActionView calculateGeoBoundary(@PathVariable String assetId,
                                                @RequestBody(required = false) AssetActionRequest request) {
        geoBoundaryApplicationService.calculateBoundary(assetId, parameters(request));
        return AssetActionView.accepted(assetId, "CALCULATE_GEO_BOUNDARY");
    }

    @PostMapping("/{assetId}/publish-layer")
    public AssetActionView publishLayer(@PathVariable String assetId,
                                        @RequestBody(required = false) AssetActionRequest request) {
        LayerPublishRecord record = layerPublishApplicationService.publishLayer(assetId, requestedBy(request),
                parameters(request));
        String status = record.status() == LayerStatus.PENDING_APPROVAL ? "PENDING_APPROVAL" : "ACCEPTED";
        return new AssetActionView(assetId, "PUBLISH_LAYER", status, Instant.now());
    }

    @DeleteMapping("/{assetId}")
    public AssetActionView deleteAsset(@PathVariable String assetId) {
        assetApplicationService.findRequired(assetId);
        return new AssetActionView(assetId, "DELETE", "PENDING_APPROVAL", Instant.now());
    }

    @GetMapping("/stats")
    public AssetStatsView getStats() {
        return assetQueryService.getStats();
    }

    private String requestedBy(AssetActionRequest request) {
        return request == null ? null : request.requestedBy();
    }

    private Map<String, Object> parameters(AssetActionRequest request) {
        return request == null || request.parameters() == null ? Map.of() : request.parameters();
    }
}
