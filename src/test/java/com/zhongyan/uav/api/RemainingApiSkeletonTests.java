package com.zhongyan.uav.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhongyan.uav.asset.port.AssetObject;
import com.zhongyan.uav.asset.port.AssetStoragePort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.List;
import java.util.Map;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@WithMockUser(username = "skeleton-tester", authorities = {"WRITE", "TASK_APPROVE", "AGENT_CHAT", "AGENT_TOOL_APPROVE"})
class RemainingApiSkeletonTests {
    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private AssetStoragePort assetStoragePort;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();
    }

    @Test
    void exposesConfigurationApiWithMockRepositories() throws Exception {
        String deviceId = createDevice();
        mockMvc.perform(get("/v2/config/devices/{deviceId}", deviceId).contextPath("/v2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.deviceId").value(deviceId));
        mockMvc.perform(get("/v2/config/devices").contextPath("/v2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].deviceId").exists());
        mockMvc.perform(post("/v2/config/devices/{deviceId}/validate", deviceId).contextPath("/v2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.configType").value("DEVICE"))
                .andExpect(jsonPath("$.data.status").value("PASSED"));
        mockMvc.perform(post("/v2/config/devices/{deviceId}/disable", deviceId).contextPath("/v2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.action").value("DISABLE"));

        String cameraConfigId = createCamera();
        mockMvc.perform(post("/v2/config/cameras/{cameraConfigId}/versions", cameraConfigId)
                        .contextPath("/v2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("version", 2, "cameraType", "HSI"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.version").value(2));
        mockMvc.perform(post("/v2/config/cameras/{cameraConfigId}/activate", cameraConfigId).contextPath("/v2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.action").value("ACTIVATE"));
        mockMvc.perform(get("/v2/config/cameras/{cameraConfigId}", cameraConfigId).contextPath("/v2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));
        mockMvc.perform(post("/v2/config/cameras/{cameraConfigId}/validate", cameraConfigId).contextPath("/v2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.configType").value("CAMERA"));

        String modelConfigId = createModel();
        mockMvc.perform(post("/v2/config/models/{modelConfigId}/versions", modelConfigId)
                        .contextPath("/v2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("version", 2, "modelType", "SEGMENTATION", "runtimeType", "ONNX"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.version").value(2));
        mockMvc.perform(post("/v2/config/models/{modelConfigId}/artifacts", modelConfigId)
                        .contextPath("/v2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("artifactType", "WEIGHTS", "objectKey", "models/mock.onnx"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.modelConfigId").value(modelConfigId));
        mockMvc.perform(post("/v2/config/models/{modelConfigId}/activate", modelConfigId).contextPath("/v2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.action").value("ACTIVATE"));
        mockMvc.perform(post("/v2/config/models/{modelConfigId}/validate", modelConfigId).contextPath("/v2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.configType").value("MODEL"));

        String transferConfigId = createTransfer();
        mockMvc.perform(post("/v2/config/transfers/{transferConfigId}/versions", transferConfigId)
                        .contextPath("/v2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("version", 2, "transferType", "MINIO"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.version").value(2));
        mockMvc.perform(post("/v2/config/transfers/{transferConfigId}/activate", transferConfigId).contextPath("/v2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.action").value("ACTIVATE"));
        String validationResponse = mockMvc.perform(post("/v2/config/transfers/{transferConfigId}/validate", transferConfigId)
                        .contextPath("/v2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.configType").value("TRANSFER"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        String validationId = readText(validationResponse, "validationId");

        mockMvc.perform(get("/v2/config/validations/{validationId}", validationId).contextPath("/v2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.validationId").value(validationId));
        mockMvc.perform(get("/v2/config/validations")
                        .contextPath("/v2")
                        .param("configType", "TRANSFER")
                        .param("configId", transferConfigId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].configId").value(transferConfigId));
    }

    @Test
    void exposesAssetApiSkeletons() throws Exception {
        String objectKey = "missions/mission-api-assets/rgb.png";
        byte[] image = pngBytes();
        assetStoragePort.put(new AssetObject(objectKey, new ByteArrayInputStream(image), image.length,
                "image/png", Map.of("fixture", "true")));

        String createResponse = mockMvc.perform(post("/v2/assets")
                        .contextPath("/v2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("missionId", "mission-api-assets",
                                "assetType", "IMAGE",
                                "name", "rgb.png",
                                "objectKey", objectKey,
                                "contentType", "image/png",
                                "sizeBytes", image.length,
                                "createdBy", "api-tester",
                                "metadata", Map.of("latitude", 30.1, "longitude", 104.1)))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.assetType").value("IMAGE"))
                .andExpect(jsonPath("$.data.status").value("AVAILABLE"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        String assetId = readText(createResponse, "assetId");

        mockMvc.perform(get("/v2/assets").contextPath("/v2"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/v2/assets/{assetId}", assetId).contextPath("/v2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.assetId").value(assetId));
        mockMvc.perform(post("/v2/assets/{assetId}/preview", assetId)
                        .contextPath("/v2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("requestedBy", "api-tester",
                                "parameters", Map.of("longEdge", 64)))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.action").value("GENERATE_PREVIEW"));
        mockMvc.perform(post("/v2/assets/{assetId}/geo-boundary", assetId)
                        .contextPath("/v2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("requestedBy", "api-tester",
                                "parameters", Map.of("latitude", 30.1, "longitude", 104.1)))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.action").value("CALCULATE_GEO_BOUNDARY"));
        mockMvc.perform(post("/v2/assets/{assetId}/publish-layer", assetId)
                        .contextPath("/v2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("requestedBy", "admin-1",
                                "parameters", Map.of("approved", true, "layerId", "asset-api-layer")))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.action").value("PUBLISH_LAYER"))
                .andExpect(jsonPath("$.data.status").value("ACCEPTED"));
        mockMvc.perform(get("/v2/assets/{assetId}/layers", assetId).contextPath("/v2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].layerId").value("asset-api-layer"));
        mockMvc.perform(delete("/v2/assets/{assetId}", assetId).contextPath("/v2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PENDING_APPROVAL"));
        mockMvc.perform(get("/v2/assets/stats").contextPath("/v2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalCount").exists());
    }

    @Test
    void exposesTelemetryApiSkeletons() throws Exception {
        mockMvc.perform(post("/v2/uavs/uav-1/telemetry")
                        .contextPath("/v2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("missionId", "mission-telemetry-api",
                                "latitude", 30.1,
                                "longitude", 104.1,
                                "altitudeMeters", 120.0,
                                "reportedAt", "2026-04-29T00:10:00Z"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.uavId").value("uav-1"))
                .andExpect(jsonPath("$.data.missionId").value("mission-telemetry-api"))
                .andExpect(jsonPath("$.data.status").value("RECORDED"));

        mockMvc.perform(get("/v2/uavs/uav-1/telemetry/latest").contextPath("/v2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.uavId").value("uav-1"))
                .andExpect(jsonPath("$.data.latitude").value(30.1));
        mockMvc.perform(get("/v2/uavs/uav-1/telemetry")
                        .contextPath("/v2")
                        .param("from", "2026-04-29T00:00:00Z")
                        .param("to", "2026-04-29T01:00:00Z"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1));
        mockMvc.perform(get("/v2/uavs/uav-1/track")
                        .contextPath("/v2")
                        .param("missionId", "mission-telemetry-api"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.points.length()").value(1));
    }

    @Test
    void exposesRealtimeApiSkeletons() throws Exception {
        mockMvc.perform(get("/v2/realtime/missions/mission-1/events").contextPath("/v2"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_EVENT_STREAM))
                .andExpect(content().string(containsString("subscribed")));
        mockMvc.perform(get("/v2/realtime/tasks/task-1/events").contextPath("/v2"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_EVENT_STREAM))
                .andExpect(content().string(containsString("subscribed")));
        mockMvc.perform(get("/v2/realtime/uavs/uav-1/telemetry").contextPath("/v2"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_EVENT_STREAM))
                .andExpect(content().string(containsString("subscribed")));
        mockMvc.perform(get("/v2/realtime/agent/sessions/session-1/events").contextPath("/v2"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_EVENT_STREAM))
                .andExpect(content().string(containsString("subscribed")));
    }

    @Test
    void exposesAgentApiSkeletons() throws Exception {
        mockMvc.perform(post("/v2/agent/sessions")
                .contextPath("/v2")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("missionId", "mission-1", "taskId", "task-1", "title", "Agent API"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(400))
            .andExpect(jsonPath("$.message").value("createdBy must not be blank"));

        String sessionResponse = mockMvc.perform(post("/v2/agent/sessions")
                        .contextPath("/v2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("missionId", "mission-1", "taskId", "task-1",
                                "createdBy", "api-tester", "title", "Agent API"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.missionId").value("mission-1"))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        String sessionId = readText(sessionResponse, "sessionId");

        mockMvc.perform(get("/v2/agent/sessions/{sessionId}", sessionId).contextPath("/v2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.sessionId").value(sessionId));
        mockMvc.perform(post("/v2/agent/sessions/{sessionId}/messages", sessionId)
                        .contextPath("/v2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("role", "USER", "content", "summary",
                                "createdBy", "api-tester"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.userMessage.sessionId").value(sessionId))
                .andExpect(jsonPath("$.data.assistantMessage.role").value("ASSISTANT"));
        mockMvc.perform(get("/v2/agent/sessions/{sessionId}/messages", sessionId).contextPath("/v2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2));
        mockMvc.perform(get("/v2/agent/sessions/{sessionId}/events", sessionId).contextPath("/v2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2));
        String missionResponse = mockMvc.perform(post("/v2/missions")
                        .contextPath("/v2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("name", "Agent Tool Mission", "scenarioType", "TEST",
                                "priority", 1, "createdBy", "api-tester"))))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        String missionId = readText(missionResponse, "missionId");
        String taskResponse = mockMvc.perform(post("/v2/missions/{missionId}/tasks", missionId)
                        .contextPath("/v2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("taskType", "CAPTURE", "priority", 1,
                                "deviceId", "device-agent-tool", "createdBy", "api-tester"))))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        String taskId = readText(taskResponse, "taskId");
        String lowRiskResponse = mockMvc.perform(post("/v2/agent/tool-calls")
                        .contextPath("/v2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("sessionId", sessionId, "messageId", "message-1",
                                "userId", "user-1", "toolName", "task.query",
                                "input", Map.of("taskId", taskId),
                                "permissions", List.of("AGENT_CHAT")))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("COMPLETED"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        String lowRiskToolCallId = readText(lowRiskResponse, "toolCallId");
        mockMvc.perform(get("/v2/agent/tool-calls/{toolCallId}", lowRiskToolCallId).contextPath("/v2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.toolCallId").value(lowRiskToolCallId));

        String highRiskResponse = mockMvc.perform(post("/v2/agent/tool-calls")
                        .contextPath("/v2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("sessionId", sessionId, "messageId", "message-1",
                                "userId", "user-1", "toolName", "taskCommand.create",
                                "input", Map.of("taskId", taskId, "commandType", "START_CAPTURE",
                                        "reason", "operator requested"),
                                "permissions", List.of("AGENT_TOOL_APPROVE")))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("WAITING_APPROVAL"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        String highRiskToolCallId = readText(highRiskResponse, "toolCallId");
        mockMvc.perform(post("/v2/agent/tool-calls/{toolCallId}/approve", highRiskToolCallId)
                        .contextPath("/v2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("reviewer", "admin-1"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("COMPLETED"));

        String rejectedResponse = mockMvc.perform(post("/v2/agent/tool-calls")
                        .contextPath("/v2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("sessionId", sessionId, "messageId", "message-1",
                                "userId", "user-1", "toolName", "taskCommand.create",
                                "input", Map.of("taskId", taskId, "commandType", "STOP_CAPTURE",
                                        "reason", "operator requested"),
                                "permissions", List.of("AGENT_TOOL_APPROVE")))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("WAITING_APPROVAL"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        String rejectedToolCallId = readText(rejectedResponse, "toolCallId");
        mockMvc.perform(post("/v2/agent/tool-calls/{toolCallId}/reject", rejectedToolCallId)
                        .contextPath("/v2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("reviewer", "admin-1", "reason", "risk too high"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("REJECTED"));
    }

    @Test
    @WithMockUser(username = "viewer-without-agent-authority")
    void rejectsAgentApiWithoutAgentPermission() throws Exception {
        mockMvc.perform(post("/v2/agent/sessions")
                        .contextPath("/v2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("missionId", "mission-1", "taskId", "task-1",
                                "createdBy", "api-tester", "title", "Agent API"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(403));
    }

    private String createDevice() throws Exception {
        String response = mockMvc.perform(post("/v2/config/devices")
                        .contextPath("/v2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("deviceId", "device-api-1", "deviceName", "RGB Device",
                                "deviceType", "RGB", "host", "127.0.0.1"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("DRAFT"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        return readText(response, "deviceId");
    }

    private String createCamera() throws Exception {
        String response = mockMvc.perform(post("/v2/config/cameras")
                        .contextPath("/v2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("cameraType", "RGB"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.version").value(1))
                .andReturn()
                .getResponse()
                .getContentAsString();
        return readText(response, "cameraConfigId");
    }

    private String createModel() throws Exception {
        String response = mockMvc.perform(post("/v2/config/models")
                        .contextPath("/v2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("modelType", "DETECTION", "runtimeType", "PYTHON"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.version").value(1))
                .andReturn()
                .getResponse()
                .getContentAsString();
        return readText(response, "modelConfigId");
    }

    private String createTransfer() throws Exception {
        String response = mockMvc.perform(post("/v2/config/transfers")
                        .contextPath("/v2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("transferType", "SFTP"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.version").value(1))
                .andReturn()
                .getResponse()
                .getContentAsString();
        return readText(response, "transferConfigId");
    }

    private String readText(String response, String fieldName) throws Exception {
        JsonNode root = objectMapper.readTree(response);
        String value = root.path("data").path(fieldName).asText();
        assertThat(value).isNotBlank();
        return value;
    }

    private String json(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }

    private byte[] pngBytes() throws Exception {
        BufferedImage image = new BufferedImage(80, 40, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(image, "png", output);
        return output.toByteArray();
    }
}
