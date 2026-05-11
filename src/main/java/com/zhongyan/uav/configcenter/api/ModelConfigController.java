package com.zhongyan.uav.configcenter.api;

import com.zhongyan.uav.common.response.ResponseResult;
import com.zhongyan.uav.configcenter.api.request.CreateModelArtifactRequest;
import com.zhongyan.uav.configcenter.api.request.CreateModelConfigRequest;
import com.zhongyan.uav.configcenter.api.request.CreateModelConfigVersionRequest;
import com.zhongyan.uav.configcenter.api.response.ConfigActionView;
import com.zhongyan.uav.configcenter.api.response.ConfigValidationView;
import com.zhongyan.uav.configcenter.api.response.ModelArtifactView;
import com.zhongyan.uav.configcenter.api.response.ModelConfigVersionView;
import com.zhongyan.uav.configcenter.application.ConfigValidationService;
import com.zhongyan.uav.configcenter.application.ModelConfigApplicationService;
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
@RequestMapping("/config/models")
public class ModelConfigController {
    private final ModelConfigApplicationService modelConfigApplicationService;
    private final ConfigValidationService configValidationService;

    public ModelConfigController(ModelConfigApplicationService modelConfigApplicationService,
                                 ConfigValidationService configValidationService) {
        this.modelConfigApplicationService = modelConfigApplicationService;
        this.configValidationService = configValidationService;
    }

    @PostMapping
    public ModelConfigVersionView createModel(@RequestBody CreateModelConfigRequest request) {
        return ModelConfigVersionView.from(modelConfigApplicationService.createModel(request.modelType(),
                request.runtimeType(), request.parameters(), request.createdBy()));
    }

    @GetMapping
    public List<ModelConfigVersionView> listModels() {
        return modelConfigApplicationService.listModels().stream()
                .map(ModelConfigVersionView::from)
                .collect(Collectors.toList());
    }

    @GetMapping("/{modelConfigId}")
    public ModelConfigVersionView getModel(@PathVariable String modelConfigId) {
        return ModelConfigVersionView.from(modelConfigApplicationService.getModel(modelConfigId));
    }

    @PostMapping("/{modelConfigId}/versions")
    public ModelConfigVersionView createVersion(@PathVariable String modelConfigId,
                                                @RequestBody CreateModelConfigVersionRequest request) {
        return ModelConfigVersionView.from(modelConfigApplicationService.createVersion(modelConfigId,
                request.version(), request.modelType(), request.runtimeType(), request.parameters(), request.createdBy()));
    }

    @PostMapping("/{modelConfigId}/artifacts")
    public ModelArtifactView createArtifact(@PathVariable String modelConfigId,
                                            @RequestBody CreateModelArtifactRequest request) {
        return ModelArtifactView.from(modelConfigApplicationService.createArtifact(modelConfigId,
                request.artifactType(), request.objectKey(), request.checksum(), request.metadata(), request.createdBy()));
    }

    @PostMapping("/{modelConfigId}/validate")
    public ConfigValidationView validateModel(@PathVariable String modelConfigId) {
        return ConfigValidationView.from(configValidationService.validateModel(modelConfigId));
    }

    @PostMapping("/{modelConfigId}/activate")
    public ConfigActionView activateModel(@PathVariable String modelConfigId) {
        modelConfigApplicationService.activateModel(modelConfigId);
        return ConfigActionView.accepted("MODEL", modelConfigId, "ACTIVATE");
    }

    @PostMapping("/{modelConfigId}/disable")
    public ConfigActionView disableModel(@PathVariable String modelConfigId) {
        modelConfigApplicationService.disableModel(modelConfigId);
        return ConfigActionView.accepted("MODEL", modelConfigId, "DISABLE");
    }
}
