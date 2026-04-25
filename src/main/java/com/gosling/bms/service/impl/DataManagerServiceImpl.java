package com.gosling.bms.service.impl;

import com.gosling.bms.conf.DataConfig;
import com.gosling.bms.dao.entity.FileData;
import com.gosling.bms.exception.BaseException;
import com.gosling.bms.response.PageResponse;
import com.gosling.bms.service.DeleteItemsResult;
import com.gosling.bms.service.DataManagerService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
@Slf4j
public class DataManagerServiceImpl implements DataManagerService {

    private final DataConfig dataConfig;
    private final Map<String, Long> latestTimestampMap = new ConcurrentHashMap<>();

    public DataManagerServiceImpl(DataConfig dataConfig) {
        this.dataConfig = dataConfig;
    }

    @Override
    public List<FileData> getFileList(String type, String task) {
        Path path = buildTaskPath(type, task);
        if (dataConfig.getHistory().equals(task)) {
            log.info("Loading history files from path: {}", path);
            return loadFiles(path.toFile(), task, type);
        }
        if (dataConfig.getCollect().equals(task) || dataConfig.getProcess().equals(task)) {
            return loadTaskFiles(path.toFile(), type, task);
        }
        throw new BaseException("Unsupported task: " + task);
    }

    @Override
    public PageResponse<FileData> getHistoryPage(String type, int page, int pageSize) {
        validatePage(page, pageSize);
        List<FileData> fullList = getFileList(type, dataConfig.getHistory());
        fullList.sort(Comparator.comparing(FileData::getTime, Comparator.nullsLast(Comparator.naturalOrder())).reversed());
        return buildPage(fullList, page, pageSize, null);
    }

    @Override
    public PageResponse<FileData> getRecentPage(String type, String task, int page, int pageSize, Long snapshotTime) {
        validatePage(page, pageSize);
        if (!dataConfig.getCollect().equals(task) && !dataConfig.getProcess().equals(task)) {
            throw new BaseException("Unsupported recent task: " + task);
        }

        long effectiveSnapshotTime = snapshotTime == null ? System.currentTimeMillis() : snapshotTime;
        File taskDir = buildTaskPath(type, task).toFile();
        File polyDir = new File(taskDir, "poly");
        if (!taskDir.exists() || !taskDir.isDirectory()) {
            throw new BaseException("Directory does not exist: " + taskDir.getAbsolutePath());
        }
        System.out.println("taskDir: " + taskDir.getAbsolutePath() + ", polyDir: " + polyDir.getAbsolutePath());
        File[] debugImages = listImageFiles(taskDir);
        System.out.println("Scanning recent files in " + taskDir.getAbsolutePath()
                + " with snapshotTime=" + effectiveSnapshotTime);
        if (debugImages != null) {
            for (File image : debugImages) {
                System.out.println("Recent candidate: " + image.getName()
                        + ", lastModified=" + image.lastModified());
            }
        }

        List<RecentFileMeta> metas = scanRecentFileMetas(taskDir, effectiveSnapshotTime);
        int total = metas.size();
        int fromIndex = (page - 1) * pageSize;
        List<FileData> pageList = Collections.emptyList();
        if (fromIndex < total) {
            int toIndex = Math.min(fromIndex + pageSize, total);
            pageList = metas.subList(fromIndex, toIndex).stream()
                    .map(meta -> new AbstractMap.SimpleEntry<>(meta, new File(taskDir, meta.getFileName())))
                    .map(entry -> FileData.fromFile(entry.getValue(), polyDir, task, type, dataConfig.getWebPath()))
                    .collect(Collectors.toList());
        }
        return buildPageResponse(pageList, page, pageSize, effectiveSnapshotTime, total);
    }

    @Override
    public DeleteItemsResult deleteItems(String type, String task, List<String> names) {
        validateDeleteRequest(type, task, names);
        Path taskPath = buildTaskPath(type, task).normalize();
        Path polyPath = taskPath.resolve("poly").normalize();
        List<String> failedNames = new ArrayList<>();
        int successCount = 0;
        for (String name : names) {
            try {
                boolean deleted = deleteItemFiles(taskPath, polyPath, name);
                if (deleted) {
                    successCount++;
                } else {
                    failedNames.add(name);
                }
            } catch (Exception e) {
                log.warn("Failed to delete file: type={}, task={}, name={}", type, task, name, e);
                failedNames.add(name);
            }
        }
        return new DeleteItemsResult(type, task, successCount, failedNames.size(), failedNames);
    }

