package com.zhongyan.uav.configcenter.api;

import com.zhongyan.uav.common.response.ResponseResult;
import com.zhongyan.uav.configcenter.api.response.ConfigValidationView;
import com.zhongyan.uav.configcenter.application.ConfigValidationService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@ResponseResult
@RestController
@RequestMapping("/config/validations")
public class ConfigValidationController {
    private final ConfigValidationService configValidationService;

    public ConfigValidationController(ConfigValidationService configValidationService) {
        this.configValidationService = configValidationService;
    }

    @GetMapping("/{validationId}")
    public ConfigValidationView getValidation(@PathVariable String validationId) {
        return ConfigValidationView.from(configValidationService.getValidation(validationId));
    }

    @GetMapping
    public List<ConfigValidationView> listValidations(@RequestParam(required = false) String configType,
                                                      @RequestParam(required = false) String configId) {
        return configValidationService.listValidations(configType, configId).stream()
                .map(ConfigValidationView::from)
                .collect(Collectors.toList());
    }
}
