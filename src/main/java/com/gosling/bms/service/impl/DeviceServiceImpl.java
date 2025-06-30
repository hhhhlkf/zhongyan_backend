package com.gosling.bms.service.impl;

import com.gosling.bms.conf.CameraConfig;
import com.gosling.bms.exception.BaseException;
import com.gosling.bms.service.DeviceService;
import com.jcraft.jsch.JSch;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;



@Service
@Slf4j
public class DeviceServiceImpl implements DeviceService {

    private final CameraConfig cameraConfig;

    public DeviceServiceImpl(CameraConfig cameraConfig) {
        this.cameraConfig = cameraConfig;
    }


    @Override
    public Map<String, Float> getDeviceStatus() {
        // 这里可以实现获取设备状态的逻辑
        HashMap<String, Float> statusMap = new HashMap<>();
        List<CameraConfig.CameraInfo> cameraList = cameraConfig.getCameras();
        if (cameraList == null || cameraList.isEmpty()) {
            throw new BaseException("No cameras configured.");
        }
        for (int i = 0; i < cameraList.size(); i++) {
            CameraConfig.CameraInfo camera = cameraList.get(i);
            // 假设每个板卡都有一个获取状态的方法
            // 这里可以调用实际的API或执行命令来获取板卡状态
            log.info("Checking status for camera: {}", camera.getHost());
            String command = "free | awk '/Mem:/ {printf(\"%.4f\", $3/$2)}'";
            // 这里可以使用JSch或其他方式执行命令
            JSch jsch = new JSch();


            try{
                // 创建SSH会话
                var session = jsch.getSession(camera.getUsername(), camera.getHost(), 22);
                session.setPassword(camera.getPassword());
                session.setConfig("StrictHostKeyChecking", "no");
                session.connect();

                // 执行命令
                var channel = session.openChannel("exec");
                ((com.jcraft.jsch.ChannelExec) channel).setCommand(command);
                channel.setInputStream(null);
                var in = channel.getInputStream();
                channel.connect();

                // 读取输出
                StringBuilder output = new StringBuilder();
                byte[] buffer = new byte[1024];
                int read;
                while ((read = in.read(buffer)) != -1) {
                    output.append(new String(buffer, 0, read));
                }

                if (output.length() == 0) {
                    throw new BaseException("No output received from camera: " + camera.getHost());
                }

                // 处理输出结果
                log.info("Output: {}", output.toString().trim());

                // 加入到状态映射中
                statusMap.put("nx_" + (i + 1), Float.parseFloat(output.toString().trim()));

                // 断开连接
                channel.disconnect();
                session.disconnect();
            } catch (Exception e) {
                throw new BaseException("Error checking camera status: " + camera.getHost(), e);
            }

            // 模拟返回状态
        }


        return statusMap;
    }

   // 通过 SSH 执行远程命令并返回输出结果
   public String execCommand(String host, String username, String password, String command) {
       JSch jsch = new JSch();
       try {
           // 创建 SSH 会话
           var session = jsch.getSession(username, host, 22);
           session.setPassword(password);
           // 跳过主机密钥检查
           session.setConfig("StrictHostKeyChecking", "no");
           session.connect();

           // 打开 exec 通道用于执行命令
           var channel = session.openChannel("exec");
           ((com.jcraft.jsch.ChannelExec) channel).setCommand(command);
           channel.setInputStream(null);
           var in = channel.getInputStream();
           channel.connect();

           // 读取命令执行结果
           StringBuilder output = new StringBuilder();
           byte[] buffer = new byte[1024];
           int read;
           while ((read = in.read(buffer)) != -1) {
               output.append(new String(buffer, 0, read));
           }

           // 断开通道和会话
           channel.disconnect();
           session.disconnect();

           // 返回命令输出
           return output.toString().trim();
       } catch (Exception e) {
           // 捕获异常并抛出自定义异常
           throw new BaseException("Error executing command: " + e.getMessage(), e);
       }
   }

    @Override
    public ArrayList<Integer> getDeviceStatus(ArrayList<String> deviceList) {
        if (deviceList == null || deviceList.isEmpty()) {
            throw new BaseException("Device list cannot be null or empty.");
        }
        // 'nx', 'trans', 'llt', 'rgb', 'hsi'
        ArrayList<Integer> statusList = new ArrayList<>();
        for(String device : deviceList) {
            if(device.equalsIgnoreCase("nx")) {
                // 这里可以实现获取NX设备状态的逻辑
                Boolean isNXAvailable = true;
                for(CameraConfig.CameraInfo camera : cameraConfig.getCameras()) {
                    // 假设每个摄像头都有一个获取状态的方法
                    // 这里可以调用实际的API或执行命令来获取摄像头状态
                    log.info("Checking status for camera: {}", camera.getHost());
                    try {
                        boolean reachable = InetAddress.getByName(camera.getHost()).isReachable(3000);
                    } catch (IOException e) {
                        isNXAvailable = false;
                        log.info("Camera {} is not reachable.", camera.getHost());
                        statusList.add(1); // 添加不可用状态
                    }
                }
                if (isNXAvailable) {
                    statusList.add(0); // 添加可用状态
                }
            }else if (device.equalsIgnoreCase("trans")) {
                statusList.add(CameraConfig.transStatus);
            } else {
                // 摄像头类型处理逻辑
                for (CameraConfig.CameraInfo camera : cameraConfig.getCameras()) {
                    if (camera.getType().equalsIgnoreCase(device)) {
                        String command = camera.getCheckCommand();
                        try {
                            String output = execCommand(camera.getHost(), "nvidia", "nvidia", command);
                            log.info("Output for camera {}: {}", camera.getType(), output);
                            if (output.contains("available")) {
                                statusList.add(0); // 摄像头可用
                            } else {
                                statusList.add(1); // 摄像头不可用
                            }
                        } catch (BaseException e) {
                            log.info("Error checking camera {}: {}", camera.getType(), e.getMessage());
                            statusList.add(1); // 摄像头不可用
                        }
                    }
                }
            }
        }

        return statusList;
    }
}