    @Override
    public DeleteItemsResult deleteAllItems(String type, String task) {
        validateDeleteTask(type, task);
        Path taskPath = buildTaskPath(type, task).normalize();
        Path polyPath = taskPath.resolve("poly").normalize();
        List<String> failedNames = new ArrayList<>();
        int successCount = 0;
        File[] images = listImageFiles(taskPath.toFile());
        if (images == null || images.length == 0) {
            latestTimestampMap.remove(type + "_" + task);
            return new DeleteItemsResult(type, task, 0, 0, failedNames);
        }

        for (File image : images) {
            String fileName = image.getName();
            try {
                boolean deleted = deleteItemFiles(taskPath, polyPath, fileName);
                if (deleted) {
                    successCount++;
                } else {
                    failedNames.add(fileName);
                }
            } catch (Exception e) {
                log.warn("Failed to delete file while clearing task: type={}, task={}, name={}", type, task, fileName, e);
                failedNames.add(fileName);
            }
        }
        latestTimestampMap.remove(type + "_" + task);
        return new DeleteItemsResult(type, task, successCount, failedNames.size(), failedNames);
    }

    @Override
    public Boolean deleteHistoryFile(String type, String name) {
        DeleteItemsResult result = deleteItems(type, dataConfig.getHistory(), Collections.singletonList(name));
        return result.getSuccessCount() > 0;
    }

