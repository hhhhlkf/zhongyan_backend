package com.zhongyan.uav.geo.port;

import com.zhongyan.uav.asset.domain.Asset;
import com.zhongyan.uav.geo.domain.LayerPublishRecord;

import java.util.Map;

public interface GeoServerPort {
    LayerPublishRecord publishLayer(Asset asset, Map<String, Object> parameters);
}
