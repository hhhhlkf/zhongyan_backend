package com.gosling.bms.dao.entity;

import com.gosling.bms.exception.BaseException;
import com.gosling.bms.utils.PolyCoordinateUtils;
import lombok.Data;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static com.gosling.bms.utils.FileUtils.getWiredIPAddress;

@Data
public class FileData {
    private String id;
    private String name;
    private String time;
    private Boolean graphic;
    private List<LatLng> poly;
    private String task;
    private String path;
    private String type;
    private String url;

    @Data
    public static class LatLng {
        private Double lat;
        private Double lng;

        public LatLng(Double lng, Double lat) {
            this.lat = lat;
            this.lng = lng;
        }
    }

    public static String getPolyFileName(String fileName) {
        ParsedFileName parsed = parseFileName(fileName);
        return parsed.typePrefix + "P_" + parsed.logicalSuffix + ".txt";
    }

    public static File findMatchingPolyFile(File polyDir, String imageFileName) {
        return findLocalMatchingPolyFile(polyDir, imageFileName);
    }

    public static File findSharedPolyFile(File polyDir, String imageFileName) {
        for (File candidateDir : buildSearchDirs(polyDir)) {
            File polyFile = findLocalMatchingPolyFile(candidateDir, imageFileName);
            if (polyFile != null) {
                return polyFile;
            }
        }
        return null;
    }

    private static File findLocalMatchingPolyFile(File polyDir, String imageFileName) {
        if (polyDir == null || !polyDir.exists() || !polyDir.isDirectory()) {
            return null;
        }

        File exactFile = new File(polyDir, getPolyFileName(imageFileName));
        if (exactFile.isFile()) {
            return exactFile;
        }

        String exactGroupKey = getExactGroupKey(imageFileName);
        File exactGroupFile = findByGroupKey(polyDir, exactGroupKey, true);
        if (exactGroupFile != null) {
            return exactGroupFile;
        }

        String looseGroupKey = getLooseGroupKey(imageFileName);
        return findByGroupKey(polyDir, looseGroupKey, false);
    }

    private static List<File> buildSearchDirs(File polyDir) {
        List<File> dirs = new ArrayList<>();
        if (polyDir == null) {
            return dirs;
        }
        File taskDir = polyDir.getParentFile();
        File typeDir = taskDir == null ? null : taskDir.getParentFile();
        if (typeDir != null && typeDir.isDirectory()) {
            addPolyDirIfPresent(dirs, new File(typeDir, "history"), polyDir);
            addPolyDirIfPresent(dirs, new File(typeDir, "collect"), polyDir);
            addPolyDirIfPresent(dirs, new File(typeDir, "process"), polyDir);
        }
        if (!dirs.contains(polyDir)) {
            dirs.add(polyDir);
        }
        return dirs;
    }

    private static void addPolyDirIfPresent(List<File> dirs, File taskDir, File currentPolyDir) {
        File candidate = new File(taskDir, "poly");
        if (candidate.exists() && candidate.isDirectory() && !candidate.equals(currentPolyDir) && !dirs.contains(candidate)) {
            dirs.add(candidate);
        }
    }

    private static File findByGroupKey(File polyDir, String targetGroupKey, boolean exact) {
        File[] candidates = polyDir.listFiles((dir, name) -> {
            if (name == null || !name.toLowerCase().endsWith(".txt")) {
                return false;
            }
            try {
                String candidateKey = exact ? getExactGroupKey(name) : getLooseGroupKey(name);
                return targetGroupKey.equals(candidateKey);
            } catch (Exception ignored) {
                return false;
            }
        });
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

    public static String getExactGroupKey(String fileName) {
        ParsedFileName parsed = parseFileName(fileName);
        return parsed.typePrefix + "_" + parsed.logicalSuffix;
    }

    public static String getLooseGroupKey(String fileName) {
        ParsedFileName parsed = parseFileName(fileName);
        String logicalSuffix = parsed.logicalSuffix.replaceFirst("_\\d{13}$", "");
        return parsed.typePrefix + "_" + logicalSuffix;
    }

    private static ParsedFileName parseFileName(String fileName) {
        if (fileName == null || !fileName.contains(".") || !fileName.contains("_")) {
            throw new BaseException("Illegal image file name: " + fileName);
        }
        int lastDot = fileName.lastIndexOf(".");
        String baseName = fileName.substring(0, lastDot);
        int firstUnderscore = baseName.indexOf("_");
        if (firstUnderscore <= 0 || firstUnderscore == baseName.length() - 1) {
            throw new BaseException("Illegal image file name: " + fileName);
        }
        String typePrefix = baseName.substring(0, firstUnderscore);
        if (typePrefix.endsWith("O") || typePrefix.endsWith("P")) {
            typePrefix = typePrefix.substring(0, typePrefix.length() - 1);
        }
        String logicalSuffix = baseName.substring(firstUnderscore + 1);
        return new ParsedFileName(typePrefix, logicalSuffix);
    }

    public static FileData fromFile(File imgFile, File polyDir, String task, String type, String webPath) {
        FileData fd = new FileData();
        fd.setName(imgFile.getName());
        fd.setId(generateId(imgFile.getName()));
        fd.setPath(imgFile.getAbsolutePath());
        fd.setTask(task);
        fd.setType(type);
        fd.setGraphic(!task.equals("history"));
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        fd.setTime(sdf.format(new Date(imgFile.lastModified())));
        fd.setUrl(buildUrl(type, task, imgFile.getName(), webPath));

        File polyFile = findSharedPolyFile(polyDir, imgFile.getName());
        if (polyFile != null) {
            fd.setPoly(readPoly(polyFile));
        } else {
            fd.setPoly(new ArrayList<>());
        }
        return fd;
    }

    private static String buildUrl(String type, String task, String fileName, String webPath) {
        return getWiredIPAddress() + webPath + "/" + type + "/" + task + "/" + fileName;
    }

    private static String generateId(String fileName) {
        String base = fileName.contains(".") ? fileName.substring(0, fileName.lastIndexOf('.')) : fileName;
        return base + "_" + Math.abs(fileName.hashCode());
    }

    private static List<LatLng> readPoly(File polyFile) {
        List<LatLng> list = new ArrayList<>();
        try {
            List<PolyCoordinateUtils.GeoPoint> points = PolyCoordinateUtils.readPoints(polyFile);
            for (PolyCoordinateUtils.GeoPoint point : points) {
                list.add(new LatLng(point.getLon(), point.getLat()));
            }
        } catch (IOException e) {
            throw new BaseException("Error reading poly file: " + polyFile.getAbsolutePath(), e);
        }
        return list;
    }

    private static final class ParsedFileName {
        private final String typePrefix;
        private final String logicalSuffix;

        private ParsedFileName(String typePrefix, String logicalSuffix) {
            this.typePrefix = typePrefix;
            this.logicalSuffix = logicalSuffix;
        }
    }
}
