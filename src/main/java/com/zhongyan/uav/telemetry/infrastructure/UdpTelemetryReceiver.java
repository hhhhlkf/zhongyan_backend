package com.zhongyan.uav.telemetry.infrastructure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhongyan.uav.telemetry.application.UavTelemetryIngestService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Component
@ConditionalOnProperty(prefix = "bms.telemetry.udp", name = "enabled", havingValue = "true")
public class UdpTelemetryReceiver {
    private final TelemetryProperties properties;
    private final UavTelemetryIngestService ingestService;
    private final TelemetryMessageParser messageParser;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private volatile boolean running;
    private DatagramSocket socket;

    public UdpTelemetryReceiver(TelemetryProperties properties,
                                UavTelemetryIngestService ingestService,
                                ObjectMapper objectMapper) {
        this.properties = properties;
        this.ingestService = ingestService;
        this.messageParser = new TelemetryMessageParser(objectMapper);
    }

    @PostConstruct
    public void start() throws SocketException {
        socket = new DatagramSocket(properties.getUdp().getPort());
        running = true;
        executorService.submit(this::receiveLoop);
    }

    @PreDestroy
    public void stop() {
        running = false;
        if (socket != null) {
            socket.close();
        }
        executorService.shutdownNow();
    }

    private void receiveLoop() {
        int bufferBytes = Math.max(properties.getUdp().getBufferBytes(), 1024);
        while (running) {
            byte[] buffer = new byte[bufferBytes];
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
            try {
                socket.receive(packet);
                String message = new String(packet.getData(), packet.getOffset(), packet.getLength(),
                        StandardCharsets.UTF_8);
                ingestService.ingest(messageParser.parse(null, message));
            } catch (SocketException ex) {
                if (running) {
                    throw new IllegalStateException("UDP telemetry socket failed", ex);
                }
            } catch (IOException | RuntimeException ex) {
                // Drop malformed UDP packets; durable diagnostics are handled by HTTP/Kafka paths.
            }
        }
    }
}
