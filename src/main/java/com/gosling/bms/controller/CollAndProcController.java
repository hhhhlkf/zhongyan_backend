package com.gosling.bms.controller;


import com.gosling.bms.dao.entity.file;
import com.gosling.bms.exception.BaseException;
import com.gosling.bms.response.ResponseResult;
import com.gosling.bms.utils.FileUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.gosling.bms.utils.FileUtils.*;

@RestController
@Slf4j
public class CollAndProcController {

    //    需要设置一个全局变量，用于存储文件的基础路径
    private Process newPyProcess;
    private Process uavNewPyProcess;
    private Process getPyProcess;


    @GetMapping("/area/axis")
    @ResponseResult
    public Map<String, Object> getAreaAxis(@RequestParam("folderName") String folderName) {
        if (folderName == null || folderName.equals("")) {
            throw new BaseException("参数为空");
        }
        Path polyPath = Paths.get(basePath, folderName, "poly.txt");
        if (!Files.exists(polyPath)) {
            throw new BaseException("文件不存在");
        }
        List<String> polyList;
        try {
            polyList = Files.readAllLines(polyPath);
        } catch (IOException e) {
            throw new BaseException("读取文件失败");
        }
        List<Float[]> parsedPolyList = new ArrayList<>();
        // 解析poly.txt文档，只解析前两行
        for (int i = 0; i < 4; i++) {
            if (i == 2) {
                continue;
            }
            String item = polyList.get(i);
            String[] parts = item.split(" ");
            float num1 = Float.parseFloat(parts[0]);
            float num2 = Float.parseFloat(parts[1]);
            parsedPolyList.add(new Float[]{num1, num2});
        }
        // 解析最后一行
        String divideNum = polyList.get(2);
        String[] divideParts = divideNum.split(" ");
        int divideX = Integer.parseInt(divideParts[0]);
        int divideY = Integer.parseInt(divideParts[1]);
        // 解析第四行，是坐标
        Map<String, Object> map = new HashMap<>();
        map.put("polyList", parsedPolyList);
        map.put("divideX", divideX);
        map.put("divideY", divideY);
        return map;
    }

