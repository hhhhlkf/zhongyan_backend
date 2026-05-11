package com.zhongyan.uav.asset.infrastructure.mock;

import com.zhongyan.uav.asset.domain.TaskAsset;
import com.zhongyan.uav.asset.domain.TaskAssetRepository;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class InMemoryTaskAssetRepository implements TaskAssetRepository {
    private final Map<String, TaskAsset> taskAssets = new ConcurrentHashMap<>();

    @Override
    public TaskAsset save(TaskAsset taskAsset) {
        taskAssets.put(key(taskAsset), taskAsset);
        return taskAsset;
    }

    @Override
    public List<TaskAsset> findByTaskId(String taskId) {
        return taskAssets.values().stream()
                .filter(taskAsset -> taskAsset.taskId().equals(taskId))
                .sorted(Comparator.comparing(TaskAsset::boundAt))
                .collect(Collectors.toList());
    }

    @Override
    public List<TaskAsset> findByAssetId(String assetId) {
        return taskAssets.values().stream()
                .filter(taskAsset -> taskAsset.assetId().equals(assetId))
                .sorted(Comparator.comparing(TaskAsset::boundAt))
                .collect(Collectors.toList());
    }

    private String key(TaskAsset taskAsset) {
        return taskAsset.taskId() + ":" + taskAsset.assetId() + ":" + taskAsset.role();
    }
}
