package com.zhongyan.uav.configcenter.api;

import com.zhongyan.uav.common.response.ResponseResult;
import com.zhongyan.uav.configcenter.api.request.CreateDeviceConfigRequest;
import com.zhongyan.uav.configcenter.api.response.ConfigActionView;
import com.zhongyan.uav.configcenter.api.response.ConfigValidationView;
import com.zhongyan.uav.configcenter.api.response.DeviceConfigView;
import com.zhongyan.uav.configcenter.application.ConfigValidationService;
import com.zhongyan.uav.configcenter.application.DeviceConfigApplicationService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@ResponseResult
@RestController
@RequestMapping("/config/devices")
public class DeviceConfigController {
    private final DeviceConfigApplicationService deviceConfigApplicationService;
    private final ConfigValidationService configValidationService;

    public DeviceConfigController(DeviceConfigApplicationService deviceConfigApplicationService,
                                  ConfigValidationService configValidationService) {
        this.deviceConfigApplicationService = deviceConfigApplicationService;
        this.configValidationService = configValidationService;
    }

    @PostMapping
    public DeviceConfigView createDevice(@RequestBody CreateDeviceConfigRequest request) {
        return DeviceConfigView.from(deviceConfigApplicationService.createDevice(request.deviceId(),
                request.deviceName(), request.deviceType(), request.host(), request.connection(),
                request.capabilities(), request.createdBy()));
    }

    @GetMapping
    public List<DeviceConfigView> listDevices() {
        return deviceConfigApplicationService.listDevices().stream()
                .map(DeviceConfigView::from)
                .collect(Collectors.toList());
    }

    @GetMapping("/{deviceId}")
    public DeviceConfigView getDevice(@PathVariable String deviceId) {
        return DeviceConfigView.from(deviceConfigApplicationService.getDevice(deviceId));
    }

    @PostMapping("/{deviceId}/validate")
    public ConfigValidationView validateDevice(@PathVariable String deviceId) {
        return ConfigValidationView.from(configValidationService.validateDevice(deviceId));
    }

    @PostMapping("/{deviceId}/disable")
    public ConfigActionView disableDevice(@PathVariable String deviceId) {
        deviceConfigApplicationService.disableDevice(deviceId);
        return ConfigActionView.accepted("DEVICE", deviceId, "DISABLE");
    }
}
