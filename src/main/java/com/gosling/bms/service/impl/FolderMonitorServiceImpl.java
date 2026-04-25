package com.gosling.bms.service.impl;

import com.gosling.bms.exception.BaseException;
import com.gosling.bms.service.FolderMonitorService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.gosling.bms.utils.FileUtils.basePath;

@Service
@Slf4j
public class FolderMonitorServiceImpl implements FolderMonitorService {

    private static final long POLL_INTERVAL_MS = 100L;
    private static final DateTimeFormatter LOG_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final AtomicBoolean running = new AtomicBoolean(false);
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private volatile Future<?> monitorTask;

    @Override
    public synchronized void startMonitor(String folder1, String folder2) {
        if (running.get()) {
            throw new BaseException("文件监控任务已在运行");
        }

        Path sourceFolder = resolveSourceFolder(folder1);
        if (!Files.isDirectory(sourceFolder)) {
            throw new BaseException("监控目录不存在: " + sourceFolder);
        }

        clearNewTxtFile(folder1);
        clearNewTxtFile(folder2);
        clearLogFile();

        running.set(true);
        monitorTask = executor.submit(() -> monitorLoop(folder1, folder2));
        log.info("Started folder monitor for source={}, folder1={}, folder2={}", sourceFolder, folder1, folder2);
    }

    @Override
    public synchronized void stopMonitor() {
        running.set(false);
        if (monitorTask != null) {
            monitorTask.cancel(true);
            monitorTask = null;
        }
    }

    @Override
    public boolean isRunning() {
        return running.get();
    }

    private void monitorLoop(String folder1, String folder2) {
        Path sourceFolder = resolveSourceFolder(folder1);
        while (running.get() && !Thread.currentThread().isInterrupted()) {
            try {
                List<String> processedEntries = processSourceFolder(sourceFolder, folder1, folder2);
                if (!processedEntries.isEmpty()) {
                    appendLog(processedEntries);
                }
                Thread.sleep(POLL_INTERVAL_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                running.set(false);
                log.error("Folder monitor failed for source={}", sourceFolder, e);
                break;
            }
        }
    }

    private List<String> processSourceFolder(Path sourceFolder, String folder1, String folder2) throws IOException {
        List<String> processedEntries = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(sourceFolder)) {
            for (Path sourcePath : stream) {
                if (Files.isDirectory(sourcePath)) {
                    continue;
                }
                if (sourcePath.toString().contains("Rate")) {
                    continue;
                }
                processFile(sourcePath, folder1, folder2);
                processedEntries.add("Processed file: " + sourcePath.getFileName());
            }
        }
        return processedEntries;
    }

    private void processFile(Path sourcePath, String folder1, String folder2) throws IOException {
        String fileName = sourcePath.getFileName().toString();
        Path targetFolder;
        if (fileName.contains("D")) {
            targetFolder = Paths.get(basePath, folder1);
        } else if (fileName.contains("A")) {
            targetFolder = Paths.get(basePath, folder2);
        } else {
            targetFolder = Paths.get(basePath);
        }

        Files.createDirectories(targetFolder);
        Path targetPath = targetFolder.resolve(fileName);
        Files.move(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);

        if (!fileName.endsWith(".txt")) {
            Files.writeString(
                    targetFolder.resolve("new.txt"),
                    fileName + System.lineSeparator(),
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND
            );
        }
    }

    private void clearNewTxtFile(String folderName) {
        Path newTxtPath = Paths.get(basePath, folderName, "new.txt");
        try {
            Files.createDirectories(newTxtPath.getParent());
            Files.write(newTxtPath, new byte[0], StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            throw new BaseException("清空 new.txt 失败: " + newTxtPath, e);
        }
    }

    private void clearLogFile() {
        Path logPath = Paths.get(basePath, "log.txt");
        try {
            Files.write(logPath, new byte[0], StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            throw new BaseException("清空日志失败: " + logPath, e);
        }
    }

    private void appendLog(List<String> entries) {
        Path logPath = Paths.get(basePath, "log.txt");
        StringBuilder builder = new StringBuilder();
        builder.append(LocalDateTime.now().format(LOG_TIME_FORMATTER))
                .append(" - Completed processing files:")
                .append(System.lineSeparator());
        for (String entry : entries) {
            builder.append(entry).append(System.lineSeparator());
        }
        builder.append(System.lineSeparator());

        try {
            Files.writeString(
                    logPath,
                    builder.toString(),
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND
            );
        } catch (IOException e) {
            throw new BaseException("写入日志失败: " + logPath, e);
        }
    }

    private Path resolveSourceFolder(String folder1) {
        return Paths.get(basePath, folder1 + "_sim");
    }
}
