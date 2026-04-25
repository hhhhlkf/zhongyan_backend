package com.gosling.bms.conf;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Data
@Component
@ConfigurationProperties(prefix = "resource")
public class DataConfig {
    private String basePath;
    private List<Receive> receive;
    private String collect;
    private String process;
    private String history;
    private String time;
    private String result;
    private String webPath;
    private Preview preview;
    private Coordinate coordinate;

    @Data
    public static class Receive {
        private String type;
        private String path;
    }

    @Data
    public static class Preview {
        private boolean enabled;
        private PreviewItem collect;
        private PreviewItem process;
    }

    @Data
    public static class PreviewItem {
        private Integer longEdge;
        private Integer targetSizeKb;
        private Double minQuality;
        private Integer retainCount;
    }

    @Data
    public static class Coordinate {
        private Double groundElevationMeters;
        private Boolean onlineGroundElevationEnabled;
        private String elevationApiUrl;
        private Integer elevationConnectTimeoutMs;
        private Integer elevationReadTimeoutMs;
        private Long elevationCacheTtlSeconds;
        private Double horizontalFovDeg;
        private Double verticalFovDeg;
        private Boolean useYaw;
        private Long maxMatchSeconds;
    }
}
