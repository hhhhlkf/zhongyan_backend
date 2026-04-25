package com.gosling.bms.service.impl;

import com.gosling.bms.utils.UdpDataFileStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class UdpReceiverService implements Runnable {

    private volatile boolean running = true;
    private Thread thread;
    private DatagramSocket socket;
    private ScheduledExecutorService cleanupExecutor;

    @Value("${udp-data.max-records:3600}")
    private int maxRecords;

    @Value("${udp-data.trim-interval-ms:60000}")
    private long trimIntervalMs;

    @Value("${udp-data.listen-port:6308}")
    private int listenPort;

    @Value("${udp-data.file-path:src/main/resources/static/udp_data.json}")
    private String udpDataFilePath;

    private Path getUdpDataPath() {
        return Paths.get(udpDataFilePath);
    }

    private synchronized void appendDataToJsonFile(String data) {
        try {
            UdpDataFileStore.append(getUdpDataPath(), data, maxRecords);
        } catch (IOException e) {
            log.error("Failed to write udp_data.json", e);
        }
    }

    @PostConstruct
    public void start() {
        try {
            UdpDataFileStore.trimToLatest(getUdpDataPath(), maxRecords);
        } catch (IOException e) {
            log.error("Failed to trim udp_data.json on startup", e);
        }

        startCleanupTask();
        log.info("UDP receiver service started, port={}, file={}", listenPort, getUdpDataPath());
        thread = new Thread(this, "UdpReceiverThread");
        thread.start();
    }

    private void startCleanupTask() {
        cleanupExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread cleanupThread = new Thread(r, "UdpDataCleanupThread");
            cleanupThread.setDaemon(true);
            return cleanupThread;
        });
        cleanupExecutor.scheduleWithFixedDelay(() -> {
            try {
                UdpDataFileStore.trimToLatest(getUdpDataPath(), maxRecords);
            } catch (IOException e) {
                log.error("Failed to trim udp_data.json", e);
            }
        }, trimIntervalMs, trimIntervalMs, TimeUnit.MILLISECONDS);
    }

    @PreDestroy
    public void stop() {
        running = false;
        if (socket != null && !socket.isClosed()) {
            socket.close();
        }
        if (cleanupExecutor != null) {
            cleanupExecutor.shutdownNow();
        }
    }

    @Override
    public void run() {
        log.info("UdpReceiverService run() started");
        try {
            socket = new DatagramSocket(listenPort);
            byte[] buffer = new byte[4096];
            while (running) {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);
                String data = new String(packet.getData(), 0, packet.getLength());
                log.info("Received UDP data: {}", data);
                appendDataToJsonFile(data);
            }
        } catch (Exception e) {
            if (running) {
                log.error("UDP receiver failed", e);
            }
        } finally {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        }
    }
}
