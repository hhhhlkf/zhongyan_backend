package com.zhongyan.uav.realtime.api;

import com.zhongyan.uav.realtime.application.RealtimePushService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * 实时推送 REST 入口。
 * <p>
 * 当前实现提供 SSE 订阅端点，前端通过这些端点接收任务、遥测和 Agent 会话事件；
 * 控制器只负责订阅路由映射，实际连接管理和事件分发由 {@link RealtimePushService} 完成。
 */
@RestController
@RequestMapping("/realtime")
public class RealtimeController {
    private final RealtimePushService realtimePushService;

    public RealtimeController(RealtimePushService realtimePushService) {
        this.realtimePushService = realtimePushService;
    }

    @GetMapping(value = "/missions/{missionId}/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter missionEvents(@PathVariable String missionId) {
        return realtimePushService.subscribe("task-events", missionId);
    }

    @GetMapping(value = "/tasks/{taskId}/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter taskEvents(@PathVariable String taskId) {
        return realtimePushService.subscribe("task-events", taskId);
    }

    @GetMapping(value = "/uavs/{uavId}/telemetry", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter uavTelemetry(@PathVariable String uavId) {
        return realtimePushService.subscribe("uav-telemetry", uavId);
    }

    @GetMapping(value = "/agent/sessions/{sessionId}/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter agentSessionEvents(@PathVariable String sessionId) {
        return realtimePushService.subscribe("agent-events", sessionId);
    }
}
