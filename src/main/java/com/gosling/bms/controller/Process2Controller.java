package com.gosling.bms.controller;

import com.gosling.bms.dao.entity.fileSet;
import com.gosling.bms.exception.BaseException;
import com.gosling.bms.response.ResponseResult;
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
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.gosling.bms.utils.FileUtils.*;

@RestController
@Slf4j
public class Process2Controller {

    private Process model3Process;
    private Process model1Process;
    private Integer imageNum = 0;
    private String stage2folder = "process2";
    private Process model2Process;

    @GetMapping("/process2/result")
    @ResponseResult
    public Map<String, Object> processResult(@RequestParam("path") String path,
                                             @RequestParam("flag") Integer flag) throws IOException, InterruptedException {
        Path filePath = Paths.get(basePath, stage2folder, stage2FolderInput, "process2.txt");
        if (flag == 1) {
            imageNum++;
            if(imageNum > 2){
                imageNum = 0;
                Files.write(filePath, new byte[0], StandardOpenOption.TRUNCATE_EXISTING);
                throw new BaseException("文件上传不能超过两个");
            }
            System.out.println("进行配准");
            if(Files.exists(filePath)){
                Files.write(filePath, (path + System.lineSeparator()).getBytes(), StandardOpenOption.APPEND);
                List<String> lines = Files.readAllLines(filePath);
                System.out.println("文件包含 " + lines.size() + " 行");
                if(lines.size() == 2){
                    // 读取这两行
                    String file1 = lines.get(0);
                    String file2 = lines.get(1);
                    System.out.println("Line 1: " + file1);
                    System.out.println("Line 2: " + file2);
                    if(file1.endsWith(".tif") && file2.endsWith(".tif")){
                        System.out.println("文件后缀均为tif，继续处理...");
                    }
                    else {
                        System.out.println("文件后缀不一致或不都是tif，返回...");
                        imageNum = 0;
                        Files.write(filePath, new byte[0], StandardOpenOption.TRUNCATE_EXISTING);
                        throw new BaseException("文件后缀不一致或不都是tif");
                    }
                    Files.write(filePath, new byte[0], StandardOpenOption.TRUNCATE_EXISTING);
                    imageNum = 0;
                    System.out.println("文件已清空");
                    String workingDir = "../model/registration";
                    ProcessBuilder processBuilder = new ProcessBuilder("cmd.exe", "/c","conda", "--no-plugins", "run", "-n", "model_1", "python", "registration_yeh.py",
                            "--sour_name", file1,
                            "--tar_name", file2,
                            "--output_name", "reg_" + file1.substring(0, file1.lastIndexOf('.')) + ".png")
                            .directory(new File(workingDir));
                    Map<String, String> env = processBuilder.environment();
                    List<String> command = processBuilder.command();
                    // 将命令列表转换为字符串
                    String commandString = String.join(" ", command);
                    // 输出拼接后的指令
                    System.out.println("拼接后的指令: " + commandString);
                    model1Process = processBuilder.start();
                    // 监控进程
                    // 创建一个线程来读取进程的输出
                    Thread outputThread = new Thread(() -> {
                        try (BufferedReader reader = new BufferedReader(new InputStreamReader(model1Process.getInputStream()))) {
                            String line;
                            while ((line = reader.readLine()) != null) {
                                System.out.println(line);
                            }
                        } catch (IOException e) {
                            throw new BaseException(e);
                        }
                    });

                    Thread errorThread = new Thread(() -> {
                        try (BufferedReader reader = new BufferedReader(new InputStreamReader(model1Process.getErrorStream()))) {
                            String line;
                            while ((line = reader.readLine()) != null) {
                                System.err.println(line);
                            }
                        } catch (IOException e) {
                            throw new BaseException(e);
                        }
                    });

                    // 启动线程
                    outputThread.start();
                    errorThread.start();
                    int exitCode = model1Process.waitFor();
                    System.out.println("Process exited with code: " + exitCode);

                    // 等待线程结束
                    outputThread.join();
                    errorThread.join();
                }
            }
        } else if (flag == 2) {
            String workingDir = "../model/zy";
            // 判断path是不是tif文件
            if (!path.endsWith(".tif")) {
                throw new BaseException("文件后缀不是tif");
            }
            ProcessBuilder processBuilder = new ProcessBuilder("conda", "run", "-n", "model_1", "python", "Slid_predict_gpu.py",
                    "--file_name", path)
                    .directory(new File(workingDir));
            model2Process = processBuilder.start();
            // 监控进程
            // 创建一个线程来读取进程的输出
            Thread outputThread = new Thread(() -> {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(model2Process.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        System.out.println(line);
                    }
                } catch (IOException e) {
                    throw new BaseException(e);
                }
            });

            // 创建一个线程来读取进程的错误输出
            Thread errorThread = new Thread(() -> {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(model2Process.getErrorStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        System.err.println(line);
                    }
                } catch (IOException e) {
                    throw new BaseException(e);
                }
            });

