package com.zhongyan.uav.asset.infrastructure.mock;

import com.zhongyan.uav.asset.port.AssetObject;
import com.zhongyan.uav.asset.port.AssetStoragePort;
import com.zhongyan.uav.asset.port.StoredObject;
import com.zhongyan.uav.common.error.BusinessException;
import com.zhongyan.uav.common.error.ErrorCode;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryAssetStorage implements AssetStoragePort {
    private final Map<String, StoredAssetObject> objects = new ConcurrentHashMap<>();

    @Override
    public StoredObject put(AssetObject object) {
        try {
            byte[] content = object.content().readAllBytes();
            StoredObject stored = new StoredObject("memory", object.objectKey(), content.length,
                    object.contentType(), "memory-" + content.length, Instant.now(), object.metadata());
            objects.put(object.objectKey(), new StoredAssetObject(stored, content));
            return stored;
        } catch (IOException exception) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "failed to read asset content", exception);
        }
    }

    @Override
    public Optional<StoredObject> get(String objectKey) {
        return Optional.ofNullable(objects.get(objectKey)).map(StoredAssetObject::storedObject);
    }

    @Override
    public InputStream read(String objectKey) {
        StoredAssetObject object = objects.get(objectKey);
        if (object == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "asset object not found: " + objectKey);
        }
        return new ByteArrayInputStream(object.content());
    }

    @Override
    public void delete(String objectKey) {
        objects.remove(objectKey);
    }

    @Override
    public String createReadUrl(String objectKey, Duration expiry) {
        return "/v2/assets/objects/" + objectKey;
    }

    private record StoredAssetObject(StoredObject storedObject, byte[] content) {
    }
}
