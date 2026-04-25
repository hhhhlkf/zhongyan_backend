package com.gosling.bms.service.impl;

import com.gosling.bms.conf.CameraConfig;
import com.gosling.bms.exception.BaseException;
import com.gosling.bms.service.DeviceService;
import com.gosling.bms.service.SonyCameraService;
import com.jcraft.jsch.JSch;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@Slf4j
public class DeviceServiceImpl implements DeviceService {
    private static final int DEFAULT_PING_TIMEOUT_MS = 1500;
    private static final int DEFAULT_SSH_TIMEOUT_MS = 3000;

    private final CameraConfig cameraConfig;
    private final SonyCameraService sonyCameraService;

    public DeviceServiceImpl(CameraConfig cameraConfig, SonyCameraService sonyCameraService) {
        this.cameraConfig = cameraConfig;
        this.sonyCameraService = sonyCameraService;
    }

    @Override
    public Map<String, Float> getDeviceStatus() {
        HashMap<String, Float> statusMap = new HashMap<>();
        List<CameraConfig.CameraInfo> cameraList = cameraConfig.getCameras();
        if (cameraList == null || cameraList.isEmpty()) {
            throw new BaseException("No cameras configured.");
        }

        for (int i = 0; i < cameraList.size(); i++) {
            CameraConfig.CameraInfo camera = cameraList.get(i);
            log.info("Checking status for camera: {}", camera.getHost());
            String command = "free | awk '/Mem:/ {printf(\"%.4f\", $3/$2)}'";
            JSch jsch = new JSch();

            try {
                var session = jsch.getSession(camera.getUsername(), camera.getHost(), 22);
                session.setPassword(camera.getPassword());
                session.setConfig("StrictHostKeyChecking", "no");
                session.connect();

                var channel = session.openChannel("exec");
                ((com.jcraft.jsch.ChannelExec) channel).setCommand(command);
                channel.setInputStream(null);
                var in = channel.getInputStream();
                channel.connect();

                StringBuilder output = new StringBuilder();
                byte[] buffer = new byte[1024];
                int read;
                while ((read = in.read(buffer)) != -1) {
                    output.append(new String(buffer, 0, read));
                }

                if (output.length() == 0) {
                    throw new BaseException("No output received from camera: " + camera.getHost());
                }

                log.info("Output: {}", output.toString().trim());
                statusMap.put("nx_" + (i + 1), Float.parseFloat(output.toString().trim()));

                channel.disconnect();
                session.disconnect();
            } catch (Exception e) {
                throw new BaseException("Error checking camera status: " + camera.getHost(), e);
            }
        }

        return statusMap;
    }

    public String execCommand(String host, String username, String password, String command, Integer timeoutMs) {
        JSch jsch = new JSch();
        int connectTimeoutMs = timeoutMs != null && timeoutMs > 0 ? timeoutMs : DEFAULT_SSH_TIMEOUT_MS;
        try {
            var session = jsch.getSession(username, host, 22);
            session.setPassword(password);
            session.setConfig("StrictHostKeyChecking", "no");
            session.connect(connectTimeoutMs);

            var channel = (com.jcraft.jsch.ChannelExec) session.openChannel("exec");
            channel.setCommand(command);
            channel.setInputStream(null);
            var in = channel.getInputStream();
            channel.connect(connectTimeoutMs);

            StringBuilder output = new StringBuilder();
            byte[] buffer = new byte[1024];
            long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(connectTimeoutMs);
            while (true) {
                while (in.available() > 0) {
                    int read = in.read(buffer);
                    if (read < 0) {
                        break;
                    }
                    output.append(new String(buffer, 0, read));
                }
                if (channel.isClosed()) {
                    while (in.available() > 0) {
                        int read = in.read(buffer);
                        if (read < 0) {
                            break;
                        }
                        output.append(new String(buffer, 0, read));
                    }
                    break;
                }
                if (System.nanoTime() >= deadline) {
                    throw new BaseException("Command timeout for host: " + host);
                }
                Thread.sleep(50);
            }

            channel.disconnect();
            session.disconnect();
            return output.toString().trim();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BaseException("Command interrupted: " + host, e);
        } catch (Exception e) {
            throw new BaseException("Error executing command: " + e.getMessage(), e);
        }
    }

    @Override
    public ArrayList<Integer> getDeviceStatus(ArrayList<String> deviceList) {
        if (deviceList == null || deviceList.isEmpty()) {
            throw new BaseException("Device list cannot be null or empty.");
        }
        log.info("Received device list: {}", deviceList);
        List<CameraConfig.CameraInfo> cameras = cameraConfig.getCameras();
        Map<String, CameraConfig.CameraInfo> cameraByType = cameras == null ? new HashMap<>() : cameras.stream()
                .collect(Collectors.toMap(camera -> camera.getType().toLowerCase(), camera -> camera, (left, right) -> left));

        List<CompletableFuture<Integer>> futures = deviceList.stream()
                .map(device -> CompletableFuture.supplyAsync(() -> getSingleDeviceStatus(device, cameras, cameraByType)))
                .collect(Collectors.toList());

        ArrayList<Integer> statusList = futures.stream()
                .map(CompletableFuture::join)
                .collect(Collectors.toCollection(ArrayList::new));
        log.info("Device status list: {}", statusList);
        return statusList;
    }

    private Integer getSingleDeviceStatus(String device,
                                          List<CameraConfig.CameraInfo> cameras,
                                          Map<String, CameraConfig.CameraInfo> cameraByType) {
        if (device.equalsIgnoreCase("nx")) {
            return checkNxStatus(cameras);
        }
        if (device.equalsIgnoreCase("trans")) {
            return CameraConfig.transStatus;
        }
        if (device.equalsIgnoreCase("rgb")) {
            return checkRgbCameraStatus();
        }

        CameraConfig.CameraInfo camera = cameraByType.get(device.toLowerCase());
        if (camera == null) {
            log.info("No camera config found for device {}", device);
            return 1;
        }

        String command = camera.getCheckCommand();
        try {
            String output = execCommand(
                    camera.getHost(),
                    camera.getUsername(),
                    camera.getPassword(),
                    command,
                    camera.getCommandTimeoutMs()
            );
            log.info("Output for camera {}: {}", camera.getType(), output);
            return output.contains("available") ? 0 : 1;
        } catch (BaseException e) {
            log.info("Error checking camera {}: {}", camera.getType(), e.getMessage());
            return 1;
        }
    }

    private Integer checkNxStatus(List<CameraConfig.CameraInfo> cameras) {
        if (cameras == null || cameras.isEmpty()) {
            return 1;
        }
        boolean allReachable = cameras.parallelStream().allMatch(this::isCameraReachable);
        return allReachable ? 0 : 1;
    }

    private boolean isCameraReachable(CameraConfig.CameraInfo camera) {
        log.info("Checking status for camera: {}", camera.getHost());
        try {
            return InetAddress.getByName(camera.getHost()).isReachable(DEFAULT_PING_TIMEOUT_MS);
        } catch (IOException e) {
            log.info("Camera {} check failed.", camera.getHost(), e);
            return false;
        }
    }

    private Integer checkRgbCameraStatus() {
        try {
            sonyCameraService.getStatus(null);
            return 0;
        } catch (BaseException e) {
            log.info("RGB camera check failed: {}", e.getMessage());
            return 1;
        }
    }
}
