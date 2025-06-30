package com.gosling.bms.service.impl;

import com.gosling.bms.conf.DataConfig;
import com.gosling.bms.dao.entity.FileData;
import com.gosling.bms.exception.BaseException;
import com.gosling.bms.service.DataManagerService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
@Slf4j
public class DataManagerServiceImpl implements DataManagerService {


    private final DataConfig dataConfig;

    private final Map<String, Long> latestTimestampMap = new ConcurrentHashMap<>();

    // 加载DataConfig
    public DataManagerServiceImpl(DataConfig dataConfig) {
        // 这里可以加载配置文件或进行其他初始化操作
        this.dataConfig = dataConfig;

    }

    /**
     * 获取文件列表
     *
     * @param type 文件类型
     * @param task 任务名称，包含history, collect, process
     * @return 文件列表
     */
    @Override
    public List<FileData> getFileList(String type, String task) {
        Path path = Path.of(dataConfig.getBasePath(), type, task);
        if (task.equals("history")) {
            log.info("Loading history files from path: {}", path);
            return loadHistoryFiles(path.toString(), task, type);
        } else if (task.equals("collect") || task.equals("process")) {
            // 处理收集或处理文件的逻辑
            // 这里可以添加获取收集或处理文件列表的代码
            return loadTaskFiles(path.toString(), type, task); // 返回空列表或实际的文件列表
        } else {
            throw new BaseException("Unsupported file type: " + type);
        }
    }

    /**
     * 加载历史文件
     *
     * @param path 文件路径
     * @param task 任务名称
     * @param type 文件类型
     * @return 文件数据列表
     */
    public List<FileData> loadHistoryFiles(String path, String task, String type) {
        File dir = new File(path);
        File polyDir = new File(dir, "poly");
        File[] images = dir.listFiles((d, name) -> name.endsWith(".png") || name.endsWith(".jpg"));
        List<FileData> result = new ArrayList<>();
        if (!dir.exists() || !dir.isDirectory()) {
            throw new BaseException("Directory does not exist: " + path);
        }
        if (images != null) {
            for (File img : images) {
//                System.out.println("Loading image: " + img.getAbsolutePath());
                result.add(FileData.fromFile(img, polyDir, task, type, dataConfig.getWebPath()));
            }
        }else {
            log.warn("No images found in directory: {}", path);
            throw new BaseException("No images found in directory: " + path);
        }
        return result;
    }

    public List<FileData> loadTaskFiles(String path, String type, String task) {
        File dir = new File(path);
        File polyDir = new File(dir, "poly");
        File[] images = dir.listFiles((d, name) -> name.endsWith(".png") || name.endsWith(".jpg"));
        List<FileData> result = new ArrayList<>();
        if (images != null) {
            // 获取上次已加载的最新时间戳
            String key = type + "_" + task;
            long lastTimestamp = latestTimestampMap.getOrDefault(key, 0L);
            // 只保留比上次时间戳新的文件
            List<File> newFiles = Arrays.stream(images)
                    .filter(f -> f.lastModified() > lastTimestamp)
                    .sorted(Comparator.comparingLong(File::lastModified).reversed())
                    .collect(Collectors.toList());
            // 加载新文件
            for (File img : newFiles) {
                result.add(FileData.fromFile(img, polyDir, task, type, dataConfig.getWebPath()));
            }
            // 更新最新时间戳
            if (!newFiles.isEmpty()) {
                long maxTimestamp = newFiles.get(0).lastModified();
                latestTimestampMap.put(key, maxTimestamp);
            }
        }
        return result;
    }

    @Override
    public Boolean transferFile(String type, String task) {
        Path path = Path.of(dataConfig.getBasePath(), type, task);
        Path polyDir = path.resolve("poly");
        Path historyPath = Path.of(dataConfig.getBasePath(), type, "history");
        Path historyPolyDir = historyPath.resolve("poly");

        try {
            // 创建目标目录
            java.nio.file.Files.createDirectories(historyPath);
            java.nio.file.Files.createDirectories(historyPolyDir);

            File[] imgFiles = path.toFile().listFiles(file ->
                    file.isFile() && (file.getName().endsWith(".jpg") || file.getName().endsWith(".png")));
            if (imgFiles == null) return true;

            boolean allSuccess = true;
            for (File imgFile : imgFiles) { // 遍历所有图片文件
                String fileName = imgFile.getName(); // 获取图片文件名
                int lastUnderscore = fileName.lastIndexOf("_"); // 查找文件名中最后一个下划线的位置
                if (lastUnderscore == -1) { // 如果没有下划线，说明文件名格式不对
                    imgFile.delete(); // 删除该图片
                    allSuccess = false; // 标记有失败
                    continue; // 跳过本次循环
                }
                String polyFileName = FileData.getPolyFileName(fileName); // 构造对应的poly文件名
                File polyFile = polyDir.resolve(polyFileName).toFile(); // 获取poly文件对象

                if (polyFile.exists()) { // 如果poly文件存在
                    // 移动图片到history目录
                    java.nio.file.Files.move(imgFile.toPath(), historyPath.resolve(fileName),
                            java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                    // 移动poly文件到history的poly目录
                    java.nio.file.Files.move(polyFile.toPath(), historyPolyDir.resolve(polyFileName),
                            java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                } else { // 如果poly文件不存在
                    // 删除图片文件
                    if (!imgFile.delete()) {
                        allSuccess = false; // 删除失败，标记
                    }
                }
            }
            return allSuccess;
        } catch (Exception e) {
            throw new BaseException("Error transferring files: " + e.getMessage(), e);
        }
    }

    @Override
    public Boolean monitorAndTransferFile() {
        return null;
    }

    @Override
    public Boolean closeMonitor() {
        return null;
    }
}
