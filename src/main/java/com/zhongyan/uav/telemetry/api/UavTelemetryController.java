package com.zhongyan.uav.telemetry.api;

import com.zhongyan.uav.common.response.ResponseResult;
import com.zhongyan.uav.telemetry.api.request.IngestTelemetryRequest;
import com.zhongyan.uav.telemetry.api.response.UavTelemetryView;
import com.zhongyan.uav.telemetry.api.response.UavTrackView;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@ResponseResult
@RestController
@RequestMapping("/uavs/{uavId}")
public class UavTelemetryController {
    /**
     * 接收遥测数据空壳响应，当前不写数据库、不投递 Kafka。
     */
    @PostMapping("/telemetry")
    public UavTelemetryView ingestTelemetry(@PathVariable String uavId,
                                            @RequestBody IngestTelemetryRequest request) {
        return UavTelemetryView.placeholder("telemetry-" + UUID.randomUUID(), uavId,
                request.latitude(), request.longitude(), request.altitudeMeters());
    }

    /**
     * 查询无人机最新遥测空壳，当前返回路径参数组成的占位视图。
     */
    @GetMapping("/telemetry/latest")
    public UavTelemetryView getLatestTelemetry(@PathVariable String uavId) {
        return UavTelemetryView.placeholder("telemetry-latest", uavId, null, null, null);
    }

    /**
     * 查询无人机遥测列表空壳，当前不读取数据库。
     */
    @GetMapping("/telemetry")
    public List<UavTelemetryView> listTelemetry(@PathVariable String uavId,
                                                @RequestParam(required = false) String from,
                                                @RequestParam(required = false) String to) {
        return List.of();
    }

    /**
     * 查询无人机轨迹空壳，当前不读取历史遥测。
     */
    @GetMapping("/track")
    public UavTrackView getTrack(@PathVariable String uavId,
                                 @RequestParam(required = false) String missionId) {
        return UavTrackView.empty(uavId, missionId);
    }
}
