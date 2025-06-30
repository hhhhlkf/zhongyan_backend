package com.gosling.bms.conf;

import lombok.Data;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;


@Data
@Component
@ConfigurationProperties(prefix = "server-param")
public class CameraConfig {

    @Setter
    public static int transStatus = 1; // 传输状态，0表示开始，1表示未开始

    private List<CameraInfo> cameras;

    private Transfer transfer;

    @Data
    public static class CameraInfo {
        private String host;
        private String username;
        private String password;
        private String type;
        private String checkCommand;
        private String startCommand;
        private String stopCommand;

        private String startProcess;
        private String stopProcess;
    }

    @Data
    public static class Transfer{
        private String host;
        private String username;
        private String password;
        private String checkCommand;
        private String startCommand;
        private String stopCommand;

    }
}
