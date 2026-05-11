package com.zhongyan.uav.asset.infrastructure.mock;

import com.zhongyan.uav.asset.domain.Asset;
import com.zhongyan.uav.asset.domain.AssetRepository;
import com.zhongyan.uav.asset.domain.AssetStatus;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class InMemoryAssetRepository implements AssetRepository {
    private final Map<String, Asset> assets = new ConcurrentHashMap<>();

    /**
     * 将 Asset 元数据保存到内存 Map。
     */
    @Override
    public Asset save(Asset asset) {
        assets.put(asset.assetId(), asset);
        return asset;
    }

    /**
     * 从内存中按资产编号查找 Asset。
     */
    @Override
    public Optional<Asset> findById(String assetId) {
        return Optional.ofNullable(assets.get(assetId));
    }

    @Override
    public List<Asset> findAll() {
        return assets.values().stream()
                .sorted(Comparator.comparing(Asset::createdAt))
                .collect(Collectors.toList());
    }

    /**
     * 从内存中查询某个 Mission 下的资产。
     */
    @Override
    public List<Asset> findByMissionId(String missionId) {
        return assets.values().stream()
                .filter(asset -> asset.missionId().equals(missionId))
                .sorted(Comparator.comparing(Asset::createdAt))
                .collect(Collectors.toList());
    }

    /**
     * 从内存中查询某个 Task 下的资产。
     */
    @Override
    public List<Asset> findByTaskId(String taskId) {
        return assets.values().stream()
                .filter(asset -> taskId.equals(asset.taskId()))
                .sorted(Comparator.comparing(Asset::createdAt))
                .collect(Collectors.toList());
    }

    /**
     * 从内存中按资产状态筛选资产。
     */
    @Override
    public List<Asset> findByStatus(AssetStatus status) {
        return assets.values().stream()
                .filter(asset -> asset.status() == status)
                .sorted(Comparator.comparing(Asset::createdAt))
                .collect(Collectors.toList());
    }
}
