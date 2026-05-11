package com.zhongyan.uav.configcenter.api;

import com.zhongyan.uav.common.response.ResponseResult;
import com.zhongyan.uav.configcenter.api.request.CreateCameraConfigRequest;
import com.zhongyan.uav.configcenter.api.request.CreateCameraConfigVersionRequest;
import com.zhongyan.uav.configcenter.api.response.CameraConfigVersionView;
import com.zhongyan.uav.configcenter.api.response.ConfigActionView;
import com.zhongyan.uav.configcenter.api.response.ConfigValidationView;
import com.zhongyan.uav.configcenter.application.CameraConfigApplicationService;
import com.zhongyan.uav.configcenter.application.ConfigValidationService;
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
@RequestMapping("/config/cameras")
public class CameraConfigController {
    private final CameraConfigApplicationService cameraConfigApplicationService;
    private final ConfigValidationService configValidationService;

    public CameraConfigController(CameraConfigApplicationService cameraConfigApplicationService,
                                  ConfigValidationService configValidationService) {
        this.cameraConfigApplicationService = cameraConfigApplicationService;
        this.configValidationService = configValidationService;
    }

    @PostMapping
    public CameraConfigVersionView createCamera(@RequestBody CreateCameraConfigRequest request) {
        return CameraConfigVersionView.from(cameraConfigApplicationService.createCamera(
                request.cameraType(), request.fov(), request.parameters(), request.createdBy()));
    }

    @GetMapping
    public List<CameraConfigVersionView> listCameras() {
        return cameraConfigApplicationService.listCameras().stream()
                .map(CameraConfigVersionView::from)
                .collect(Collectors.toList());
    }

    @GetMapping("/{cameraConfigId}")
    public CameraConfigVersionView getCamera(@PathVariable String cameraConfigId) {
        return CameraConfigVersionView.from(cameraConfigApplicationService.getCamera(cameraConfigId));
    }

    @PostMapping("/{cameraConfigId}/versions")
    public CameraConfigVersionView createVersion(@PathVariable String cameraConfigId,
                                                 @RequestBody CreateCameraConfigVersionRequest request) {
        return CameraConfigVersionView.from(cameraConfigApplicationService.createVersion(cameraConfigId,
                request.version(), request.cameraType(), request.fov(), request.parameters(), request.createdBy()));
    }

    @PostMapping("/{cameraConfigId}/validate")
    public ConfigValidationView validateCamera(@PathVariable String cameraConfigId) {
        return ConfigValidationView.from(configValidationService.validateCamera(cameraConfigId));
    }

    @PostMapping("/{cameraConfigId}/activate")
    public ConfigActionView activateCamera(@PathVariable String cameraConfigId) {
        cameraConfigApplicationService.activateCamera(cameraConfigId);
        return ConfigActionView.accepted("CAMERA", cameraConfigId, "ACTIVATE");
    }

    @PostMapping("/{cameraConfigId}/disable")
    public ConfigActionView disableCamera(@PathVariable String cameraConfigId) {
        cameraConfigApplicationService.disableCamera(cameraConfigId);
        return ConfigActionView.accepted("CAMERA", cameraConfigId, "DISABLE");
    }
}
