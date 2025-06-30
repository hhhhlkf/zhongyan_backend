package com.gosling.bms.service.impl;

import com.gosling.bms.conf.CameraConfig;
import com.gosling.bms.exception.BaseException;
import com.gosling.bms.service.MethodService;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;

@Service
@Slf4j
public class MethodServiceImpl implements MethodService {
    // 这里可以实现MethodService接口的方法
    // 例如，添加方法来处理特定的业务逻辑或数据处理
    private final CameraConfig cameraConfig;

    // 示例方法
    public MethodServiceImpl(CameraConfig cameraConfig) {
        this.cameraConfig = cameraConfig;
    }

    @Override
    public Boolean startMethod(String task) {
        for (CameraConfig.CameraInfo camera : cameraConfig.getCameras()) {
            if (!camera.getType().equals(task)) {
                continue;
            }
            String startCommand = camera.getStartProcess();
            JSch jsch = new JSch();
            try {
                com.jcraft.jsch.Session session = jsch.getSession(camera.getUsername(), camera.getHost(), 22);
                session.setPassword(camera.getPassword());
                session.setConfig("StrictHostKeyChecking", "no");
                session.connect();

                ChannelExec channel = (ChannelExec) session.openChannel("exec");
                channel.setCommand(startCommand);
                channel.setInputStream(null);
                java.io.InputStream in = channel.getInputStream();
                channel.connect();

                BufferedReader reader = new BufferedReader(new InputStreamReader(in));
                String line;
                long startTime = System.currentTimeMillis();
                long timeout = 15000; // 10秒超时
                while ((line = reader.readLine()) != null) {
                    log.info("Method start output: {}", line);
                    if (line.contains("Success start method")) {
                        channel.disconnect();
                        session.disconnect();
                        return true;
                    }
                    if( line.contains("Failed start method")) {
                        log.error("Failed to stop method: {}", line);
                        channel.disconnect();
                        session.disconnect();
                        return false;
                    }
                    if (System.currentTimeMillis() - startTime > timeout) {
                        log.warn("读取摄像头输出超时");
                        break;
                    }
                }
                channel.disconnect(); // 断开通道
                session.disconnect(); // 断开会话
                return false; // 如果没有找到成功的输出，则返回 false
                // 启动失败，关闭资源
            } catch (Exception e) {
                throw new BaseException("Process start failed: " + e.getMessage());
            }
        }

        return false;
    }

    @Override
    public Boolean stopMethod(String task) {
        for (CameraConfig.CameraInfo camera : cameraConfig.getCameras()) {
            if (!camera.getType().equals(task)) {
                continue;
            }
            String stopCommand = camera.getStopProcess();
            JSch jsch = new JSch();
            try {
                com.jcraft.jsch.Session session = jsch.getSession(camera.getUsername(), camera.getHost(), 22);
                session.setPassword(camera.getPassword());
                session.setConfig("StrictHostKeyChecking", "no");
                session.connect();

                ChannelExec channel = (ChannelExec) session.openChannel("exec");
                channel.setCommand(stopCommand);
                channel.setInputStream(null);
                java.io.InputStream in = channel.getInputStream();
                channel.connect();

                BufferedReader reader = new BufferedReader(new InputStreamReader(in));
                String line;
                long startTime = System.currentTimeMillis();
                long timeout = 15000; // 10秒超时
                while ((line = reader.readLine()) != null) {
                    log.info("Method start output: {}", line);
                    if (line.contains("Success stop method")) {
                        channel.disconnect();
                        session.disconnect();
                        return true;
                    }
                    if( line.contains("Failed stop method")) {
                        log.error("Failed to stop method: {}", line);
                        channel.disconnect();
                        session.disconnect();
                        return false;
                    }
                    if (System.currentTimeMillis() - startTime > timeout) {
                        log.warn("读取摄像头输出超时");
                        break;
                    }
                }
                channel.disconnect(); // 断开通道
                session.disconnect(); // 断开会话
                return false; // 如果没有找到成功的输出，则返回 false
                // 启动失败，关闭资源
            } catch (Exception e) {
                throw new BaseException("Process stop failed: " + e.getMessage());
            }
        }
        throw new BaseException("No matching Method found for task: " + task);

    }
}
