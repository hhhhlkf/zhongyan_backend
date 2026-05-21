package com.zhongyan.uav.telemetry.infrastructure;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "bms.telemetry")
public class TelemetryProperties {
    private final Udp udp = new Udp();
    private final Kafka kafka = new Kafka();

    public Udp getUdp() {
        return udp;
    }

    public Kafka getKafka() {
        return kafka;
    }

    public static class Udp {
        private boolean enabled;
        private int port = 18088;
        private int bufferBytes = 8192;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public int getPort() {
            return port;
        }

        public void setPort(int port) {
            this.port = port;
        }

        public int getBufferBytes() {
            return bufferBytes;
        }

        public void setBufferBytes(int bufferBytes) {
            this.bufferBytes = bufferBytes;
        }
    }

    public static class Kafka {
        private boolean enabled;
        private String topic = "uav-telemetry";

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getTopic() {
            return topic;
        }

        public void setTopic(String topic) {
            this.topic = topic;
        }
    }
}
