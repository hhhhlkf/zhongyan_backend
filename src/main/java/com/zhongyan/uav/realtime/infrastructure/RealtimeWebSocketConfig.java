package com.zhongyan.uav.realtime.infrastructure;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

@Configuration
@EnableWebSocket
public class RealtimeWebSocketConfig implements WebSocketConfigurer {
    private final WebSocketRealtimePushService webSocketRealtimePushService;

    public RealtimeWebSocketConfig(WebSocketRealtimePushService webSocketRealtimePushService) {
        this.webSocketRealtimePushService = webSocketRealtimePushService;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(webSocketRealtimePushService,
                        "/ws/realtime/agent/sessions/*/events",
                        "/ws/realtime/tasks/*/events",
                        "/ws/realtime/missions/*/events",
                        "/ws/realtime/uavs/*/telemetry",
                        "/ws/realtime/topics/*/*")
                .addInterceptors(new RealtimeSubscriptionInterceptor())
                .setAllowedOriginPatterns("*");
    }

    private static final class RealtimeSubscriptionInterceptor implements HandshakeInterceptor {
        @Override
        public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                       WebSocketHandler wsHandler, Map<String, Object> attributes) {
            String path = request.getURI().getPath();
            String[] parts = path.split("/");
            putSubscription(parts, attributes);
            return true;
        }

        @Override
        public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Exception exception) {
        }

        private static void putSubscription(String[] parts, Map<String, Object> attributes) {
            String path = String.join("/", parts);
            if (path.contains("ws/realtime/agent/sessions/")) {
                attributes.put("topic", "agent-events");
                attributes.put("key", partBefore(parts, "events"));
            } else if (path.contains("ws/realtime/uavs/")) {
                attributes.put("topic", "uav-telemetry");
                attributes.put("key", partBefore(parts, "telemetry"));
            } else if (path.contains("ws/realtime/tasks/")) {
                attributes.put("topic", "task-events");
                attributes.put("key", partBefore(parts, "events"));
            } else if (path.contains("ws/realtime/missions/")) {
                attributes.put("topic", "task-events");
                attributes.put("key", partBefore(parts, "events"));
            } else if (path.contains("ws/realtime/topics/") && parts.length >= 2) {
                attributes.put("topic", parts[parts.length - 2]);
                attributes.put("key", parts[parts.length - 1]);
            } else {
                attributes.put("topic", "*");
                attributes.put("key", "*");
            }
        }

        private static String partBefore(String[] parts, String marker) {
            for (int index = 1; index < parts.length; index++) {
                if (marker.equals(parts[index]) && index > 0) {
                    return parts[index - 1];
                }
            }
            return "*";
        }
    }
}
