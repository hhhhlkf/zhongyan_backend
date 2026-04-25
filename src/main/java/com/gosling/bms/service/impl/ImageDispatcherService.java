package com.gosling.bms.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.gosling.bms.conf.DataConfig;
import com.gosling.bms.dao.entity.FileData;
import com.gosling.bms.utils.PolyCoordinateUtils;
import com.gosling.bms.utils.UdpDataFileStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class ImageDispatcherService implements Runnable {

    private final DataConfig dataConfig;
    private final ImagePreviewService imagePreviewService;
    private final GroundElevationService groundElevationService;
    private static final String DIRECTORY_PATH = "\\\\192.168.1.100\\FileRecv_Shared\\Data";
    private static final Pattern IMAGE_PATTERN = Pattern.compile("^.+\\.(jpg|jpeg|png)$", Pattern.CASE_INSENSITIVE);
    private static final Pattern RGB_IMAGE_PATTERN = Pattern.compile("^rgb(O)?_([^\\\\/]+)\\.(jpg|jpeg|png)$", Pattern.CASE_INSENSITIVE);
    private static final Pattern TIMESTAMP_TXT_PATTERN = Pattern.compile("^([^\\\\/]+)\\.txt$", Pattern.CASE_INSENSITIVE);

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
                log.info("File {} moved to {}", source.getFileName(), target);
                return;
            } catch (java.nio.file.FileSystemException e) {
                if (retry == 0) {
                    throw e;
                }

                try {
                    Thread.sleep(500);
                } catch (InterruptedException ignored) {
                    log.warn("Retry interrupted while moving file: {}", e.getMessage());
                }
            }
        }
    }

    @Override
    public void run() {
        log.info("Image dispatcher service started, watching {}", DIRECTORY_PATH);
        while (true) {
            try {
                File dir = new File(DIRECTORY_PATH);
                File[] files = dir.listFiles();
                if (files != null) {
                    for (File file : files) {
                        if (file.isFile() && isTimestampTxtFile(file)) {
                            dispatchTimestampFile(file);
                        }
                    }
                    for (File file : files) {
                        if (file.isFile() && isImageFile(file)) {
                            dispatchFile(file);
                        }
                    }
                }
                reconcileRgbPolyDirectories();
                Thread.sleep(2000);
            } catch (Exception e) {
                log.error("Image dispatcher service failed", e);
            }
        }
    }

    private void dispatchTimestampFile(File file) throws IOException {
        Path timeDir = getTimeDir("rgb");
        Files.createDirectories(timeDir);
        Path targetPath = timeDir.resolve(file.getName());
        moveFileWithRetry(file.toPath(), targetPath);
        log.info("Timestamp file {} moved to {}", file.getName(), targetPath);
    }

    private void dispatchFile(File file) throws IOException {
        String fileName = file.getName();
        for (DataConfig.Receive receive : dataConfig.getReceive()) {
            String type = receive.getType();
            if (fileName.contains(type)) {
                try {
                    Path timeFile = null;
                    Long captureTimestampMillis = null;
                    if ("rgb".equalsIgnoreCase(type)) {
                        timeFile = resolveTimeFile(type, fileName);
                        if (timeFile == null) {
                            log.warn("Unsupported RGB image name format, skip dispatch. file={}", fileName);
                            return;
                        }
                        if (!Files.exists(timeFile)) {
                            log.info("Timestamp txt not ready, postpone image dispatch. file={}, timeFile={}", fileName, timeFile);
                            return;
                        }
                        captureTimestampMillis = readCaptureTimestampMillis(timeFile);
                    }

                    String task = resolveTask(fileName, type);
                    Path typePath = Paths.get(dataConfig.getBasePath(), type);
                    Path historyDir = typePath.resolve(dataConfig.getHistory());
                    Path taskDir = typePath.resolve(task);
                    Path historyPolyDir = historyDir.resolve("poly");
                    Path taskPolyDir = taskDir.resolve("poly");

                    Files.createDirectories(historyDir);
                    Files.createDirectories(taskDir);
                    Files.createDirectories(historyPolyDir);
                    Files.createDirectories(taskPolyDir);

                    String localFileName = buildLocalFileName(fileName, captureTimestampMillis);
                    Path historyPath = historyDir.resolve(localFileName);
                    moveFileWithRetry(file.toPath(), historyPath);

                    File historyFile = historyPath.toFile();
                    String previewFileName = imagePreviewService.buildPreviewFileName(localFileName);
                    File previewFile = taskDir.resolve(previewFileName).toFile();
                    DataConfig.PreviewItem previewConfig = resolvePreviewConfig(task);
                    if (isPreviewEnabled()) {
                        imagePreviewService.generatePreview(historyFile, previewFile, previewConfig);
                    } else {
                        Files.copy(historyPath, previewFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    }

                    createCoordinateTxt(localFileName, type, timeFile, historyFile);
                    trimPreviewDirectory(taskDir.toFile(), taskPolyDir.toFile(), previewConfig);
                    log.info("File {} archived to {} and preview generated at {}", fileName, historyPath, previewFile);
                } catch (Exception e) {
                    log.error("Failed to dispatch file {}", fileName, e);
                }
                break;
            }
        }
    }

    private void createCoordinateTxt(String localFileName, String type, Path timeFile, File imageFile) {
        try {
            String canonicalPolyFileName = FileData.getPolyFileName(localFileName);
            List<PolyCoordinateUtils.GeoPoint> corners = loadExistingCorners(type, localFileName);
            if ((corners == null || corners.isEmpty())) {
                File coordinateSourceImage = resolveCoordinateSourceImage(type, localFileName, imageFile);
                if (coordinateSourceImage == null) {
                    log.info("Coordinate generation deferred until original image is available. file={}", localFileName);
                    return;
                }
                corners = calculateCorners(type, timeFile, coordinateSourceImage);
            }
            if (corners == null || corners.isEmpty()) {
                return;
            }
            writeCanonicalPoly(type, canonicalPolyFileName, corners);
        } catch (Exception e) {
            log.error("Failed to generate coordinate txt", e);
            throw new RuntimeException("Failed to generate coordinate txt", e);
        }
    }

    private List<PolyCoordinateUtils.GeoPoint> calculateCorners(String type, Path timeFile, File imageFile) throws IOException {
        DataConfig.Coordinate coordinateConfig = dataConfig.getCoordinate();
        if (coordinateConfig == null) {
            log.warn("Coordinate config is missing, skip coordinate txt generation.");
            return null;
        }

        long captureTimestampMillis = resolveCaptureTimestampMillis(timeFile, imageFile);
        JsonNode root = UdpDataFileStore.readSnapshot();
        if (root == null || !root.isArray() || root.size() == 0) {
            log.warn("UDP snapshot is empty, skip coordinate txt generation.");
            return null;
        }

        JsonNode bestDataNode = null;
        long minDiff = Long.MAX_VALUE;
        for (JsonNode node : root) {
            JsonNode dataNode = node.get("data");
            if (dataNode == null || !dataNode.has("timestamp")) {
                continue;
            }
            long ts = normalizeTimestampMillis(dataNode.get("timestamp").asLong());
            long diff = Math.abs(ts - captureTimestampMillis);
            if (diff < minDiff) {
                minDiff = diff;
                bestDataNode = dataNode;
            }
        }
        if (bestDataNode == null) {
            log.warn("No matched telemetry data found, skip coordinate txt generation.");
            return null;
        }

        long maxMatchSeconds = coordinateConfig.getMaxMatchSeconds() == null ? 5L : coordinateConfig.getMaxMatchSeconds();
        long maxMatchMillis = maxMatchSeconds * 1000L;
        if (maxMatchSeconds > 0 && minDiff > maxMatchMillis) {
            log.warn("Telemetry match too far from capture time: diff={}ms, limit={}ms, file={}",
                    minDiff, maxMatchMillis, imageFile.getName());
            return null;
        }

        double centerLon = bestDataNode.path("lon").asDouble(Double.NaN);
        double centerLat = bestDataNode.path("lat").asDouble(Double.NaN);
        double uavAlt = bestDataNode.path("alt").asDouble(Double.NaN);
        double yawDeg = bestDataNode.path("yaw").asDouble(0.0);
        if (Double.isNaN(centerLon) || Double.isNaN(centerLat) || Double.isNaN(uavAlt)) {
            log.warn("Telemetry data is incomplete, skip coordinate txt generation. file={}", imageFile.getName());
            return null;
        }

        double groundElevationMeters = groundElevationService.resolveGroundElevation(coordinateConfig, centerLat, centerLon);

        double horizontalFovDeg = requireConfig(coordinateConfig.getHorizontalFovDeg(), "horizontalFovDeg");
        double verticalFovDeg = resolveVerticalFovDeg(coordinateConfig, imageFile, horizontalFovDeg);
        double relativeHeight = uavAlt - groundElevationMeters;
        if (relativeHeight <= 0) {
            log.warn("Relative height is invalid: alt={}, groundElevation={}, file={}",
                    uavAlt, groundElevationMeters, imageFile.getName());
            return null;
        }

        double halfGroundWidth = relativeHeight * Math.tan(Math.toRadians(horizontalFovDeg / 2.0));
        double halfGroundHeight = relativeHeight * Math.tan(Math.toRadians(verticalFovDeg / 2.0));
        double effectiveYawDeg = coordinateConfig.getUseYaw() == null || coordinateConfig.getUseYaw() ? yawDeg : 0.0;

        PolyCoordinateUtils.GeoPoint leftTop = offsetByYaw(centerLat, centerLon, -halfGroundWidth, halfGroundHeight, effectiveYawDeg);
        PolyCoordinateUtils.GeoPoint rightTop = offsetByYaw(centerLat, centerLon, halfGroundWidth, halfGroundHeight, effectiveYawDeg);
        PolyCoordinateUtils.GeoPoint rightBottom = offsetByYaw(centerLat, centerLon, halfGroundWidth, -halfGroundHeight, effectiveYawDeg);
        PolyCoordinateUtils.GeoPoint leftBottom = offsetByYaw(centerLat, centerLon, -halfGroundWidth, -halfGroundHeight, effectiveYawDeg);
        return List.of(leftTop, rightTop, rightBottom, leftBottom);
    }

    private List<PolyCoordinateUtils.GeoPoint> loadExistingCorners(String type, String localFileName) throws IOException {
        for (String task : List.of(dataConfig.getHistory(), dataConfig.getCollect(), dataConfig.getProcess())) {
            File polyDir = Paths.get(dataConfig.getBasePath(), type, task, "poly").toFile();
            File polyFile = FileData.findMatchingPolyFile(polyDir, localFileName);
            if (polyFile != null && polyFile.isFile()) {
                return PolyCoordinateUtils.readPoints(polyFile);
            }
        }
        return null;
    }

    private void writeCanonicalPoly(String type, String polyFileName, List<PolyCoordinateUtils.GeoPoint> corners)
            throws IOException {
        for (String task : List.of(dataConfig.getHistory(), dataConfig.getCollect(), dataConfig.getProcess())) {
            Path polyDir = Paths.get(dataConfig.getBasePath(), type, task, "poly");
            Files.createDirectories(polyDir);
            PolyCoordinateUtils.writePoints(polyDir.resolve(polyFileName).toFile(), corners);
        }
    }

    private void reconcileRgbPolyDirectories() {
        reconcileTaskPolyDirectory("rgb", dataConfig.getHistory());
        reconcileTaskPolyDirectory("rgb", dataConfig.getCollect());
        reconcileTaskPolyDirectory("rgb", dataConfig.getProcess());
    }

    private void reconcileTaskPolyDirectory(String type, String task) {
        File taskDir = Paths.get(dataConfig.getBasePath(), type, task).toFile();
        File polyDir = new File(taskDir, "poly");
        try {
            reconcileTaskPolyDirectory(taskDir, polyDir);
        } catch (IOException e) {
            log.warn("Failed to reconcile poly directory. type={}, task={}", type, task, e);
        }
    }

    static void reconcileTaskPolyDirectory(File taskDir, File polyDir) throws IOException {
        if (taskDir == null || !taskDir.exists() || !taskDir.isDirectory()) {
            return;
        }
        Files.createDirectories(polyDir.toPath());

        File[] images = taskDir.listFiles(file -> isImageFile(file) && file.isFile());
        if (images == null) {
            images = new File[0];
        }

        Set<String> expectedPolyNames = new LinkedHashSet<>();
        for (File image : images) {
            String expectedPolyName = FileData.getPolyFileName(image.getName());
            expectedPolyNames.add(expectedPolyName);
            synchronizeExactPolyFile(polyDir, image.getName());
        }

        File[] polyFiles = polyDir.listFiles((dir, name) -> name != null && name.toLowerCase().endsWith(".txt"));
        if (polyFiles == null) {
            return;
        }
        for (File polyFile : polyFiles) {
            if (!expectedPolyNames.contains(polyFile.getName())) {
                Files.deleteIfExists(polyFile.toPath());
            }
        }
    }

    static boolean synchronizeExactPolyFile(File polyDir, String imageFileName) throws IOException {
        if (polyDir == null || imageFileName == null || imageFileName.trim().isEmpty()) {
            return false;
        }
        Files.createDirectories(polyDir.toPath());
        File targetPolyFile = new File(polyDir, FileData.getPolyFileName(imageFileName));
        if (targetPolyFile.isFile()) {
            return true;
        }

        File sharedPolyFile = FileData.findSharedPolyFile(polyDir, imageFileName);
        if (sharedPolyFile == null || !sharedPolyFile.isFile()) {
            return false;
        }

        if (sameFile(targetPolyFile, sharedPolyFile)) {
            return true;
        }

        Files.copy(sharedPolyFile.toPath(), targetPolyFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        Files.setLastModifiedTime(targetPolyFile.toPath(), FileTime.fromMillis(sharedPolyFile.lastModified()));
        return true;
    }

    private static boolean sameFile(File left, File right) throws IOException {
        return left.getCanonicalFile().equals(right.getCanonicalFile());
    }

    private File resolveCoordinateSourceImage(String type, String localFileName, File currentImageFile) {
        if (!isProcessImage(localFileName, type)) {
            return currentImageFile;
        }
        File originalHistoryImage = findOriginalHistoryImage(type, localFileName);
        if (originalHistoryImage != null) {
            return originalHistoryImage;
        }
        DataConfig.Coordinate coordinateConfig = dataConfig.getCoordinate();
        if (coordinateConfig != null && coordinateConfig.getVerticalFovDeg() != null) {
            return currentImageFile;
        }
        return null;
    }

    private File findOriginalHistoryImage(String type, String localFileName) {
        File historyDir = Paths.get(dataConfig.getBasePath(), type, dataConfig.getHistory()).toFile();
        if (!historyDir.exists() || !historyDir.isDirectory()) {
            return null;
        }
        String exactGroupKey = FileData.getExactGroupKey(localFileName);
        String looseGroupKey = FileData.getLooseGroupKey(localFileName);
        File[] candidates = historyDir.listFiles(candidate ->
                candidate.isFile()
                        && isImageFile(candidate)
                        && !isProcessImage(candidate.getName(), type)
                        && (exactGroupKey.equals(FileData.getExactGroupKey(candidate.getName()))
                        || looseGroupKey.equals(FileData.getLooseGroupKey(candidate.getName()))));
        if (candidates == null || candidates.length == 0) {
            return null;
        }
        File latest = candidates[0];
        for (File candidate : candidates) {
            if (candidate.lastModified() > latest.lastModified()) {
                latest = candidate;
            }
        }
        return latest;
    }

    private Path getTimeDir(String type) {
        return Paths.get(dataConfig.getBasePath(), type, dataConfig.getTime());
    }

    private Path resolveTimeFile(String type, String imageFileName) {
        String suffix = extractImageSuffix(imageFileName);
        if (suffix == null) {
            return null;
        }
        return getTimeDir(type).resolve(suffix + ".txt");
    }

    static String extractImageSuffix(String imageFileName) {
        Matcher matcher = RGB_IMAGE_PATTERN.matcher(imageFileName);
        if (!matcher.matches()) {
            return null;
        }
        return matcher.group(2);
    }

    static boolean isTimestampTxtFile(File file) {
        if (file == null || !file.isFile()) {
            return false;
        }
        return TIMESTAMP_TXT_PATTERN.matcher(file.getName()).matches();
    }

    static boolean isImageFile(File file) {
        if (file == null || !file.isFile()) {
            return false;
        }
        return IMAGE_PATTERN.matcher(file.getName()).matches();
    }

    static boolean isProcessImage(String imageFileName, String type) {
        if (imageFileName == null || type == null) {
            return false;
        }
        return imageFileName.regionMatches(true, 0, type + "O_", 0, (type + "O_").length());
    }

    static String buildLocalFileName(String originalFileName, Long captureTimestampMillis) {
        if (captureTimestampMillis == null || captureTimestampMillis <= 0) {
            return originalFileName;
        }
        Matcher matcher = RGB_IMAGE_PATTERN.matcher(originalFileName);
        if (!matcher.matches()) {
            return originalFileName;
        }
        String processMarker = matcher.group(1) == null ? "" : matcher.group(1);
        String logicalSuffix = matcher.group(2);
        String extension = originalFileName.substring(originalFileName.lastIndexOf('.'));
        String timestampText = String.valueOf(captureTimestampMillis);
        if (!logicalSuffix.endsWith("_" + timestampText)) {
            logicalSuffix = logicalSuffix + "_" + timestampText;
        }
        return "rgb" + processMarker + "_" + logicalSuffix + extension;
    }

    static long readCaptureTimestampMillis(Path timeFile) throws IOException {
        List<String> lines = Files.readAllLines(timeFile);
        if (lines.isEmpty()) {
            throw new IOException("Timestamp file is empty: " + timeFile);
        }
        String value = lines.get(0).trim();
        if (!value.matches("\\d{13}")) {
            throw new IOException("Timestamp file must contain 13-digit milliseconds: " + timeFile);
        }
        return Long.parseLong(value);
    }

    static long normalizeTimestampMillis(long timestamp) {
        return timestamp < 1_000_000_000_000L ? timestamp * 1000L : timestamp;
    }

    static long resolveCaptureTimestampMillis(Path timeFile, File imageFile) throws IOException {
        if (timeFile != null) {
            return readCaptureTimestampMillis(timeFile);
        }
        return Files.readAttributes(imageFile.toPath(), java.nio.file.attribute.BasicFileAttributes.class)
                .creationTime().toMillis();
    }

    static double requireConfig(Double value, String fieldName) {
        if (value == null) {
            throw new IllegalArgumentException("Coordinate config field is required: " + fieldName);
        }
        return value;
    }

    static double resolveVerticalFovDeg(DataConfig.Coordinate coordinateConfig, File imageFile, double horizontalFovDeg)
            throws IOException {
        if (coordinateConfig.getVerticalFovDeg() != null) {
            return coordinateConfig.getVerticalFovDeg();
        }
        BufferedImage bufferedImage = ImageIO.read(imageFile);
        if (bufferedImage == null) {
            throw new IOException("Failed to read image size: " + imageFile.getAbsolutePath());
        }
        return calcVerticalFovDeg(horizontalFovDeg, bufferedImage.getWidth(), bufferedImage.getHeight());
    }

    static double calcVerticalFovDeg(double horizontalFovDeg, int imageWidthPx, int imageHeightPx) {
        double horizontalRad = Math.toRadians(horizontalFovDeg);
        double verticalRad = 2.0 * Math.atan(Math.tan(horizontalRad / 2.0) * ((double) imageHeightPx / imageWidthPx));
        return Math.toDegrees(verticalRad);
    }

    static PolyCoordinateUtils.GeoPoint offsetByYaw(double centerLat, double centerLon,
                                                    double eastMeters, double northMeters, double yawDeg) {
        double yawRad = Math.toRadians(yawDeg);
        double rotatedEast = eastMeters * Math.cos(yawRad) + northMeters * Math.sin(yawRad);
        double rotatedNorth = -eastMeters * Math.sin(yawRad) + northMeters * Math.cos(yawRad);
        return moveLatLon(centerLat, centerLon, rotatedEast, rotatedNorth);
    }

    static PolyCoordinateUtils.GeoPoint moveLatLon(double latDeg, double lonDeg, double eastMeters, double northMeters) {
        double metersPerDegLat = 111320.0;
        double metersPerDegLon = 111320.0 * Math.cos(Math.toRadians(latDeg));
        if (Math.abs(metersPerDegLon) < 1e-9) {
            throw new IllegalArgumentException("Longitude conversion is unstable at latitude: " + latDeg);
        }
        double newLat = latDeg + northMeters / metersPerDegLat;
        double newLon = lonDeg + eastMeters / metersPerDegLon;
        return new PolyCoordinateUtils.GeoPoint(newLon, newLat);
    }

    private String resolveTask(String fileName, String type) {
        return isProcessImage(fileName, type) ? dataConfig.getProcess() : dataConfig.getCollect();
    }

    private DataConfig.PreviewItem resolvePreviewConfig(String task) {
        if (dataConfig.getPreview() == null) {
            return null;
        }
        if (dataConfig.getProcess().equals(task)) {
            return dataConfig.getPreview().getProcess();
        }
        return dataConfig.getPreview().getCollect();
    }

    private boolean isPreviewEnabled() {
        return dataConfig.getPreview() == null || dataConfig.getPreview().isEnabled();
    }

    private void trimPreviewDirectory(File taskDir, File polyDir, DataConfig.PreviewItem config) {
        int retainCount = config == null || config.getRetainCount() == null ? 100 : config.getRetainCount();
        imagePreviewService.trimPreviewDirectory(taskDir, polyDir, retainCount);
    }
}
