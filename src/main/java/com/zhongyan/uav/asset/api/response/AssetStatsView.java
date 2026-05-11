package com.zhongyan.uav.asset.api.response;

public record AssetStatsView(
        long totalCount,
        long imageCount,
        long videoCount,
        long modelResultCount,
        long reportCount,
        long attachmentCount) {
    /**
     * 构建资产统计零值视图，表示当前 API 空壳不读取存储。
     */
    public static AssetStatsView empty() {
        return new AssetStatsView(0, 0, 0, 0, 0, 0);
    }
}
