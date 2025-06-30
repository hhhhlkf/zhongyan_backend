package com.gosling.bms.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.File;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;

@Service
@Slf4j
public class UdpReceiverService implements Runnable {

    private volatile boolean running = true;
    private Thread thread;
    private DatagramSocket socket;

    private static final String FILE_PATH = "src/main/resources/static/udp_data.json";
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private synchronized void appendDataToJsonFile(String data) {
        File file = new File(FILE_PATH);
        ArrayNode arrayNode;
        try {
            if (file.exists() && file.length() > 0) {
                arrayNode = (ArrayNode) objectMapper.readTree(file);
            } else {
                arrayNode = objectMapper.createArrayNode();
            }
            JsonNode newNode = objectMapper.readTree(data);
            arrayNode.add(newNode);
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(file, arrayNode);
        } catch (IOException e) {
            log.error("写入JSON文件失败", e);
        }
    }

    @PostConstruct
    public void start() {
        // 启动前只保留 udp_data.json 最新三条数据
        try {
            File file = new File(FILE_PATH);
            if (file.exists() && file.length() > 0) {
                ArrayNode arrayNode = (ArrayNode) objectMapper.readTree(file);
                if (arrayNode.size() > 3) {
                    ArrayNode lastThree = objectMapper.createArrayNode();
                    for (int i = arrayNode.size() - 3; i < arrayNode.size(); i++) {
                        lastThree.add(arrayNode.get(i));
                    }
                    objectMapper.writerWithDefaultPrettyPrinter().writeValue(file, lastThree);
                    log.info("已保留 udp_data.json 最新三条数据");
                }
            }
        } catch (IOException e) {
            log.error("处理 udp_data.json 文件失败", e);
        }

        log.info("UDP接收服务启动");
        thread = new Thread(this, "UdpReceiverThread");
        thread.start();

    }

    @PreDestroy
    public void stop() {
        running = false;
        if (socket != null && !socket.isClosed()) {
            socket.close();
        }
    }

    @Override
    public void run() {
        log.info("UdpReceiverService run() started");
        try {
            socket = new DatagramSocket(2010); // 监听9000端口
            byte[] buffer = new byte[4096];
            while (running) {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);
                // 处理接收到的数据
                String data = new String(packet.getData(), 0, packet.getLength());
                log.info("收到UDP数据: {}", data);
                appendDataToJsonFile(data);
                // 可将数据保存为文件或进一步处理
            }
        } catch (Exception e) {
            if (running) {
                log.error("UDP接收异常", e);
            }
        } finally {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        }
    }
}