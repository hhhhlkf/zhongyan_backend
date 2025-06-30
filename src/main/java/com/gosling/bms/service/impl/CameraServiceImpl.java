package com.gosling.bms.service.impl;

import com.gosling.bms.conf.CameraConfig;
import com.gosling.bms.exception.BaseException;
import com.gosling.bms.service.CameraService;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;

@Service
@Slf4j
public class CameraServiceImpl implements CameraService {

    private final List<CameraConfig.CameraInfo> cameraList;
    private final CameraConfig cameraConfig;

    /**
     * 构造函数，加载摄像头配置
     *
     * @param cameraConfig 摄像头配置
     */
    public CameraServiceImpl(CameraConfig cameraConfig) {
        this.cameraConfig = cameraConfig;
        this.cameraList = this.cameraConfig.getCameras();
        if (cameraList == null || cameraList.isEmpty()) {
            throw new BaseException("No cameras configured.");
        }
    }

    @Override
    public Boolean isCameraAvailable(String task) {

        if (cameraList == null || cameraList.isEmpty()) {
            throw new BaseException("No cameras configured.");
        }

        // 比对task与cameraList中的type，仅判断该type类型的摄像头是否可用
        for (CameraConfig.CameraInfo camera : cameraList) {
            if (camera.getType().equalsIgnoreCase(task)) {

                String command = camera.getCheckCommand(); // 获取检查命令
                JSch jsch = new JSch();
                try {
                    Session session = jsch.getSession("nvidia", camera.getHost(), 22);
                    session.setPassword("nvidia"); // 设置密码
                    // 跳过主机密钥检查
                    session.setConfig("StrictHostKeyChecking", "no");
                    session.connect();
                    // 打开执行命令的通道
                    ChannelExec channel = (ChannelExec) session.openChannel("exec");
                    channel.setCommand(command); // 设置要执行的命令
                    channel.setInputStream(null); // 不需要输入流
                    java.io.InputStream in = channel.getInputStream(); // 获取命令输出流
                    channel.connect(); // 执行命令
                    // 读取命令输出
                    StringBuilder output = new StringBuilder();
                    byte[] buffer = new byte[1024];
                    int read;
                    while ((read = in.read(buffer)) != -1) {
                        output.append(new String(buffer, 0, read));
                    }
                    channel.disconnect(); // 断开通道
                    session.disconnect(); // 断开会话

                    // TODO 根据实际需求处理结果
                    // 未发现相机设备
                    return output.toString().contains("available");

                } catch (JSchException | IOException e) {
                    throw new BaseException("Camera check failed: " + e.getMessage(), e);
                }

            }
        }

        return false;
    }

    // 新增方法：负责建立 SSH 连接并执行启动命令
    private Boolean startCameraSession(CameraConfig.CameraInfo camera, Integer camSpeed) {
        String command = camera.getStartCommand() + " " + camSpeed; // 获取启动命令并添加速度参数
        log.info("Starting camera {} with command: {}", camera.getType(), command);
        JSch jsch = new JSch();
        try {
            // 1. 建立 SSH 会话
            Session session = jsch.getSession("nvidia", camera.getHost(), 22);
            session.setPassword("nvidia"); // 设置密码
            session.setConfig("StrictHostKeyChecking", "no"); // 跳过主机密钥检查
            session.connect();

            // 2. 打开命令执行通道
            ChannelExec channel = (ChannelExec) session.openChannel("exec");
            channel.setCommand(command);
            channel.setInputStream(null);
            java.io.InputStream in = channel.getInputStream();
            channel.connect();

            // 3. 读取命令输出，判断是否启动成功
            BufferedReader reader = new BufferedReader(new InputStreamReader(in));
            String line;
            long startTime = System.currentTimeMillis();
            long timeout = 15000; // 10秒超时
            while ((line = reader.readLine()) != null) {
                log.info("Camera start output: {}", line);
                if (line.contains("Success start camera")) {
                    channel.disconnect();
                    session.disconnect();
                    return true;
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
        } catch (JSchException | IOException e) {
            throw new BaseException("Camera start failed: " + e.getMessage(), e);
        }
    }

    @Override
    public Boolean startCamera(String task, Integer camSpeed) {
        if (!isCameraAvailable(task)) {
            log.info("Camera type {} is not available", task);
            return false;
        }
        for (CameraConfig.CameraInfo camera : cameraList) {
            if (camera.getType().equalsIgnoreCase(task)) {
                return startCameraSession(camera, camSpeed);
            }
        }
        return false;
    }

    @Override
    public Boolean stopCamera(String task) {
       for (CameraConfig.CameraInfo camera : cameraList) {
            if (camera.getType().equalsIgnoreCase(task)) {
                String command = camera.getStopCommand(); // 获取停止命令
                JSch jsch = new JSch();
                try {
                    Session session = jsch.getSession("nvidia", camera.getHost(), 22);
                    session.setPassword("nvidia"); // 设置密码
                    session.setConfig("StrictHostKeyChecking", "no"); // 跳过主机密钥检查
                    session.connect();
                    // 打开执行命令的通道
                    ChannelExec channel = (ChannelExec) session.openChannel("exec");
                    channel.setCommand(command); // 设置要执行的命令
                    channel.setInputStream(null); // 不需要输入流
                    java.io.InputStream in = channel.getInputStream(); // 获取命令输出流
                    channel.connect(); // 执行命令
                    // 读取命令输出
                    BufferedReader reader = new BufferedReader(new InputStreamReader(in));
                    String line;
                    long startTime = System.currentTimeMillis();
                    long timeout = 10000; // 10秒超时
                    while ((line = reader.readLine()) != null) {
                        log.info("Camera stop output: {}", line);
                        if (line.contains("Success stop camera")) {
                            channel.disconnect();
                            session.disconnect();
                            return true;
                        }
                        if (System.currentTimeMillis() - startTime > timeout) {
                            log.warn("读取摄像头输出超时");
                            break;
                        }
                    }
                    channel.disconnect(); // 断开通道
                    session.disconnect(); // 断开会话
                    return false; // 如果没有找到成功的输出，则返回 false

                } catch (JSchException | IOException e) {
                    throw new BaseException("Camera stop failed: " + e.getMessage(), e);
                }
            }
        }
        return false;
    }
}
