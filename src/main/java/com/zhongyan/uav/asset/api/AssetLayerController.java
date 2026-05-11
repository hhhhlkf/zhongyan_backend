package com.zhongyan.uav.asset.api;

import com.zhongyan.uav.asset.api.response.AssetLayerView;
import com.zhongyan.uav.asset.application.AssetQueryService;
import com.zhongyan.uav.common.response.ResponseResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@ResponseResult
@RestController
@RequestMapping("/assets/{assetId}/layers")
public class AssetLayerController {
    private final AssetQueryService assetQueryService;

    public AssetLayerController(AssetQueryService assetQueryService) {
        this.assetQueryService = assetQueryService;
    }

    @GetMapping
    public List<AssetLayerView> listLayers(@PathVariable String assetId) {
        return assetQueryService.listLayers(assetId);
    }

    @GetMapping("/{layerId}")
    public AssetLayerView getLayer(@PathVariable String assetId, @PathVariable String layerId) {
        return assetQueryService.getLayer(assetId, layerId);
    }
}
