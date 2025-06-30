package com.gosling.bms.controller;

import com.gosling.bms.exception.BaseException;
import com.gosling.bms.response.ResponseResult;
import com.gosling.bms.service.ClockService;
import com.gosling.bms.service.DeviceService;
import com.gosling.bms.utils.FileUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import static com.gosling.bms.utils.FileUtils.basePath;

@RestController
@Slf4j
public class DeviceController {

    @Autowired
    private DeviceService deviceService;

    @Autowired
    private ClockService clockService;

    public static void generateRandomNumbers(ArrayList<Integer> status) {
        Random random = new Random();

        // 生成4个随机数并添加到队列
        for (int i = 0; i < 5; i++) {
            int randomNumber = random.nextInt(2); // 生成0到2之间的随机数
            status.add(randomNumber);
        }
    }

    @GetMapping("/device/info")
    @ResponseResult
    public Map<String, Object> getDeviceInfo(@RequestParam List<String> deviceList) {
//        System.out.println("Received device list: " + deviceList);
//        ArrayList<Integer> status = new ArrayList<>();
        // TODO 这里可以添加获取设备信息的逻辑
        ArrayList<Integer> status = deviceService.getDeviceStatus((ArrayList<String>) deviceList);
//        generateRandomNumbers(status);
        Map<String, Object> map = new HashMap<>();
        map.put("status", status);
        return map;
    }

    @GetMapping("/log/info")
    @ResponseResult
    public Map<String, Object> getLogList() throws IOException {
        Path logFilePath = Paths.get(basePath, "log.txt");
        if (!Files.exists(logFilePath)) {
            throw new BaseException("文件错误: log.txt");
        }

        List<String> logLines = Files.readAllLines(logFilePath);
        List<String> logList = new ArrayList<>();

        StringBuilder logEntry = new StringBuilder();
        for (String line : logLines) {
            if (line.trim().isEmpty()) {
                if (logEntry.length() > 0) {
                    logList.add(logEntry.toString().trim());
                    logEntry.setLength(0);
                }
            } else {
                logEntry.append(line).append("\n");
            }
        }
        // Add the last log entry if it exists
        if (logEntry.length() > 0) {
            logList.add(logEntry.toString().trim());
        }

        Files.write(logFilePath, new byte[0]);

        HashMap<String, Object> map = new HashMap<>();
        map.put("logList", logList);
        return map;
    }

    @GetMapping("/transport/schedule")
    @ResponseResult
    public Float getTransportSchedule() {

        Float schedule = 0.0f;
        Random random = new Random();
        schedule = random.nextFloat();
        String formattedFloat = String.format("%.4f", schedule);
        schedule = Float.parseFloat(formattedFloat);
        return schedule;
    }

    @GetMapping("/transport/rate")
    @ResponseResult
    public Float getTransportRate() {

        Float rate = 0.0f;
        Float rateSecond = 0.0f;
        String directoryPath = "\\\\192.168.101.100\\FileRecv_Shared\\Rate";
        File directory = new File(directoryPath);

        if (directory.exists() && directory.isDirectory()) {
            // 获取所有以 "0M" 开头的 png 文件
            File[] files = directory.listFiles((dir, name) -> name.startsWith("0M") && name.endsWith(".txt"));

            if (files != null && files.length > 0) {
                // 找到后缀最大的文件
                Optional<File> latestFile = Arrays.stream(files)
                        .max(Comparator.comparingInt(file -> {
                            String name = file.getName();
                            String numberPart = name.substring(2, name.length() - 4); // 去掉 "0M" 和 ".png"
                            return Integer.parseInt(numberPart);
                        }));
                // 找到后缀第二大的文件
                Optional<File> secondLatestFile = Arrays.stream(files)
                        .filter(file -> !file.equals(latestFile.get()))
                        .max(Comparator.comparingInt(file -> {
                            String name = file.getName();
                            String numberPart = name.substring(2, name.length() - 4); // 去掉 "0M" 和 ".png"
                            return Integer.parseInt(numberPart);
                        }));

                if (latestFile.isPresent()) {
                    try (BufferedReader reader = new BufferedReader(new FileReader(latestFile.get()))) {
                        // 读取文件的第一行
                        String firstLine = reader.readLine();
                        if (firstLine != null) {
                            // 将字符串转换为数字，并除以 1024*1024
                            rate = Float.parseFloat(firstLine) / (1024 * 1024);
                        }
                    } catch (IOException | NumberFormatException e) {
                        e.printStackTrace();
                    }
                }
                if (secondLatestFile.isPresent()) {
                    try (BufferedReader reader = new BufferedReader(new FileReader(secondLatestFile.get()))) {
                        // 读取文件的第一行
                        String firstLine = reader.readLine();
                        if (firstLine != null) {
                            // 将字符串转换为数字，并除以 1024*1024
                            rateSecond = Float.parseFloat(firstLine) / (1024 * 1024);
                        }
                    } catch (IOException | NumberFormatException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        return rate - rateSecond;
    }

    @GetMapping("/source/percentage")
    @ResponseResult
    public Map<String, Float> getSourcePercentage(@RequestParam Boolean isSim) {

        if (isSim == null) {
            throw new BaseException("参数错误: isSim不能为空");
        }
        if (isSim) {
            Float nx_1 = 0.0f;
            Float nx_2 = 0.0f;
            Float nx_3 = 0.0f;

            // 获取传输速率
            Random random = new Random();
            nx_1 = 0.7f + random.nextFloat() * (0.9f - 0.8f);
            nx_2 = 0.7f + random.nextFloat() * (0.9f - 0.8f);
            nx_3 = 0.7f + random.nextFloat() * (0.9f - 0.8f);

            String formattedFloat_1 = String.format("%.4f", nx_1);
            String formattedFloat_2 = String.format("%.4f", nx_2);
            String formattedFloat_3 = String.format("%.4f", nx_3);

            nx_1 = Float.parseFloat(formattedFloat_1);
            nx_2 = Float.parseFloat(formattedFloat_2);
            nx_3 = Float.parseFloat(formattedFloat_3);

            HashMap<String, Float> nxMap = new HashMap<>();
            nxMap.put("nx_1", nx_1);
            nxMap.put("nx_2", nx_2);
            nxMap.put("nx_3", nx_3);

            return nxMap;
        } else {
//            return (HashMap<String, Float>) Map.of("nx_1", 0.0f, "nx_2", 0.0f, "nx_3", 0.0f);
            Map<String, Float> deviceStatus = deviceService.getDeviceStatus();
            if (deviceStatus == null || deviceStatus.isEmpty()) {
                throw new BaseException("设备状态获取失败");
            }
            return deviceStatus;
        }

    }

    @GetMapping("/server/ip")
    @ResponseResult
    public HashMap<String, String> getWebServer() {
        HashMap<String, String> webServer = new HashMap<>();
        String wiredIPAddress = FileUtils.getWiredIPAddress();
        webServer.put("ip", wiredIPAddress);
        return webServer;
    }

    // TODO 传输速率也要记录


    @GetMapping("/device/clock")
    @ResponseResult
    public Map<String, Object> getDeviceClock() {
        Map<String, Object> map = new HashMap<>();
        try {
            Boolean isCla = clockService.calibrateTime();
            map.put("status", isCla);
            return map;
        } catch (Exception e) {
            log.error("获取设备时钟失败: {}", e.getMessage());
            throw new BaseException("获取设备时钟失败: " + e.getMessage());
        }

    }
}
