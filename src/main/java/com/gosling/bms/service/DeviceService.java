package com.gosling.bms.service;

import java.util.ArrayList;
import java.util.Map;

public interface DeviceService {
    /**
     * 获取设备状态
     *
     * @return 设备状态映射，键为设备名称，值为设备状态（如在线、离线等）
     */
    Map<String, Float>getDeviceStatus();

    ArrayList<Integer>getDeviceStatus(ArrayList<String> deviceList);
}
