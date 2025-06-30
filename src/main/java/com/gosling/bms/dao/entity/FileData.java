package com.gosling.bms.dao.entity;

import com.gosling.bms.exception.BaseException;
import lombok.Data;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static com.gosling.bms.utils.FileUtils.getWiredIPAddress;

@Data
public class FileData {
    private String id; // 文件ID
    private String name; // 文件名
    private String time; // 真正的拍照下来的时间
    private Boolean graphic; // 是否直接显示在界面上
    private List<LatLng> poly; // 图片坐标（左上角、右下角经纬度）
    private String task; // 任务类型
    private String path; // 文件路径
    private String type; // 结果类型（collect, process, history等）
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
        int lastUnderscore = fileName.lastIndexOf("_");
        int lastDot = fileName.lastIndexOf(".");
        String imgSuffix = fileName.substring(lastUnderscore + 1, lastDot);
        String imgPrefix = fileName.substring(0, lastUnderscore);
        if (imgPrefix.endsWith("O")) {
            imgPrefix = imgPrefix.substring(0, imgPrefix.length() - 1);
        }
        return imgPrefix + "P_" + imgSuffix + ".txt";
    }

    public static FileData fromFile(File imgFile, File polyDir, String task, String type, String webPath) {
        FileData fd = new FileData();
        fd.setName(imgFile.getName());
        fd.setId(generateId(imgFile.getName()));
        fd.setPath(imgFile.getAbsolutePath());
        fd.setTask(task);
        fd.setType(type);
        // 收集和处理数据不直接显示
        fd.setGraphic(!task.equals("history")); // 历史数据直接显示
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        fd.setTime(sdf.format(new Date(imgFile.lastModified())));        fd.setUrl(getWiredIPAddress() + webPath + "/" + type + "/" + task + "/" + imgFile.getName());

        String polyFileName = getPolyFileName(imgFile.getName());
        File[] polyFiles = polyDir.listFiles((dir, name) -> name.equals(polyFileName));
        if (polyFiles != null && polyFiles.length > 0) {
            fd.setPoly(readPoly(polyFiles[0]));
        } else {
            fd.setPoly(new ArrayList<>()); // 如果没有找到poly文件，设置为空列表
        }
        return fd;
    }

    // 生成唯一id
    private static String generateId(String fileName) {
        String base = fileName.contains(".") ? fileName.substring(0, fileName.lastIndexOf('.')) : fileName;
        return base + "_" + Math.abs(fileName.hashCode());
    }


    // 读取poly文件
    private static List<LatLng> readPoly(File polyFile) {
        List<LatLng> list = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(polyFile))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] arr = line.trim().split("\\s+|,");
                if (arr.length == 2) {
                    list.add(new LatLng(Double.valueOf(arr[0]), Double.valueOf(arr[1])));
                }
            }
        } catch (IOException e) {
            // 可加日志
            throw new BaseException("Error reading poly file: " + polyFile.getAbsolutePath(), e);
        }
        return list;
    }

}