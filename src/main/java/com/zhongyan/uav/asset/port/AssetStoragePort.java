package com.zhongyan.uav.asset.port;

import java.util.Optional;
import java.io.InputStream;
import java.time.Duration;

public interface AssetStoragePort {
    StoredObject put(AssetObject object);

    Optional<StoredObject> get(String objectKey);

    InputStream read(String objectKey);

    void delete(String objectKey);

    String createReadUrl(String objectKey, Duration expiry);
}
