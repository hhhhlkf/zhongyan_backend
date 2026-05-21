package com.zhongyan.uav.task.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhongyan.uav.asset.domain.Asset;
import com.zhongyan.uav.asset.domain.AssetRepository;
import com.zhongyan.uav.asset.domain.AssetRole;
import com.zhongyan.uav.asset.domain.AssetType;
import com.zhongyan.uav.task.application.TaskApplicationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@WithMockUser(username = "api-tester", authorities = {"WRITE", "TASK_APPROVE"})
class ApiControllerFlowTests {
    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private AssetRepository assetRepository;

    @Autowired
    private TaskApplicationService taskApplicationService;

    /**
     * 使用当前 Spring Web 上下文手动构建 MockMvc。
     */
    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();
    }

    /**
     * 验证 Controller 可以通过 /v2 新接口跑通 Mission -> Task -> Command/Event -> Asset 查询链路。
     */
    @Test
    void runsMissionTaskCommandApprovalEventAssetApiFlow() throws Exception {
        String missionId = createMission();
        String taskId = createTask(missionId);

        mockMvc.perform(post("/v2/tasks/{taskId}/submit", taskId)
                        .contextPath("/v2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("submittedBy", "operator-1"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.status").value("QUEUED"));

        String commandId = createHighRiskCommand(taskId);

        mockMvc.perform(get("/v2/approvals/pending").contextPath("/v2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[*].commandId", hasItem(commandId)));

        mockMvc.perform(post("/v2/approvals/{commandId}/approve", commandId)
                        .contextPath("/v2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("approver", "admin-1"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.status").value("PENDING_DISPATCH"));

        assetRepository.save(Asset.created("asset-api-1", missionId, taskId, AssetType.IMAGE,
                        AssetRole.OUTPUT, "rgb-api.png", "missions/mock/rgb-api.png",
                        "image/png", 1024, "sha256:api", Map.of(), "operator-1", Instant.now())
                .markAvailable(Instant.now()));

        mockMvc.perform(get("/v2/tasks/{taskId}/events", taskId).contextPath("/v2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.length()").value(4));

        mockMvc.perform(get("/v2/tasks/{taskId}/assets", taskId).contextPath("/v2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].assetId").value("asset-api-1"));

        mockMvc.perform(get("/v2/missions/{missionId}/tasks", missionId).contextPath("/v2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].taskId").value(taskId));
    }

    @Test
    void listsTaskAttemptsAfterFailureRetryAndCancel() throws Exception {
        String missionId = createMission();
        String taskId = createTask(missionId);

        mockMvc.perform(post("/v2/tasks/{taskId}/submit", taskId)
                        .contextPath("/v2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("submittedBy", "operator-2"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("QUEUED"));

        taskApplicationService.startTaskAttempt(taskId, "mock-node-a");
        taskApplicationService.failTask(taskId, "MOCK_FAILED", "mock failure", "logs/api-attempt-1.log");

        mockMvc.perform(post("/v2/tasks/{taskId}/retry", taskId)
                        .contextPath("/v2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("requestedBy", "operator-2"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.status").value("QUEUED"));

        taskApplicationService.startTaskAttempt(taskId, "mock-node-b");

        mockMvc.perform(post("/v2/tasks/{taskId}/cancel", taskId)
                        .contextPath("/v2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("cancelledBy", "operator-2"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.status").value("CANCELLED"));

        mockMvc.perform(get("/v2/tasks/{taskId}/attempts", taskId).contextPath("/v2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].attemptNo").value(1))
                .andExpect(jsonPath("$.data[0].result").value("FAILED"))
                .andExpect(jsonPath("$.data[1].attemptNo").value(2))
                .andExpect(jsonPath("$.data[1].result").value("CANCELLED"));
    }

    /**
     * 创建测试 Mission，并返回 Mission 编号。
     */
    private String createMission() throws Exception {
        Map<String, Object> request = Map.of(
                "name", "API 应急任务",
                "scenarioType", "emergency",
                "priority", 10,
                "createdBy", "operator-1",
                "description", "controller flow");

        String response = mockMvc.perform(post("/v2/missions")
                        .contextPath("/v2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.status").value("DRAFT"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        return readText(response, "missionId");
    }

    /**
     * 在指定 Mission 下创建测试 Task，并返回 Task 编号。
     */
    private String createTask(String missionId) throws Exception {
        Map<String, Object> request = Map.of(
                "taskType", "CAPTURE",
                "priority", 10,
                "deviceId", "device-rgb-1",
                "configSnapshot", Map.of("cameraType", "rgb"),
                "inputAssetIds", List.of(),
                "createdBy", "operator-1");

        String response = mockMvc.perform(post("/v2/missions/{missionId}/tasks", missionId)
                        .contextPath("/v2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.status").value("DRAFT"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        return readText(response, "taskId");
    }

    /**
     * 创建高风险命令，并返回命令编号。
     */
    private String createHighRiskCommand(String taskId) throws Exception {
        Map<String, Object> request = Map.of(
                "commandType", "START_CAPTURE",
                "payload", Map.of("durationSeconds", 60),
                "idempotencyKey", "api-idem-start-capture-1",
                "requestedBy", "operator-1",
                "deviceId", "device-rgb-1",
                "reason", "start capture");

        String response = mockMvc.perform(post("/v2/tasks/{taskId}/commands", taskId)
                        .contextPath("/v2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.status").value("PENDING_APPROVAL"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        return readText(response, "commandId");
    }

    /**
     * 从统一响应 JSON 的 data 节点读取文本字段。
     */
    private String readText(String response, String fieldName) throws Exception {
        JsonNode root = objectMapper.readTree(response);
        String value = root.path("data").path(fieldName).asText();
        assertThat(value).isNotBlank();
        return value;
    }
}