            // 启动线程
            outputThread.start();
            errorThread.start();
            int exitCode = model2Process.waitFor();
            System.out.println("Process exited with code: " + exitCode);

            // 等待线程结束
            outputThread.join();
            errorThread.join();


        } else if (flag == 3) {
            imageNum++;
            if(imageNum > 2){
                imageNum = 0;
                Files.write(filePath, new byte[0], StandardOpenOption.TRUNCATE_EXISTING);
                throw new BaseException("文件上传不能超过两个");
            }
            System.out.println("进行损毁评估");
//            BasicFileAttributes attr = Files.readAttributes(p, BasicFileAttributes.class);
            try {
                // 检查文件是否存在
                if (Files.exists(filePath)) {
                    // 读取文件内容
                    Files.write(filePath, (path + System.lineSeparator()).getBytes(), StandardOpenOption.APPEND);
                    // 检查文件是否包含两行
                    List<String> lines = Files.readAllLines(filePath);
                    System.out.println("文件包含 " + lines.size() + " 行");
                    if (lines.size() == 2) {
                        // 读取这两行
                        String file1 = lines.get(0);
                        String file2 = lines.get(1);
                        System.out.println("Line 1: " + file1);
                        System.out.println("Line 2: " + file2);
                        //如果file1的开头是pre_，则将file1和file2交换
                        if (file1.startsWith("pre_")) {
                            String temp = file1;
                            file1 = file2;
                            file2 = temp;
                        }
                        // 进一步判断file1和file2的开头一定分别是post和pre然后后缀完全一致
                        if (file1.startsWith("post_") && file2.startsWith("pre_")) {
                            String suffix1 = file1.substring(5); // 去掉"post_"前缀
                            String suffix2 = file2.substring(4); // 去掉"pre_"前缀

                            if (suffix1.equals(suffix2) && suffix1.endsWith(".png") && suffix2.endsWith(".png")) {
                                System.out.println("文件后缀一致，继续处理...");
                                // 继续处理逻辑
                            } else {
                                System.out.println("文件后缀不一致或不是png，返回...");
                                imageNum = 0;
                                Files.write(filePath, new byte[0], StandardOpenOption.TRUNCATE_EXISTING);
                                throw new BaseException("文件后缀不一致或不都是png");
                            }
                        } else {
                            System.out.println("文件前缀不符合要求，返回...");
                            imageNum = 0;
                            Files.write(filePath, new byte[0], StandardOpenOption.TRUNCATE_EXISTING);
                            throw new BaseException("文件前缀不符合要求");
                        }
                        // 清空文件
                        Files.write(filePath, new byte[0], StandardOpenOption.TRUNCATE_EXISTING);
                        imageNum = 0;
                        System.out.println("文件已清空");
                        String workingDir = "../model/changeOS";
                        List<String> strings = processFileName(file1);
                        // 根据strings中的值生成新的文件名
                        Path filePolyPath1 = Paths.get(basePath, emergencyPath, strings.get(0));
                        Path filePolyPath2 = Paths.get(basePath, emergencyPath, strings.get(1));
                        // 如果不存在，则创建filePath1文件和filePath2文件
                        // 将inputPath文件的内容写入filePolyPath1文件和filePolyPath2文件
                        Path inputPath = Paths.get(basePath, stage2folder, stage2FolderInput, file1.substring(0, file1.lastIndexOf('.')) + ".txt");
                        if (!Files.exists(filePolyPath1)) {
                            Files.createFile(filePolyPath1);
                            Files.write(filePolyPath1, Files.readAllBytes(inputPath));
                        }
                        if (!Files.exists(filePolyPath2)) {
                            Files.createFile(filePolyPath2);
                            Files.write(filePolyPath2, Files.readAllBytes(inputPath));
                        }

                        ProcessBuilder processBuilder = new ProcessBuilder("conda", "run", "-n", "model_3", "python", "eval.py",
                                "--image_dir", "../../business_management_system_backend/src/main/resources/static/images/process2/input/",
                                "--post_img", file1,
                                "--pre_img", file2)
                                .directory(new File(workingDir));
                        model3Process = processBuilder.start();
                        // 监控进程
                        // 创建一个线程来读取进程的输出
                        Thread outputThread = new Thread(() -> {
                            try (BufferedReader reader = new BufferedReader(new InputStreamReader(model3Process.getInputStream()))) {
                                String line;
                                while ((line = reader.readLine()) != null) {
                                    System.out.println(line);
                                }
                            } catch (IOException e) {
                                throw new BaseException(e);
                            }
                        });

                        // 创建一个线程来读取进程的错误输出
                        Thread errorThread = new Thread(() -> {
                            try (BufferedReader reader = new BufferedReader(new InputStreamReader(model3Process.getErrorStream()))) {
                                String line;
                                while ((line = reader.readLine()) != null) {
                                    System.err.println(line);
                                }
                            } catch (IOException e) {
                                throw new BaseException(e);
                            }
                        });

                        // 启动线程
                        outputThread.start();
                        errorThread.start();
                        int exitCode = model3Process.waitFor();
                        System.out.println("Process exited with code: " + exitCode);

                        // 等待线程结束
                        outputThread.join();
                        errorThread.join();
                    } else {
                        System.out.println("文件不包含两行");
                    }
                } else {
                    System.out.println("文件不存在");
                }
            } catch (IOException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

        }
        HashMap<String, Object> map = new HashMap<>();
//        map.put("fileList", fileList);
        map.put("status", true);
        return map;
    }

    @GetMapping("/process2/list")
    @ResponseResult
    public Map<String, Object> processList() throws IOException {
        List<Path> newFiles;
        ArrayList<fileSet> fileArrayList = new ArrayList<>();

        Path fullPathInput = Paths.get(basePath, stage2folder, stage2FolderInput);
        if (!Files.exists(fullPathInput)) {
            System.out.println("Path does not exist: " + fullPathInput);
            throw new BaseException("不存在上述的文件夹");
        }
        try (Stream<Path> paths = Files.list(fullPathInput)) {
            newFiles = paths.filter(Files::isRegularFile)
                    .filter(path -> !path.toString().endsWith(".txt"))
                    .collect(Collectors.toList());
            for (Path newFile : newFiles) {
                System.out.println("New file: " + newFile);
                BasicFileAttributes attr = Files.readAttributes(newFile, BasicFileAttributes.class);
                fileSet f = setFileSet(String.valueOf(newFile.getFileName()), attr, newFile);
                fileArrayList.add(f);
            }
        } catch (IOException e) {
            System.err.println("Failed to list files in directory: " + fullPathInput);
            throw new BaseException(e);
        }
        HashMap<String, Object> map = new HashMap<>();
        map.put("fileList", fileArrayList);
        return map;
    }

}
