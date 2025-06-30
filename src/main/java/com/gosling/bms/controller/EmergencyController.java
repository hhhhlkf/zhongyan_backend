package com.gosling.bms.controller;

import com.gosling.bms.dao.entity.file;
import com.gosling.bms.exception.BaseException;
import com.gosling.bms.response.ResponseResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static com.gosling.bms.utils.FileUtils.*;

@RestController
@Slf4j
public class EmergencyController {

    @GetMapping("/emergencydata/add")
    @ResponseResult
    public Map<String, Object> getAddData(@RequestParam("name") String name) throws IOException {
        System.out.println("我来啦");
        ArrayList<file> fileList = new ArrayList<>();
        if (name == null || "".equals(name)) {
            throw new BaseException("路径为空");
        }

        Path p = Paths.get(basePath, addPath, name);
        // 判断文件是否存在
        if (!Files.exists(p)) {
            throw new BaseException("文件不存在");
        }
        Path removeP = Paths.get(basePath, emergencyPath, name);

        BasicFileAttributes attr = Files.readAttributes(p, BasicFileAttributes.class);
        String subName = name.substring(0, name.lastIndexOf("."));
        Path poly = Paths.get(basePath, addPath, subName + ".txt");
        Path removePoly = Paths.get(basePath, emergencyPath, subName + ".txt");
        if (!Files.exists(poly)) {
            throw new BaseException(" 坐标文件不存在");
        }
        List<String> polyList = Files.readAllLines(poly);
        Float[] floats = polyList.stream()
                .flatMap(item -> {
                    String[] parts = item.split(" ");
                    return Stream.of(Float.parseFloat(parts[0]), Float.parseFloat(parts[1]));
                })
                .toArray(Float[]::new);
        file file = setFile(name, attr, emergencyPath, p, floats);
        fileList.add(file);
        // 将文件转移到emergencyPath下
        if (Files.exists(p)) {
            Files.move(p, removeP, StandardCopyOption.REPLACE_EXISTING);
            Files.move(poly, removePoly, StandardCopyOption.REPLACE_EXISTING);
        }

        HashMap<String, Object> map = new HashMap<>();
        map.put("fileList", fileList);
        return map;
    }

    @DeleteMapping("/emergencydata/del")
    @ResponseResult
    public void delData(@RequestBody ArrayList<String> fileList) throws IOException {
        if (fileList == null || fileList.isEmpty()) {
            throw new BaseException("未选中任何文件");
        }
        for (String fileName : fileList) {
            Path p = Paths.get(basePath, emergencyPath, fileName);
            Path ploy = Paths.get(basePath, emergencyPath, fileName.substring(0, fileName.lastIndexOf(".")) + ".txt");
            try {
                Files.delete(p);
                Files.delete(ploy);
            } catch (IOException e) {
                throw new BaseException(e.toString());
            }
        }
    }

    @GetMapping("/area/point")
    @ResponseResult
    public Map<String, Object> getAreaPoint(@RequestParam("folderName") String folderName) throws IOException {
        if (folderName == null || folderName.isEmpty()) {
            throw new BaseException("路径为空");
        }
        Path fullPath = Paths.get(basePath, folderName + "_point");
        // 判断文件夹是否存在
        if (!Files.exists(fullPath)) {
            throw new BaseException("文件夹不存在");
        }
        // 获取文件夹下所有除了.txt文件的文件名
        String[] fileNames = new File(fullPath.toString()).list((dir, name) -> !name.endsWith(".txt"));
        ArrayList<Map<String, Object>> pointList = new ArrayList<>();
        for (String fileName : fileNames) {
            Path p = Paths.get(basePath, folderName + "_point", fileName);
            BasicFileAttributes attr = Files.readAttributes(p, BasicFileAttributes.class);
            HashMap<String, Object> point = new HashMap<>();
            point.put("date", attr.creationTime().toString());
            point.put("info", fileName);
            point.put("url", webBasePath + "/" + folderName + "_point/" + fileName);
            point.put("type", Files.probeContentType(p));
            pointList.add(point);
            // 获取文件坐标
            Path poly = Paths.get(basePath, folderName + "_point", fileName.substring(0, fileName.lastIndexOf(".")) + ".txt");
            // 获取的是点坐标
            List<String> polyList = Files.readAllLines(poly);
            ArrayList<Float[]> parsedPolyList = new ArrayList<>();
            for (String item : polyList) {
                String[] parts = item.split(" ");
                float num1 = Float.parseFloat(parts[0]);
                float num2 = Float.parseFloat(parts[1]);
                parsedPolyList.add(new Float[]{num1, num2});
            }
            point.put("lng", parsedPolyList.get(0)[0]);
            point.put("lat", parsedPolyList.get(0)[1]);
            // 加入id属性
            String id = encodeFileName(folderName + fileName.substring(0, fileName.lastIndexOf(".")));
            point.put("id", id);

        }
        HashMap<String, Object> map = new HashMap<>();
        map.put("pointList", pointList);
        return map;
    }


    @GetMapping("/area/carousel")
    @ResponseResult
    public Map<String, Object> getAreaCarousel(@RequestParam("folderName") String folderName) throws IOException {
        if (folderName == null || folderName.isEmpty()) {
            throw new BaseException("路径为空");
        }
        Path fullPath = Paths.get(basePath, folderName + "_data");
        // 判断文件夹是否存在
        if (!Files.exists(fullPath)) {
            throw new BaseException("文件夹不存在");
        }
        // 获取文件夹下所有除了.txt文件的文件名
        String[] fileNames = new File(fullPath.toString()).list((dir, name) -> !name.endsWith(".txt"));
        ArrayList<Map<String, Object>> pointList = new ArrayList<>();
        for (String fileName : fileNames) {
            Path p = Paths.get(basePath, folderName + "_data", fileName);
            BasicFileAttributes attr = Files.readAttributes(p, BasicFileAttributes.class);
            HashMap<String, Object> point = new HashMap<>();
            point.put("date", attr.creationTime().toString());
            point.put("info", fileName);
            point.put("url", webBasePath + "/" + folderName + "_data/" + fileName);
            point.put("type", Files.probeContentType(p));

            // 加入id属性
            String id = encodeFileName(folderName + fileName.substring(0, fileName.lastIndexOf(".")));
            point.put("id", id);
            pointList.add(point);

        }
        HashMap<String, Object> map = new HashMap<>();
        map.put("pointList", pointList);
        return map;
    }
}
