package com.gosling.bms.service;

import java.util.Map;

public interface SonyCameraService {

    Map<String, Object> getStatus(String mac);

    Map<String, Object> getTimelapseRuntime(String mac);

    Map<String, Object> stop(String mac);

    Map<String, Object> startTimelapseForever(Integer interval, String mac);

    Map<String, Object> startTimelapse(Integer interval, Integer count, String mac);

    Map<String, Object> restartTimelapseForever(Integer interval, String mac);

    Map<String, Object> restartTimelapse(Integer interval, Integer count, String mac);
}
