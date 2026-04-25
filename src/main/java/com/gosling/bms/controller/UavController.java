package com.gosling.bms.controller;

import com.gosling.bms.dao.entity.UavRealtimeInfo;
import com.gosling.bms.response.ResponseResult;
import com.gosling.bms.service.UavService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class UavController {

    private final UavService uavService;

    @ResponseResult
    @GetMapping("/uav/info/realtime")
    public UavRealtimeInfo getRealtimeUavInfo() {
        return uavService.getRealtimeInfo();
    }
}
