package com.zhongyan.uav.asset.port;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AssetObjectTests {
    @Test
    void usesBinaryContentTypeWhenContentTypeIsBlank() {
        AssetObject object = new AssetObject(
                "missions/m-1/image.png",
                new ByteArrayInputStream(new byte[]{1, 2, 3}),
                3,
                "",
                Map.of("assetType", "IMAGE"));

        assertThat(object.contentType()).isEqualTo("application/octet-stream");
        assertThat(object.metadata()).containsEntry("assetType", "IMAGE");
    }

    @Test
    void rejectsBlankObjectKey() {
        assertThatThrownBy(() -> new AssetObject(
                " ",
                new ByteArrayInputStream(new byte[0]),
                0,
                "text/plain",
                Map.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("objectKey");
    }
}
