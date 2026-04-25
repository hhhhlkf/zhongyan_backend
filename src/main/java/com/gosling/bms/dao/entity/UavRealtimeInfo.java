package com.gosling.bms.dao.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class UavRealtimeInfo {

    private Integer id;
    private Telemetry data;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Telemetry {
        private Double lon;
        private Double lat;
        private Double alt;
        private Double velo;
        private Double yaw;
        private Double roll;
        private Double pitch;
        private Long timestamp;
    }
}
