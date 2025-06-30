package com.gosling.bms.service.impl;

import com.gosling.bms.conf.CameraConfig;
import com.gosling.bms.exception.BaseException;
import com.gosling.bms.service.ClockService;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;

@Service
@Slf4j
public class ClockServiceImpl implements ClockService {
    // 实现ClockService接口的方法
    // 这里可以添加具体的业务逻辑代码
    private final CameraConfig cameraConfig;

    // 示例方法
    public ClockServiceImpl(CameraConfig cameraConfig) {
        this.cameraConfig = cameraConfig;
    }

    @Override
    public Boolean calibrateTime() {
        for (CameraConfig.CameraInfo camera : cameraConfig.getCameras()) {
            // 获取当前时间字符串
            String timeString = java.time.LocalDateTime.now()
                    .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            log.info("Setting remote camera time for {}: {}", camera.getHost(), timeString);
            Boolean b = setRemoteTime(camera, timeString);
            if (b) {
                log.info("Successfully set time for camera: {}", camera.getHost());
            } else {
                log.error("Failed to set time for camera: {}", camera.getHost());
                return false; // 如果有一个摄像头设置失败，则返回 false
            }
        }
        return true;
    }

    private Boolean setRemoteTime(CameraConfig.CameraInfo camera, String timeString) {
        String command = "sudo date -s \"" + timeString + "\"";
        String timePart = timeString.substring(11); // 结果: "15:23:45"
        log.info("The command to set time is: {}", command);
        try {
            JSch jsch = new JSch();
            Session session = jsch.getSession(camera.getUsername(), camera.getHost(), 22);
            session.setPassword(camera.getPassword());
            session.setConfig("StrictHostKeyChecking", "no");
            session.connect(5000);

            // 1. 设置时间
            ChannelExec channel = (ChannelExec) session.openChannel("exec");
            channel.setCommand(command);
            channel.setInputStream(null);
            java.io.InputStream in = channel.getInputStream();
            channel.connect();

            BufferedReader reader = new BufferedReader(new InputStreamReader(in));
            String line;
            long startTime = System.currentTimeMillis();
            long timeout = 15000; // 10秒超时
            while ((line = reader.readLine()) != null) {
                log.info("Clock set output: {}", line);
                if (line.contains(timePart)) {
                    channel.disconnect();
                    session.disconnect();
                    return true;
                }
                if (System.currentTimeMillis() - startTime > timeout) {
                    log.warn("设置时间输出超时");
                    break;
                }
            }
            channel.disconnect(); // 断开通道
            session.disconnect(); // 断开会话
            return false; // 如果没有找到成功的输出，则返回 false


        } catch (Exception e) {
            throw new BaseException("Failed to set remote time on camera: " + camera.getHost() + " - " + e.getMessage());
        }
    }

    // 其他方法可以根据需要添加
}