    @Override
    public Boolean transferFile(String type, String task) {
        if (!dataConfig.getCollect().equals(task) && !dataConfig.getProcess().equals(task)) {
            throw new BaseException("Unsupported preview task: " + task);
        }
        Path taskPath = buildTaskPath(type, task);
        Path polyPath = taskPath.resolve("poly");
        try {
            clearDirectory(taskPath, true);
            clearDirectory(polyPath, false);
            latestTimestampMap.remove(type + "_" + task);
            return true;
        } catch (Exception e) {
            throw new BaseException("Error clearing preview files: " + e.getMessage(), e);
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

    private List<FileData> loadFiles(File dir, String task, String type) {
        File polyDir = new File(dir, "poly");
        if (!dir.exists() || !dir.isDirectory()) {
            throw new BaseException("Directory does not exist: " + dir.getAbsolutePath());
        }
        File[] images = listImageFiles(dir);
        List<FileData> result = new ArrayList<>();
        if (images == null) {
            return result;
        }
        for (File img : images) {
            result.add(FileData.fromFile(img, polyDir, task, type, dataConfig.getWebPath()));
        }
        return result;
    }

    private List<FileData> loadTaskFiles(File dir, String type, String task) {
        File polyDir = new File(dir, "poly");
        File[] images = listImageFiles(dir);
        if (images == null) {
            return Collections.emptyList();
        }

        String key = type + "_" + task;
        long lastTimestamp = latestTimestampMap.getOrDefault(key, 0L);
        List<File> newFiles = Arrays.stream(images)
                .filter(file -> file.lastModified() > lastTimestamp)
                .sorted(Comparator.comparingLong(File::lastModified).reversed())
                .collect(Collectors.toList());

        List<FileData> result = new ArrayList<>();
        for (File img : newFiles) {
            result.add(FileData.fromFile(img, polyDir, task, type, dataConfig.getWebPath()));
        }
        if (!newFiles.isEmpty()) {
            latestTimestampMap.put(key, newFiles.get(0).lastModified());
        }
        return result;
    }

    private File[] listImageFiles(File dir) {
        return dir.listFiles((d, name) -> {
            String lower = name.toLowerCase();
            return lower.endsWith(".png") || lower.endsWith(".jpg") || lower.endsWith(".jpeg");
        });
    }

    private List<RecentFileMeta> scanRecentFileMetas(File taskDir, long snapshotTime) {
        File[] images = listImageFiles(taskDir);
        if (images == null) {
            return Collections.emptyList();
        }
        File polyDir = new File(taskDir, "poly");
        return Arrays.stream(images)
                .filter(File::isFile)
                .filter(file -> hasMatchingPolyFile(polyDir, file.getName()))
                .filter(file -> file.lastModified() <= snapshotTime)
                .map(file -> new RecentFileMeta(file.getName(), file.lastModified()))
                .sorted(Comparator.comparingLong(RecentFileMeta::getLastModified).reversed()
                        .thenComparing(RecentFileMeta::getFileName, Comparator.reverseOrder()))
                .collect(Collectors.toList());
    }

    private boolean hasMatchingPolyFile(File polyDir, String imageFileName) {
        return FileData.findSharedPolyFile(polyDir, imageFileName) != null;
    }

    private void clearDirectory(Path dir, boolean skipPolyDir) throws Exception {
        if (!Files.exists(dir) || !Files.isDirectory(dir)) {
            return;
        }
        File[] files = dir.toFile().listFiles();
        if (files == null) {
            return;
        }
        for (File file : files) {
            if (skipPolyDir && file.isDirectory() && "poly".equals(file.getName())) {
                continue;
            }
            if (file.isDirectory()) {
                clearDirectory(file.toPath(), false);
                Files.deleteIfExists(file.toPath());
            } else {
                Files.deleteIfExists(file.toPath());
            }
        }
    }

    private Path buildTaskPath(String type, String task) {
        return Path.of(dataConfig.getBasePath(), type, task);
    }

    private void validateDeleteRequest(String type, String task, List<String> names) {
        validateDeleteTask(type, task);
        if (names == null || names.isEmpty()) {
            throw new BaseException("names must not be empty");
        }
    }

    private void validateDeleteTask(String type, String task) {
        if (type == null || type.trim().isEmpty()) {
            throw new BaseException("type must not be blank");
        }
        if (!isSupportedDeleteTask(task)) {
            throw new BaseException("Unsupported delete task: " + task);
        }
    }

    private boolean isSupportedDeleteTask(String task) {
        return dataConfig.getCollect().equals(task)
                || dataConfig.getProcess().equals(task)
                || dataConfig.getHistory().equals(task);
    }

    private String normalizeFileName(String fileName) {
        if (fileName == null || fileName.trim().isEmpty()) {
            throw new BaseException("name must not be blank");
        }
        String trimmed = fileName.trim();
        Path normalized = Paths.get(trimmed).normalize();
        if (normalized.isAbsolute() || normalized.getNameCount() != 1 || trimmed.contains("/") || trimmed.contains("\\")) {
            throw new BaseException("Illegal file name: " + fileName);
        }
        return normalized.toString();
    }

    private Path resolveFileWithinTask(Path baseDir, String fileName) {
        Path normalizedBaseDir = baseDir.normalize();
        Path resolved = normalizedBaseDir.resolve(fileName).normalize();
        if (!resolved.startsWith(normalizedBaseDir)) {
            throw new BaseException("Illegal file name: " + fileName);
        }
        return resolved;
    }

    private boolean deleteItemFiles(Path taskPath, Path polyPath, String name) throws Exception {
        String safeFileName = normalizeFileName(name);
        Path imagePath = resolveFileWithinTask(taskPath, safeFileName);
        File matchedPolyFile = FileData.findMatchingPolyFile(polyPath.toFile(), safeFileName);
        Path polyFilePath = matchedPolyFile == null
                ? resolveFileWithinTask(polyPath, FileData.getPolyFileName(safeFileName))
                : resolveFileWithinTask(polyPath, matchedPolyFile.getName());
        boolean deleted = Files.deleteIfExists(imagePath);
        Files.deleteIfExists(polyFilePath);
        return deleted;
    }

    private void validatePage(int page, int pageSize) {
        if (page < 1) {
            throw new BaseException("page must be greater than or equal to 1");
        }
        if (pageSize < 1) {
            throw new BaseException("pageSize must be greater than or equal to 1");
        }
    }

    private PageResponse<FileData> buildPage(List<FileData> fullList, int page, int pageSize, Long snapshotTime) {
        int total = fullList.size();
        int fromIndex = (page - 1) * pageSize;
        List<FileData> pageList;
        if (fromIndex >= total) {
            pageList = Collections.emptyList();
        } else {
            int toIndex = Math.min(fromIndex + pageSize, total);
            pageList = fullList.subList(fromIndex, toIndex);
        }
        return buildPageResponse(pageList, page, pageSize, snapshotTime, total);
    }

    private PageResponse<FileData> buildPageResponse(List<FileData> pageList, int page, int pageSize, Long snapshotTime, int total) {
        PageResponse<FileData> response = new PageResponse<>();
        response.setFileList(pageList);
        response.setPage(page);
        response.setPageSize(pageSize);
        response.setTotal(total);
        int totalPages = total == 0 ? 0 : (int) Math.ceil((double) total / pageSize);
        response.setTotalPages(totalPages);
        response.setHasNext(page < totalPages);
        response.setHasPrevious(page > 1 && total > 0);
        response.setSnapshotTime(snapshotTime);
        return response;
    }

    private static class RecentFileMeta {
        private final String fileName;
        private final long lastModified;

        private RecentFileMeta(String fileName, long lastModified) {
            this.fileName = fileName;
            this.lastModified = lastModified;
        }

        private String getFileName() {
            return fileName;
        }

        private long getLastModified() {
            return lastModified;
        }
    }
}
