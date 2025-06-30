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
    private String result;
    private String webPath;

    @Data
    public static class Receive {
        private String type;
        private String path;
    }
}