package com.gosling.bms.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.gosling.bms.conf.DataConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashSet;
import java.util.Set;


@Slf4j
@Service
@RequiredArgsConstructor
public class ImageDispatcherService implements Runnable {


    private final DataConfig dataConfig;
    private final Set<String> processedFiles = new HashSet<>();
    private static final String DIRECTORY_PATH = "\\\\192.168.1.100\\FileRecv_Shared\\Data";
    private static final String FILE_PATH = "src/main/resources/static/udp_data.json";

    @PostConstruct
    public void start() {
        Thread thread = new Thread(this, "ImageDispatcherThread");
        thread.setDaemon(true);
        thread.start();
    }

    private void moveFileWithRetry(Path source, Path target) throws IOException {
        int retry = 5;
        while (retry-- > 0) {
            try {
                Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
                log.info("文件 {} 成功移动到 {}", source.getFileName(), target);
                return;
            } catch (java.nio.file.FileSystemException e) {
                if (retry == 0) throw e;

                try { Thread.sleep(500); } catch (InterruptedException ignored) {
                    log.warn("线程被中断，重试移动文件失败: {}", e.getMessage());
                }
            }
        }
    }

    @Override
    public void run() {
        log.info("图片分流服务启动，监听目录: {}", DIRECTORY_PATH);
        while (true) {
            try {
                File dir = new File(DIRECTORY_PATH);
                File[] files = dir.listFiles();
                if (files != null) {
                    for (File file : files) {
                        if (file.isFile() && !processedFiles.contains(file.getName())) {
                            dispatchFile(file);

                        }
                    }
                }
                Thread.sleep(2000); // 每2秒扫描一次
            } catch (Exception e) {
                log.error("图片分流服务异常", e);
            }
        }
    }

    private void dispatchFile(File file) throws IOException {
        String fileName = file.getName();
        for (DataConfig.Receive receive : dataConfig.getReceive()) {
            String type = receive.getType();
            if (fileName.contains(type)) {
                try {
                    String targetDir = dataConfig.getBasePath() + File.separator + type + File.separator;
                    if (fileName.contains("O")) {
                        targetDir += dataConfig.getProcess();
                    } else {
                        targetDir += dataConfig.getCollect();
                    }
                    File destDir = new File(targetDir);
                    if (!destDir.exists()) destDir.mkdirs();
                    Path targetPath = Paths.get(destDir.getAbsolutePath(), fileName);
                    //Files.move(file.toPath(), targetPath, StandardCopyOption.REPLACE_EXISTING);
                    moveFileWithRetry(file.toPath(), targetPath);
                    log.info("文件 {} 已分流到 {}", fileName, targetPath);
                    processedFiles.add(file.getName());

                    createCoordinateTxt(fileName, destDir.getAbsolutePath(), type, targetPath.toFile());
                } catch (Exception e) {
                    log.error("分流文件 {} 时发生异常: {}", fileName, e.getMessage());
                }
                break;
            }
        }
    }


    private void createCoordinateTxt(String fileName, String targetDir, String type, File imageFile) {
        try {

            log.info("开始生成坐标txt，文件名: {}, 目标目录: {}, 类型: {}", fileName, targetDir, type);
            // 获取图片文件的创建时间戳（秒）
            Path imagePath = imageFile.toPath();
            long fileCreateTime = Files.readAttributes(imagePath, java.nio.file.attribute.BasicFileAttributes.class)
                    .creationTime().toMillis() / 1000;

            // 读取JSON文件
            File jsonFile = new File(FILE_PATH);
            if (!jsonFile.exists() || jsonFile.length() == 0) return;
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            com.fasterxml.jackson.databind.JsonNode root = mapper.readTree(jsonFile);
            if (!root.isArray() || root.size() == 0) return;

            // 找到最接近的timestamp
            double lon = 0, lat = 0;
            long minDiff = Long.MAX_VALUE;
            for (JsonNode node : root) {
                com.fasterxml.jackson.databind.JsonNode dataNode = node.get("data");
                if (dataNode == null || !dataNode.has("timestamp")) continue;
                long ts = dataNode.get("timestamp").asLong();
                long diff = Math.abs(ts - fileCreateTime);
                if (diff < minDiff) {
                    minDiff = diff;
                    lon = dataNode.get("lon").asDouble();
                    lat = dataNode.get("lat").asDouble();
                }
            }

            // 生成poly目录
            File polyDir = new File(targetDir, "poly");
            if (!polyDir.exists()) polyDir.mkdirs();

            // 生成txt文件名
            String baseName = fileName.replaceFirst(type + "O?_", type + "P_").replaceAll("\\.(jpg|png)$", ".txt");
            File txtFile = new File(polyDir, baseName);

            // TODO 计算图片的左上角和右下角
            double deltaLat = 160.0 / 111000; // 50米对应的纬度变化
            double deltaLon = 160.0 / (111320 * Math.cos(Math.toRadians(lat))); // 50米对应的经度变化
            // 写入坐标
            try (java.io.PrintWriter out = new java.io.PrintWriter(txtFile)) {
                out.printf("%.7f %.7f%n", lon, lat);
                out.printf("%.7f %.7f%n", lon + deltaLon, lat - deltaLat);
            }
            log.info("坐标文件 {} 已生成", txtFile.getAbsolutePath());
        } catch (Exception e) {
            log.error("生成坐标txt失败", e);
            throw new RuntimeException("生成坐标txt失败", e);
        }
    }

}
