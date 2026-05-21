package com.zhongyan.uav.agent.infrastructure.tool;

import com.zhongyan.uav.agent.port.AgentToolContext;
import com.zhongyan.uav.agent.port.AgentToolResult;
import com.zhongyan.uav.asset.domain.Asset;
import com.zhongyan.uav.asset.domain.AssetRole;
import com.zhongyan.uav.asset.domain.AssetType;
import com.zhongyan.uav.asset.infrastructure.mock.InMemoryAssetRepository;
import com.zhongyan.uav.mission.domain.Mission;
import com.zhongyan.uav.mission.infrastructure.mock.InMemoryMissionRepository;
import com.zhongyan.uav.task.application.TaskApplicationService;
import com.zhongyan.uav.task.domain.Task;
import com.zhongyan.uav.task.domain.TaskAttempt;
import com.zhongyan.uav.task.domain.TaskEvent;
import com.zhongyan.uav.task.domain.TaskEventType;
import com.zhongyan.uav.task.domain.TaskType;
import com.zhongyan.uav.task.infrastructure.mock.InMemoryTaskAttemptRepository;
import com.zhongyan.uav.task.infrastructure.mock.InMemoryTaskCommandRepository;
import com.zhongyan.uav.task.infrastructure.mock.InMemoryTaskEventRepository;
import com.zhongyan.uav.task.infrastructure.mock.InMemoryTaskRepository;
import com.zhongyan.uav.telemetry.application.UavTelemetryQueryService;
import com.zhongyan.uav.telemetry.domain.UavTelemetry;
import com.zhongyan.uav.telemetry.infrastructure.mock.InMemoryUavTelemetryRepository;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that Agent tools execute real application or repository reads instead of placeholders.
 */
class AgentToolImplementationTests {
    private static final Instant NOW = Instant.parse("2026-05-19T08:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);
    private static final AgentToolContext CONTEXT = new AgentToolContext("session-1", "user-1",
            "agent:session-1", Set.of("AGENT_CHAT", "AGENT_TASK_PLAN"), Map.of());

    @Test
    void queryToolsReturnMissionTaskAssetAndTelemetryData() {
        InMemoryMissionRepository missions = new InMemoryMissionRepository();
        InMemoryTaskRepository tasks = new InMemoryTaskRepository();
        InMemoryTaskEventRepository taskEvents = new InMemoryTaskEventRepository();
        InMemoryTaskAttemptRepository attempts = new InMemoryTaskAttemptRepository();
        InMemoryTaskCommandRepository commands = new InMemoryTaskCommandRepository();
        InMemoryAssetRepository assets = new InMemoryAssetRepository();
        InMemoryUavTelemetryRepository telemetry = new InMemoryUavTelemetryRepository();

        missions.save(Mission.draft("mission-1", "Flood survey", "DISASTER", null, 1,
                "tester", NOW, "field mission"));
        tasks.save(Task.draft("task-1", "mission-1", TaskType.CAPTURE, 1, "uav-1",
                null, Map.of(), List.of(), "tester", NOW));
        taskEvents.save(TaskEvent.statusChanged("event-1", "task-1", TaskEventType.CREATED,
                null, null, NOW.plusSeconds(1)));
        assets.save(Asset.created("asset-1", "mission-1", "task-1", AssetType.IMAGE,
                AssetRole.OUTPUT, "flood image", "objects/flood.jpg", "image/jpeg", 100,
                "sha256", Map.of("area", "north"), "tester", NOW).markAvailable(NOW));
        telemetry.save(UavTelemetry.record("telemetry-1", "uav-1", "mission-1", "task-1",
                NOW.plusSeconds(2), 30.1, 104.1, 120.0, 90.0, 12.0, Map.of()));

        AgentToolResult missionResult = new MissionQueryTool(missions).execute(CONTEXT, Map.of("missionId", "mission-1"));
        AgentToolResult taskResult = new TaskQueryTool(tasks, taskEvents, attempts, commands)
                .execute(CONTEXT, Map.of("taskId", "task-1"));
        AgentToolResult assetResult = new AssetSearchTool(assets)
                .execute(CONTEXT, Map.of("missionId", "mission-1", "keyword", "flood"));
        AgentToolResult statsResult = new AssetStatsTool(assets)
                .execute(CONTEXT, Map.of("missionId", "mission-1"));
        AgentToolResult telemetryResult = new TelemetryQueryTool(new UavTelemetryQueryService(telemetry, CLOCK))
                .execute(CONTEXT, Map.of("uavId", "uav-1", "missionId", "mission-1"));

        assertThat(list(missionResult, "missions")).extracting("missionId").containsExactly("mission-1");
        assertThat(list(taskResult, "tasks")).extracting("taskId").containsExactly("task-1");
        assertThat(list(assetResult, "assets")).extracting("assetId").containsExactly("asset-1");
        assertThat(statsResult.output()).containsEntry("total", 1);
        assertThat(list(telemetryResult, "telemetry")).extracting("uavId").containsExactly("uav-1");
    }

    @Test
    void taskCreateToolCreatesDraftTaskThroughApplicationService() {
        InMemoryMissionRepository missions = new InMemoryMissionRepository();
        InMemoryTaskRepository tasks = new InMemoryTaskRepository();
        InMemoryTaskEventRepository taskEvents = new InMemoryTaskEventRepository();
        InMemoryTaskAttemptRepository attempts = new InMemoryTaskAttemptRepository();
        missions.save(Mission.draft("mission-1", "Mission", "TEST", null, 1, "tester", NOW, null));

        TaskApplicationService taskService = new TaskApplicationService(tasks, missions, taskEvents, attempts, CLOCK);
        AgentToolResult result = new TaskCreateTool(taskService).execute(CONTEXT, Map.of(
                "missionId", "mission-1",
                "taskType", "capture",
                "deviceId", "uav-1",
                "parameters", Map.of("camera", "rgb")));

        assertThat(result.output()).containsEntry("status", "DRAFT");
        assertThat(tasks.findByMissionId("mission-1")).hasSize(1);
        assertThat(taskEvents.findByTaskId(tasks.findByMissionId("mission-1").get(0).taskId()))
                .extracting(TaskEvent::eventType)
                .containsExactly(TaskEventType.CREATED);
    }

    @Test
    void diagnosisToolReturnsAttemptAndEventAuditData() {
        InMemoryTaskAttemptRepository attempts = new InMemoryTaskAttemptRepository();
        InMemoryTaskEventRepository events = new InMemoryTaskEventRepository();
        attempts.save(TaskAttempt.started("attempt-1", "task-1", 1, "node-1", NOW)
                .fail(NOW.plusSeconds(5), "DEVICE_TIMEOUT", "camera timeout", "logs/attempt-1.txt"));
        events.save(TaskEvent.statusChanged("event-1", "task-1", TaskEventType.FAILED, null, null, NOW.plusSeconds(6)));

        AgentToolResult result = new DiagnosisReadTaskLogTool(attempts, events)
                .execute(CONTEXT, Map.of("taskId", "task-1"));

        assertThat(list(result, "attempts")).extracting("rawLogObjectKey").containsExactly("logs/attempt-1.txt");
        assertThat(list(result, "events")).extracting("eventType").containsExactly("FAILED");
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> list(AgentToolResult result, String key) {
        return (List<Map<String, Object>>) result.output().get(key);
    }
}