    @GetMapping("/process1/control")
    @ResponseResult
    public Map<String, Object> processControl(@RequestParam("ctrlInst") String ctrlInst,
                                              @RequestParam("isOpen") Boolean isOpen,
                                              @RequestParam("isTest") Boolean isTest,
                                              @RequestParam("folderName") String folderName) {
        System.out.println(ctrlInst);
        System.out.println(isOpen);
        Random random = new Random();
        if (ctrlInst == null || ctrlInst.equals("") || isOpen == null) {
            throw new BaseException("参数为空");
        }
        String[] instructionsArray = ctrlInst.split(",");
        // 将数组转换为列表
        List<String> instructions = Arrays.asList(instructionsArray);

        // 检查列表中是否包含特定的字符串，并输出相应的消息
        if (instructions.contains("cp") && isOpen) {
            System.out.println("采集处理传输开始");
            if (isTest == null || folderName == null || folderName.equals("")) {
                throw new BaseException("参数为空");
            }
            if (isTest) {
                System.out.println("测试传输开始");
                String workingDir = basePath + "/" + folderName;
                try {
                    newPyProcess = new ProcessBuilder("python", "new.py")
                            .directory(new File(workingDir))
                            .start();
                    if (newPyProcess.isAlive()) {
                        System.out.println("new.py 脚本启动成功");
                    } else {
                        throw new BaseException("new.py 脚本启动失败");
                    }
                } catch (Exception e) {
                    throw new BaseException("文件不存在");
                }

                String uavWorkingDir = basePath + "/" + folderName + "_uav";
                try {
                    uavNewPyProcess = new ProcessBuilder("conda", "run", "-n", "base", "python", "new.py")
                            .directory(new File(uavWorkingDir))
                            .start();
                    if (uavNewPyProcess.isAlive()) {
                        System.out.println("new.py 脚本启动成功");
                    } else {
                        throw new BaseException("new.py 脚本启动失败");
                    }
                } catch (Exception e) {
                    throw new BaseException("执行 new.py 脚本时发生错误：" + e.getMessage(), e);
                }
            } else {
                System.out.println("处理开始");
                String comm = "cd /home/nvidia/software/nx_k8s\nbash start_changeOS.sh";
                sendCommand(comm);
                movePngFiles(basePath, new String[]{folderName, folderName + "_uav"});
                // 延迟一小会儿
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                System.out.println("启动 get.py 脚本");
                try {
                    List<String> command = new ArrayList<>();
                    command.add("conda");
                    command.add("run");
                    command.add("-n");
                    command.add("base");
                    command.add("python");
                    command.add("get.py");
                    // 添加脚本参数
                    command.add(folderName);
                    command.add(folderName + "_uav");
                    System.out.println("开始创建进程");
                    ProcessBuilder processBuilder = new ProcessBuilder(command);
                    processBuilder.directory(new File(basePath));
                    getPyProcess = processBuilder.start();
                    System.out.println("进程开始");
                    Executors.newSingleThreadExecutor().submit(() -> {
                        try (BufferedReader reader = new BufferedReader(new InputStreamReader(getPyProcess.getInputStream()))) {
                            String line;
                            while ((line = reader.readLine()) != null) {
                                System.out.println("Output: " + line);
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    });
                    System.out.println("进程开始2");
                    Executors.newSingleThreadExecutor().submit(() -> {
                        try (BufferedReader reader = new BufferedReader(new InputStreamReader(getPyProcess.getErrorStream()))) {
                            String line;
                            while ((line = reader.readLine()) != null) {
                                System.err.println("Error: " + line);
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    });
                    // 检查进程是否启动成功
                    if (getPyProcess.isAlive()) {
                        System.out.println("get.py 脚本启动成功");
                    } else {
                        throw new BaseException("get.py 脚本启动失败");
                    }
                } catch (Exception e) {
                    throw new BaseException("执行 get.py 脚本时发生错误：" + e.getMessage(), e);
                }
            }
        } else if (instructions.contains("tr") && isOpen) {
            System.out.println("采集开始");
            String comm = "1 25";
//            sendCommandTr(comm);
        } else if (instructions.contains("tr") && !isOpen) {
            System.out.println("采集关闭");
            String comm = "0";
//            sendCommandTr(comm);
        } else if (instructions.contains("cp") && !isOpen) {
            System.out.println("处理关闭");
            if (newPyProcess != null && newPyProcess.isAlive()) {
                newPyProcess.destroyForcibly();
                System.out.println("new.py 脚本已停止");
            }
            if (uavNewPyProcess != null && uavNewPyProcess.isAlive()) {
                uavNewPyProcess.destroyForcibly();
                System.out.println("uav_new.py 脚本已停止");
            }
            if (getPyProcess != null && getPyProcess.isAlive()) {
                killProcess(getPyProcess, "get.py");
                getPyProcess.destroyForcibly();
                System.out.println("get.py 脚本已停止");
            }
            String comm = "cd /home/nvidia/software/nx_k8s/master\nbash changeos_shutdown.sh";
            sendCommandTr(comm);
        } else {
            throw new BaseException("信息传输错误");
        }

        Map<String, Object> map = new HashMap<>();
        map.put("status", true);
        return map;

    }

    private static void killProcess(Process process, String scriptName) {
        if (process != null && process.isAlive()) {
            try {
                // 杀掉所有 python.exe 进程
                ProcessBuilder pb = new ProcessBuilder("taskkill", "/F", "/IM", "python.exe");
                Process killProcess = pb.start();
                killProcess.waitFor();
                System.out.println("所有 python.exe 进程已被杀掉");
            } catch (Exception e) {
                System.err.println("无法杀掉 python.exe 进程: " + e);
            }
        }
    }


    @GetMapping("/process1/result")
    @ResponseResult
    public Map<String, Object> processResult(@RequestParam("fileList") List<String> fileList,
                                             @RequestParam("isAll") Boolean isAll) {
        // 输出getPyProcess的输出
        if (fileList == null || fileList.isEmpty()) {
            throw new BaseException("输入错误");
        }
        //如果fileList中有一个"*"，则将fileList中的所有文件夹都进行处理
        if (fileList.contains("*") && isAll) {
            fileList = Arrays.stream(Objects.requireNonNull(new java.io.File(basePath).list()))
                    .filter(fileName -> !fileName.equals("process2"))
                    .collect(Collectors.toList());
        }
        ArrayList<String> files = new ArrayList<>();
        List<Path> newFiles;
        ArrayList<file> fileArrayList = new ArrayList<>();
        for (String folderName : fileList) {
            Path fullPath = Paths.get(basePath, folderName);
            if (!Files.exists(fullPath) || !Files.isDirectory(fullPath)) {
                System.out.println("Path does not exist: " + fullPath);
//                throw new BaseException("不存在上述的文件夹");
                continue;
            }
            if (!isAll) {
                Path newFilePath = Paths.get(fullPath.toString(), "new.txt");
                Path polyPath = Paths.get(fullPath.toString(), "poly.txt");
                try {
                    List<String> newFileNames = Files.readAllLines(newFilePath);
                    List<String> polyList = Files.readAllLines(polyPath);
                    List<Float[]> parsedPolyList = new ArrayList<>();
                    // 解析poly.txt文档，只解析前两行
                    for (int i = 0; i < 2; i++) {
                        String item = polyList.get(i);
                        String[] parts = item.split(" ");
                        float num1 = Float.parseFloat(parts[0]);
                        float num2 = Float.parseFloat(parts[1]);
                        parsedPolyList.add(new Float[]{num1, num2});
                    }
                    // 解析最后一行
                    String divideNum = polyList.get(2);
                    String[] divideParts = divideNum.split(" ");
                    int divideX = Integer.parseInt(divideParts[0]);
                    int divideY = Integer.parseInt(divideParts[1]);
                    ArrayList<Integer> LineMark = new ArrayList<>();
                    for (int i = 0; i < newFileNames.size(); i++) {
                        String newFileName = newFileNames.get(i);
                        System.out.println("New file name from new.txt: " + newFileName);
                        String nameWithoutExtension = newFileName.replaceFirst("[.][^.]+$", "");
                        int[] polyXY = parseRowAndColumn(nameWithoutExtension);
                        Float[] result = FileUtils.retPoly(divideX, divideY, parsedPolyList, polyXY[0], polyXY[1]);
                        Path newFile = Paths.get(fullPath.toString(), newFileName);
                        BasicFileAttributes attr = Files.readAttributes(newFile, BasicFileAttributes.class);
//                        String url = "http://" +webBasePath + "/" + folderName + "/" + newFileName;
//                        if (isURLAccessible(url)) {
//                            System.out.println("URL is accessible: " + url);
//                        } else {
//                            System.out.println("URL is not accessible: " + url);
//                            continue;
//                        }
                        file f = setFile(newFileName, attr, folderName, newFile, result);
                        LineMark.add(i);
                        fileArrayList.add(f);
                    }
                    Files.write(newFilePath, new byte[0]);
//                    ArrayList<String> remainingLines = new ArrayList<>();
//                    for (int i = 0; i < newFileNames.size(); i++) {
//                        if (!LineMark.contains(i)) {
//                            remainingLines.add(newFileNames.get(i));
//                        }
//                    }
//                    Files.write(newFilePath, remainingLines);
                    // System.out.println("new.txt file cleared successfully.");

                } catch (IOException e) {
                    System.err.println("Failed to read or clear new.txt file: " + newFilePath);
                    throw new BaseException(e);
                }
            } else {
                try (Stream<Path> paths = Files.list(fullPath)) {
                    // 获取当前文件夹下的poly.txt文档
                    Path polyPath = paths.filter(Files::isRegularFile)
                            .filter(path -> path.toString().endsWith("poly.txt"))
                            .findFirst()
                            .orElse(null);
                    if (polyPath == null) {
                        Stream<Path> pathStream = Files.list(fullPath);
                        newFiles = pathStream.filter(Files::isRegularFile)
                                .filter(path -> !path.toString().endsWith(".txt"))
                                .filter(path -> !path.toString().endsWith(".py"))
                                .collect(Collectors.toList());
                        for (Path newFile : newFiles) {
                            System.out.println("New file: " + newFile);
                            String nameWithoutExtension = newFile.getFileName().toString().replaceFirst("[.][^.]+$", "");
                            Path poly = Paths.get(fullPath.toString(), nameWithoutExtension + ".txt");
                            List<String> polyList = Files.readAllLines(poly);
                            Float[] result = polyList.stream()
                                    .map(item -> item.split(" "))
                                    .map(parts -> new Float[]{Float.parseFloat(parts[0]), Float.parseFloat(parts[1])})
                                    .flatMap(Arrays::stream)
                                    .toArray(Float[]::new);
                            BasicFileAttributes attr = Files.readAttributes(newFile, BasicFileAttributes.class);
                            file f = setFile(newFile.getFileName().toString(), attr, folderName, newFile, result);
                            fileArrayList.add(f);
                        }
                        continue;
                    }
                    List<String> polyList = Files.readAllLines(polyPath);
                    List<Float[]> parsedPolyList = new ArrayList<>();
                    // 解析poly.txt文档，只解析前两行
                    for (int i = 0; i < 2; i++) {
                        String item = polyList.get(i);
                        String[] parts = item.split(" ");
                        float num1 = Float.parseFloat(parts[0]);
                        float num2 = Float.parseFloat(parts[1]);
                        parsedPolyList.add(new Float[]{num1, num2});
                    }
                    // 解析最后一行
                    String divideNum = polyList.get(2);
                    String[] divideParts = divideNum.split(" ");
                    int divideX = Integer.parseInt(divideParts[0]);
                    int divideY = Integer.parseInt(divideParts[1]);
                    Stream<Path> paths2 = Files.list(fullPath);
                    newFiles = paths2.filter(Files::isRegularFile)
                            .filter(path -> !path.toString().endsWith(".txt"))
                            .filter(path -> !path.toString().endsWith(".py"))
                            .collect(Collectors.toList());
                    for (Path newFile : newFiles) {
                        System.out.println("New file: " + newFile);
//                        files.add(newFile.toString());
                        String nameWithoutExtension = newFile.getFileName().toString().replaceFirst("[.][^.]+$", "");
                        int[] polyXY = parseRowAndColumn(nameWithoutExtension);
                        Float[] result = FileUtils.retPoly(divideX, divideY, parsedPolyList, polyXY[0], polyXY[1]);
                        System.out.println(fullPath);
//                        try (BufferedWriter writer = new BufferedWriter(new FileWriter(fullPath.toString() + '\\' + nameWithoutExtension + ".txt", false))) {
//                            // 写入结果到文件
//                            for (int i = 0; i < result.length; i += 2) {
//                                writer.write(result[i] + " " + result[i + 1]);
//                                writer.newLine();
//                            }
//                            System.out.println("Result written to file successfully.");
//                        } catch (IOException e) {
//                            e.printStackTrace();
//                        }
                        BasicFileAttributes attr = Files.readAttributes(newFile, BasicFileAttributes.class);

                        file f = setFile(newFile.getFileName().toString(), attr, folderName, newFile, result);
                        fileArrayList.add(f);
                    }
                } catch (IOException e) {
                    System.err.println("Failed to list files in directory: " + fullPath);
                    throw new BaseException(e);
                }
            }

        }

        HashMap<String, Object> map = new HashMap<>();
        map.put("fileList", fileArrayList);
        return map;
    }

}

