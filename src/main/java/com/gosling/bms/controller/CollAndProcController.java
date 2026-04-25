package com.gosling.bms.controller;

import com.gosling.bms.dao.entity.file;
import com.gosling.bms.exception.BaseException;
import com.gosling.bms.response.ResponseResult;
import com.gosling.bms.service.FolderMonitorService;
import com.gosling.bms.utils.FileUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.gosling.bms.utils.FileUtils.basePath;
import static com.gosling.bms.utils.FileUtils.movePngFiles;
import static com.gosling.bms.utils.FileUtils.parseRowAndColumn;
import static com.gosling.bms.utils.FileUtils.sendCommand;
import static com.gosling.bms.utils.FileUtils.sendCommandTr;
import static com.gosling.bms.utils.FileUtils.setFile;

@RestController
@Slf4j
public class CollAndProcController {

    private Process newPyProcess;
    private Process uavNewPyProcess;

    @Autowired
    private FolderMonitorService folderMonitorService;

    @GetMapping("/area/axis")
    @ResponseResult
    public Map<String, Object> getAreaAxis(@RequestParam("folderName") String folderName) {
        if (folderName == null || folderName.isEmpty()) {
            throw new BaseException("folderName is required");
        }

        Path polyPath = Paths.get(basePath, folderName, "poly.txt");
        if (!Files.exists(polyPath)) {
            throw new BaseException("poly.txt not found");
        }

        List<String> polyList;
        try {
            polyList = Files.readAllLines(polyPath);
        } catch (IOException e) {
            throw new BaseException("Failed to read poly.txt", e);
        }

        List<Float[]> parsedPolyList = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            if (i == 2) {
                continue;
            }
            String[] parts = polyList.get(i).split(" ");
            parsedPolyList.add(new Float[]{Float.parseFloat(parts[0]), Float.parseFloat(parts[1])});
        }

        String[] divideParts = polyList.get(2).split(" ");
        int divideX = Integer.parseInt(divideParts[0]);
        int divideY = Integer.parseInt(divideParts[1]);

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
        if (ctrlInst == null || ctrlInst.isEmpty() || isOpen == null) {
            throw new BaseException("Invalid control parameters");
        }

        List<String> instructions = Arrays.asList(ctrlInst.split(","));

        if (instructions.contains("cp") && isOpen) {
            if (isTest == null || folderName == null || folderName.isEmpty()) {
                throw new BaseException("folderName and isTest are required");
            }

            if (isTest) {
                startTestScripts(folderName);
            } else {
                startProcessAndMonitor(folderName);
            }
        } else if (instructions.contains("tr") && isOpen) {
            log.info("Transport open requested");
        } else if (instructions.contains("tr") && !isOpen) {
            log.info("Transport close requested");
        } else if (instructions.contains("cp") && !isOpen) {
            stopProcessAndMonitor();
        } else {
            throw new BaseException("Unsupported control instruction");
        }

