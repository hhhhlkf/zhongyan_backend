package com.zhongyan.uav.configcenter.api;

import com.zhongyan.uav.common.response.ResponseResult;
import com.zhongyan.uav.configcenter.api.request.CreateTransferConfigRequest;
import com.zhongyan.uav.configcenter.api.request.CreateTransferConfigVersionRequest;
import com.zhongyan.uav.configcenter.api.response.ConfigActionView;
import com.zhongyan.uav.configcenter.api.response.ConfigValidationView;
import com.zhongyan.uav.configcenter.api.response.TransferConfigVersionView;
import com.zhongyan.uav.configcenter.application.ConfigValidationService;
import com.zhongyan.uav.configcenter.application.TransferConfigApplicationService;
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
@RequestMapping("/config/transfers")
public class TransferConfigController {
    private final TransferConfigApplicationService transferConfigApplicationService;
    private final ConfigValidationService configValidationService;

    public TransferConfigController(TransferConfigApplicationService transferConfigApplicationService,
                                    ConfigValidationService configValidationService) {
        this.transferConfigApplicationService = transferConfigApplicationService;
        this.configValidationService = configValidationService;
    }

    @PostMapping
    public TransferConfigVersionView createTransfer(@RequestBody CreateTransferConfigRequest request) {
        return TransferConfigVersionView.from(transferConfigApplicationService.createTransfer(
                request.transferType(), request.endpoint(), request.parameters(), request.createdBy()));
    }

    @GetMapping
    public List<TransferConfigVersionView> listTransfers() {
        return transferConfigApplicationService.listTransfers().stream()
                .map(TransferConfigVersionView::from)
                .collect(Collectors.toList());
    }

    @GetMapping("/{transferConfigId}")
    public TransferConfigVersionView getTransfer(@PathVariable String transferConfigId) {
        return TransferConfigVersionView.from(transferConfigApplicationService.getTransfer(transferConfigId));
    }

    @PostMapping("/{transferConfigId}/versions")
    public TransferConfigVersionView createVersion(@PathVariable String transferConfigId,
                                                   @RequestBody CreateTransferConfigVersionRequest request) {
        return TransferConfigVersionView.from(transferConfigApplicationService.createVersion(transferConfigId,
                request.version(), request.transferType(), request.endpoint(), request.parameters(), request.createdBy()));
    }

    @PostMapping("/{transferConfigId}/validate")
    public ConfigValidationView validateTransfer(@PathVariable String transferConfigId) {
        return ConfigValidationView.from(configValidationService.validateTransfer(transferConfigId));
    }

    @PostMapping("/{transferConfigId}/activate")
    public ConfigActionView activateTransfer(@PathVariable String transferConfigId) {
        transferConfigApplicationService.activateTransfer(transferConfigId);
        return ConfigActionView.accepted("TRANSFER", transferConfigId, "ACTIVATE");
    }

    @PostMapping("/{transferConfigId}/disable")
    public ConfigActionView disableTransfer(@PathVariable String transferConfigId) {
        transferConfigApplicationService.disableTransfer(transferConfigId);
        return ConfigActionView.accepted("TRANSFER", transferConfigId, "DISABLE");
    }
}
