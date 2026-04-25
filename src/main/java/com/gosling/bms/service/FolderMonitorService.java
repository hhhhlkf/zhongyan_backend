package com.gosling.bms.service;

public interface FolderMonitorService {

    void startMonitor(String folder1, String folder2);

    void stopMonitor();

    boolean isRunning();
}