        Map<String, Object> map = new HashMap<>();
        map.put("status", true);
        return map;
    }

    private void startTestScripts(String folderName) {
        String workingDir = basePath + "/" + folderName;
        try {
            newPyProcess = new ProcessBuilder("python", "new.py")
                    .directory(new File(workingDir))
                    .start();
            if (!newPyProcess.isAlive()) {
                throw new BaseException("new.py failed to start");
            }
        } catch (Exception e) {
            throw new BaseException("Failed to execute new.py", e);
        }

        String uavWorkingDir = basePath + "/" + folderName + "_uav";
        try {
            uavNewPyProcess = new ProcessBuilder("conda", "run", "-n", "base", "python", "new.py")
                    .directory(new File(uavWorkingDir))
                    .start();
            if (!uavNewPyProcess.isAlive()) {
                throw new BaseException("uav new.py failed to start");
            }
        } catch (Exception e) {
            throw new BaseException("Failed to execute uav new.py", e);
        }
    }

    private void startProcessAndMonitor(String folderName) {
        String comm = "cd /home/nvidia/software/nx_k8s\nbash start_changeOS.sh";
        sendCommand(comm);
        movePngFiles(basePath, new String[]{folderName, folderName + "_uav"});

        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BaseException("Interrupted before starting folder monitor", e);
        }

        try {
            folderMonitorService.startMonitor(folderName, folderName + "_uav");
            if (!folderMonitorService.isRunning()) {
                throw new BaseException("Folder monitor failed to start");
            }
        } catch (Exception e) {
            throw new BaseException("Failed to start Java folder monitor", e);
        }
    }

    private void stopProcessAndMonitor() {
        if (newPyProcess != null && newPyProcess.isAlive()) {
            newPyProcess.destroyForcibly();
        }
        if (uavNewPyProcess != null && uavNewPyProcess.isAlive()) {
            uavNewPyProcess.destroyForcibly();
        }

        folderMonitorService.stopMonitor();

        String comm = "cd /home/nvidia/software/nx_k8s/master\nbash changeos_shutdown.sh";
        sendCommandTr(comm);
    }

    @GetMapping("/process1/result")
    @ResponseResult
    public Map<String, Object> processResult(@RequestParam("fileList") List<String> fileList,
                                             @RequestParam("isAll") Boolean isAll) {
        if (fileList == null || fileList.isEmpty()) {
            throw new BaseException("fileList is required");
        }

        if (fileList.contains("*") && isAll) {
            fileList = Arrays.stream(Objects.requireNonNull(new File(basePath).list()))
                    .filter(fileName -> !fileName.equals("process2"))
                    .collect(Collectors.toList());
        }

        List<Path> newFiles;
        ArrayList<file> fileArrayList = new ArrayList<>();
        for (String folderName : fileList) {
            Path fullPath = Paths.get(basePath, folderName);
            if (!Files.exists(fullPath) || !Files.isDirectory(fullPath)) {
                log.warn("Path does not exist: {}", fullPath);
                continue;
            }

            if (!isAll) {
                Path newFilePath = Paths.get(fullPath.toString(), "new.txt");
                Path polyPath = Paths.get(fullPath.toString(), "poly.txt");
                try {
                    List<String> newFileNames = Files.readAllLines(newFilePath);
                    List<String> polyList = Files.readAllLines(polyPath);
                    List<Float[]> parsedPolyList = new ArrayList<>();
                    for (int i = 0; i < 2; i++) {
                        String[] parts = polyList.get(i).split(" ");
                        parsedPolyList.add(new Float[]{Float.parseFloat(parts[0]), Float.parseFloat(parts[1])});
                    }

                    String[] divideParts = polyList.get(2).split(" ");
                    int divideX = Integer.parseInt(divideParts[0]);
                    int divideY = Integer.parseInt(divideParts[1]);

                    for (String newFileName : newFileNames) {
                        String nameWithoutExtension = newFileName.replaceFirst("[.][^.]+$", "");
                        int[] polyXY = parseRowAndColumn(nameWithoutExtension);
                        Float[] result = FileUtils.retPoly(divideX, divideY, parsedPolyList, polyXY[0], polyXY[1]);
                        Path newFile = Paths.get(fullPath.toString(), newFileName);
                        BasicFileAttributes attr = Files.readAttributes(newFile, BasicFileAttributes.class);
                        fileArrayList.add(setFile(newFileName, attr, folderName, newFile, result));
                    }
                    Files.write(newFilePath, new byte[0]);
                } catch (IOException e) {
                    log.error("Failed to read or clear new.txt file: {}", newFilePath, e);
                    throw new BaseException(e);
                }
            } else {
                try (Stream<Path> paths = Files.list(fullPath)) {
                    Path polyPath = paths.filter(Files::isRegularFile)
                            .filter(path -> path.toString().endsWith("poly.txt"))
                            .findFirst()
                            .orElse(null);
                    if (polyPath == null) {
                        try (Stream<Path> pathStream = Files.list(fullPath)) {
                            newFiles = pathStream.filter(Files::isRegularFile)
                                    .filter(path -> !path.toString().endsWith(".txt"))
                                    .filter(path -> !path.toString().endsWith(".py"))
                                    .collect(Collectors.toList());
                        }
                        for (Path newFile : newFiles) {
                            String nameWithoutExtension = newFile.getFileName().toString().replaceFirst("[.][^.]+$", "");
                            Path poly = Paths.get(fullPath.toString(), nameWithoutExtension + ".txt");
                            List<String> polyList = Files.readAllLines(poly);
                            Float[] result = polyList.stream()
                                    .map(item -> item.split(" "))
                                    .map(parts -> new Float[]{Float.parseFloat(parts[0]), Float.parseFloat(parts[1])})
                                    .flatMap(Arrays::stream)
                                    .toArray(Float[]::new);
                            BasicFileAttributes attr = Files.readAttributes(newFile, BasicFileAttributes.class);
                            fileArrayList.add(setFile(newFile.getFileName().toString(), attr, folderName, newFile, result));
                        }
                        continue;
                    }

                    List<String> polyList = Files.readAllLines(polyPath);
                    List<Float[]> parsedPolyList = new ArrayList<>();
                    for (int i = 0; i < 2; i++) {
                        String[] parts = polyList.get(i).split(" ");
                        parsedPolyList.add(new Float[]{Float.parseFloat(parts[0]), Float.parseFloat(parts[1])});
                    }

                    String[] divideParts = polyList.get(2).split(" ");
                    int divideX = Integer.parseInt(divideParts[0]);
                    int divideY = Integer.parseInt(divideParts[1]);

                    try (Stream<Path> paths2 = Files.list(fullPath)) {
                        newFiles = paths2.filter(Files::isRegularFile)
                                .filter(path -> !path.toString().endsWith(".txt"))
                                .filter(path -> !path.toString().endsWith(".py"))
                                .collect(Collectors.toList());
                    }

                    for (Path newFile : newFiles) {
                        String nameWithoutExtension = newFile.getFileName().toString().replaceFirst("[.][^.]+$", "");
                        int[] polyXY = parseRowAndColumn(nameWithoutExtension);
                        Float[] result = FileUtils.retPoly(divideX, divideY, parsedPolyList, polyXY[0], polyXY[1]);
                        BasicFileAttributes attr = Files.readAttributes(newFile, BasicFileAttributes.class);
                        fileArrayList.add(setFile(newFile.getFileName().toString(), attr, folderName, newFile, result));
                    }
                } catch (IOException e) {
                    log.error("Failed to list files in directory: {}", fullPath, e);
                    throw new BaseException(e);
                }
            }
        }

        HashMap<String, Object> map = new HashMap<>();
        map.put("fileList", fileArrayList);
        return map;
    }
}
