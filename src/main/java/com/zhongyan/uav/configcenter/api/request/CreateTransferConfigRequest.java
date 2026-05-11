package com.zhongyan.uav.configcenter.api.request;

import java.util.Map;

public record CreateTransferConfigRequest(
        String transferName,
        String transferType,
        Map<String, Object> endpoint,
        Map<String, Object> parameters,
        String createdBy) {
}
