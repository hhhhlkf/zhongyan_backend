package com.gosling.bms.utils;

import com.gosling.bms.dao.entity.file;
import com.gosling.bms.dao.entity.fileSet;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Enumeration;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FileUtils {
    public static final String basePath = "./src/main/resources/static/images";
    //    public static final String webBasePath = "localhost:8088/v2/static";
    public static final String webBasePath = getWiredIPAddress() + ":8088/v2/static";
    //    Paths.get("").toAbsolutePath().getParent().resolve("web/public/image").toString()
    public static final String setBasePath = "image";
    public static final ArrayList<String> mark;
    public static final String stage2folder = "process2";
    public static final String stage2FolderResult = "result";
    public static final String stage2FolderInput = "input";
    public static final String emergencyPath = "emergency";
    public static final String addPath = "emergency2";
    public static final String sendPath = "add";
    public static Integer num = 1;
    public static String ipAddress = null;

    static {
        // 初始化常量
        mark = new ArrayList<>(List.of("_1", "_2", "_3"));
    }

    public static String encodeFileName(String fileName) {
        return Base64.getEncoder().encodeToString(fileName.getBytes());
    }

    // 解码 Base64 为文件名
    public static String decodeFileName(String encodedFileName) {
        byte[] decodedBytes = Base64.getDecoder().decode(encodedFileName);
        return new String(decodedBytes);
    }

    public static float convertSizeToMB(long sizeInBytes) {
        // 将字节转换为 MB
        BigDecimal sizeInMB = BigDecimal.valueOf(sizeInBytes).divide(BigDecimal.valueOf(1024), 2, RoundingMode.HALF_UP);
        // 格式化为两位小数
        return Float.parseFloat(String.format("%.2f", sizeInMB.floatValue()));
    }

    public static String getWiredIPAddress() {
        if (ipAddress != null) {
            return ipAddress;
        }
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface networkInterface = interfaces.nextElement();
                // 排除无线网卡（通常名称包含 "wlan" 或 "wi-fi"）
                if (networkInterface.isUp() && !networkInterface.isLoopback() && !networkInterface.isVirtual()
                        && !networkInterface.isPointToPoint() && !networkInterface.getName().toLowerCase().contains("wlan") && !networkInterface.getName().toLowerCase().contains("wi-fi")) {
                    Enumeration<InetAddress> addresses = networkInterface.getInetAddresses();
                    System.out.println("I have got addresses: " + addresses.hasMoreElements());
                    while (addresses.hasMoreElements()) {
                        InetAddress address = addresses.nextElement();
                        System.out.println("the address is: " + address.getHostAddress());
                        if (address instanceof java.net.Inet4Address && !address.isLoopbackAddress() && !address.isMulticastAddress()) {
                            ipAddress = address.getHostAddress();
                            return address.getHostAddress();

                        }
                    }
                }
            }
        } catch (SocketException e) {
            e.printStackTrace();
        }
        ipAddress = "127.0.0.1"; // 如果没有找到合适的IP地址，返回默认值
        return "127.0.0.1";
    }

    public static file setFile(String newFileName, BasicFileAttributes attr, String folderName, Path p, Float[] polyRes) throws IOException {
        file f = new file();
        f.setFileName(folderName + "_" + newFileName);
        f.setId(encodeFileName(folderName + newFileName));
        f.setFileDate(attr.creationTime().toString());
        f.setFileType(Files.probeContentType(p));
        f.setFileSize(convertSizeToMB(attr.size()));
        f.setPath(webBasePath + "/" + folderName + "/" + newFileName);
        f.setLngmin(polyRes[0]);
        f.setLatmin(polyRes[1]);
        f.setLngmax(polyRes[2]);
        f.setLatmax(polyRes[3]);
        return f;
    }

    public static boolean isURLAccessible(String urlString) {
        try {
            URL url = new URL(urlString);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("HEAD");
            connection.setConnectTimeout(50000); // 5 seconds timeout
            connection.setReadTimeout(50000); // 5 seconds timeout
            int responseCode = connection.getResponseCode();
            return (responseCode >= 200 && responseCode < 400); // 2xx and 3xx codes indicate success
        } catch (IOException e) {
            return false;
        }
    }

    public static Float[] retPoly(int divideX, int divideY, List<Float[]> parsedPolyList, int targetRow, int targetColumn) {
        // 获取左上角和右下角坐标
        Float[] leftTop = parsedPolyList.get(0);
        Float[] rightBottom = parsedPolyList.get(1);

        // 计算整个区域的宽度和高度
        float width = rightBottom[0] - leftTop[0];
        float height = leftTop[1] - rightBottom[1];

        // 计算每个分块的宽度和高度
        float blockWidth = width / divideX;
        float blockHeight = height / divideY;

        // 计算指定分块的左上角和右下角坐标
        float blockLeftTopX = leftTop[0] + targetColumn * blockWidth;
        float blockLeftTopY = leftTop[1] - targetRow * blockHeight;
        float blockRightBottomX = blockLeftTopX + blockWidth;
        float blockRightBottomY = blockLeftTopY - blockHeight;

        return new Float[]{blockLeftTopX, blockLeftTopY, blockRightBottomX, blockRightBottomY};
    }

    public static int[] parseRowAndColumn(String fileName) {
        // 正则表达式匹配文件名中的行和列信息
        Pattern pattern = Pattern.compile("_(\\d{2})_(\\d{2})$");
        Matcher matcher = pattern.matcher(fileName);

        if (matcher.find()) {
            int row = Integer.parseInt(matcher.group(1));
            int column = Integer.parseInt(matcher.group(2));
            return new int[]{row, column};
        } else {
            throw new IllegalArgumentException("Invalid file name format");
        }
    }


    public static fileSet setFileSet(String newFileName, BasicFileAttributes attr, Path p) throws IOException {
        fileSet f = new fileSet();
        f.setFileName(newFileName);
        f.setFileDate(attr.creationTime().toString());
        f.setFileType(Files.probeContentType(p));
        f.setFileSize(convertSizeToMB(attr.size()));
        String subName = newFileName.substring(0, newFileName.lastIndexOf('.'));
        ArrayList<String> fileNameList = new ArrayList<>();
        for (String m : mark) {
            fileNameList.add(encodeFileName(subName + m));
        }
        f.setId(fileNameList);
        ArrayList<String> pathList = new ArrayList<>();
        for (String m : mark) {
            pathList.add(webBasePath + "/" + stage2folder + "/" + stage2FolderResult + "/" + subName + m + ".png");
        }
        f.setPath(pathList);
        Path poly = Paths.get(basePath, stage2folder, stage2FolderInput, subName + ".txt");
        List<String> polyList = Files.readAllLines(poly);
        ArrayList<Float[]> parsedPolyList = new ArrayList<>();
        for (String item : polyList) {
            String[] parts = item.split(" ");
            float num1 = Float.parseFloat(parts[0]);
            float num2 = Float.parseFloat(parts[1]);
            parsedPolyList.add(new Float[]{num1, num2});
        }
        f.setLngmin(parsedPolyList.get(0)[0]);
        f.setLatmin(parsedPolyList.get(0)[1]);
        f.setLngmax(parsedPolyList.get(1)[0]);
        f.setLatmax(parsedPolyList.get(1)[1]);
        return f;
    }

    public static void movePngFiles(String basePath, String[] paths) {
        String targetFolder = basePath + File.separator + paths[0] + "_sim";
        File targetDir = new File(targetFolder);

        if (!targetDir.exists()) {
            targetDir.mkdirs();
        }

        for (String path : paths) {
            String fullPath = basePath + File.separator + path;
            File dir = new File(fullPath);

            // 检查路径是否存在
            if (!dir.exists() || !dir.isDirectory()) {
                System.out.println("路径不存在或不是目录: " + fullPath);
                continue;
            }

            // 遍历路径中的所有文件
            File[] files = dir.listFiles((d, name) -> name.endsWith(".png"));
            if (files != null) {
                for (File file : files) {
                    try {
                        Path sourcePath = file.toPath();
                        Path targetPath = Paths.get(targetFolder, file.getName());
                        Files.move(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
                        System.out.println("移动文件: " + sourcePath + " 到 " + targetPath);
                    } catch (IOException e) {
                        System.err.println("移动文件时发生错误: " + e.getMessage());
                    }
                }
            }
        }
    }

    public static List<String> processFileName(String inputFileName) {
        // 提取文件名中的前两个数字
        String[] parts = inputFileName.split("_");
        if (parts.length < 3) {
            System.err.println("Invalid file name format");
            return null;
        }

        try {
            int firstNumber = Integer.parseInt(parts[1]);
            int secondNumber = Integer.parseInt(parts[2]);

            // 将这两个数字分别除以 512
            int firstResult = firstNumber / 512;
            int secondResult = secondNumber / 512;

            // 生成新的文件名
            String fileNameA = String.format("A_%02d_%02d.txt", secondResult, firstResult);
            String fileNameD = String.format("D_%02d_%02d.txt", secondResult, firstResult);
            return List.of(fileNameA, fileNameD);

        } catch (NumberFormatException e) {
            System.err.println("Invalid number format in file name");
        }
        return null;
    }

    public static void sendCommand(String command) {
        // 目标主机 IP 地址
        String targetHost = "192.168.1.100";

        // 检测是否能够与目标主机通信
        try {
            InetAddress address = InetAddress.getByName(targetHost);
            if (!address.isReachable(500)) { // 超时时间为 5000 毫秒（5 秒）
                System.err.println("无法与目标主机通信: " + targetHost);
                return;
            }
        } catch (IOException e) {
            System.err.println("检测目标主机通信失败: " + e);
            return;
        }

        // 源文件路径
        String sourceFilePath = "\\\\" + targetHost + "\\FileRecv_Shared\\cmdstart.txt";
        // 目标文件夹路径
        String targetDirPath = "\\\\" + targetHost + "\\FileRecv_Shared\\command";
        // 数字变量
        num++; // 你可以根据需要更改这个数字

        // 目标文件路径
        String targetFilePath = targetDirPath + "\\cmd" + num + ".txt";

        // 创建目标文件夹（如果不存在）
        File targetDir = new File(targetDirPath);
        if (!targetDir.exists()) {
            targetDir.mkdirs();
        }

        // 复制文件并重命名
        try {
            Path sourcePath = Paths.get(sourceFilePath);
            Path targetPath = Paths.get(targetFilePath);
            Files.copy(sourcePath, targetPath);
            System.out.println("文件复制并重命名成功: " + targetFilePath);
        } catch (IOException e) {
            System.err.println("文件复制失败: " + e);
        }
    }

    public static void sendCommandTr(String command) {
        // 目标主机 IP 地址
        String targetHost = "192.168.1.100";

        // 检测是否能够与目标主机通信
        try {
            InetAddress address = InetAddress.getByName(targetHost);
            if (!address.isReachable(500)) { // 超时时间为 5000 毫秒（5 秒）
                System.err.println("无法与目标主机通信: " + targetHost);
                return;
            }
        } catch (IOException e) {
            System.err.println("检测目标主机通信失败: " + e);
            return;
        }
        // 源文件路径
        String sourceFilePath = "\\\\" + targetHost + "\\FileRecv_Shared\\cmdstop.txt";
        // 目标文件夹路径
        String targetDirPath = "\\\\" + targetHost + "\\FileRecv_Shared\\command";
        // 数字变量
        num++; // 你可以根据需要更改这个数字

        // 目标文件路径
        String targetFilePath = targetDirPath + "\\cmd" + num + ".txt";

        // 创建目标文件夹（如果不存在）
        File targetDir = new File(targetDirPath);
        if (!targetDir.exists()) {
            targetDir.mkdirs();
        }

        // 复制文件并重命名
        try {
            Path sourcePath = Paths.get(sourceFilePath);
            Path targetPath = Paths.get(targetFilePath);
            Files.copy(sourcePath, targetPath);
            System.out.println("文件复制并重命名成功: " + targetFilePath);
        } catch (IOException e) {
            System.err.println("文件复制失败: " + e);
        }
    }
}
