package com.gosling.bms.service.impl;

import com.gosling.bms.conf.CameraConfig;
import com.gosling.bms.exception.BaseException;
import com.gosling.bms.service.TransferService;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;


@Service
@Slf4j
public class TransferServiceImpl implements TransferService {

    private final CameraConfig cameraConfig;


    public TransferServiceImpl(CameraConfig cameraConfig) {
        this.cameraConfig = cameraConfig;
    }


    @Override
    public Boolean isTransferAvailable() {

        return true;
    }

    @Override
    public Boolean startTransfer() {
        if (!isTransferAvailable()) {
            return false;
        }
        String command2 = cameraConfig.getTransfer().getStartCommand();
        JSch jsch = new JSch();
        try {
            Session session = jsch.getSession(cameraConfig.getTransfer().getUsername(), cameraConfig.getTransfer().getHost(), 22);
            session.setPassword(cameraConfig.getTransfer().getPassword()); // 设置密码
            session.setConfig("StrictHostKeyChecking", "no"); // 跳过主机密钥检查
            session.connect();

            // 打开执行命令的通道
            ChannelExec channel = (ChannelExec) session.openChannel("exec");
            channel.setCommand(command2);
            channel.setInputStream(null);

            java.io.InputStream in = channel.getInputStream();
            channel.connect();

            // 3. 读取命令输出，判断是否启动成功
            BufferedReader reader = new BufferedReader(new InputStreamReader(in));
            String line;
            long startTime = System.currentTimeMillis();
            long timeout = 15000; // 10秒超时
            while ((line = reader.readLine()) != null) {
                log.info("transport start output: {}", line);
                if (line.contains("Success start transport")) {
                    CameraConfig.transStatus = 0;
                    channel.disconnect();
                    session.disconnect();
                    return true;
                }
                if (line.contains("Failed start transport")) {
                    log.error("Failed to stop transport: {}", line);
                    channel.disconnect();
                    session.disconnect();
                    return false;
                }
                if (System.currentTimeMillis() - startTime > timeout) {
                    log.warn("读取传输输出超时");
                    break;
                }
            }
            channel.disconnect(); // 断开通道
            session.disconnect(); // 断开会话
            return false; // 如果没有找到成功的输出，则返回 false
            // 启动失败，关闭资源

        } catch (JSchException | IOException e) {
            throw new BaseException("Transfer start failed: " + e.getMessage(), e);
        }

    }

    @Override
    public Boolean stopTransfer() {
        String command2 = cameraConfig.getTransfer().getStopCommand();
        JSch jsch = new JSch();
        try {
            Session session = jsch.getSession(cameraConfig.getTransfer().getUsername(), cameraConfig.getTransfer().getHost(), 22);
            session.setPassword(cameraConfig.getTransfer().getPassword()); // 设置密码
            session.setConfig("StrictHostKeyChecking", "no"); // 跳过主机密钥检查
            session.connect();
            // 打开执行命令的通道
            ChannelExec channel = (ChannelExec) session.openChannel("exec");
            channel.setCommand(command2);
            channel.setInputStream(null);

            java.io.InputStream in = channel.getInputStream();
            channel.connect();

            // 3. 读取命令输出，判断是否启动成功
            BufferedReader reader = new BufferedReader(new InputStreamReader(in));
            String line;
            long startTime = System.currentTimeMillis();
            long timeout = 15000; // 10秒超时
            while ((line = reader.readLine()) != null) {
                log.info("transport start output: {}", line);
                if (line.contains("Success stop transport")) {
                    CameraConfig.transStatus = 1;
                    channel.disconnect();
                    session.disconnect();
                    return true;
                }
                if (line.contains("Failed stop transport")) {
                    log.error("Failed to stop transport: {}", line);
                    channel.disconnect();
                    session.disconnect();
                    return false;
                }
                if (System.currentTimeMillis() - startTime > timeout) {
                    log.warn("读取通信输出超时");
                    break;
                }
            }
            channel.disconnect(); // 断开通道
            session.disconnect(); // 断开会话
            return false; // 如果没有找到成功的输出，则返回 false
            // 启动失败，关闭资源

        } catch (JSchException | IOException e) {
            throw new BaseException("Transfer stop failed: " + e.getMessage(), e);
        }

    }
}
