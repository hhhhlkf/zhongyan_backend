package com.zhongyan.uav.agent.port;

import com.zhongyan.uav.agent.domain.ToolRiskLevel;
import com.zhongyan.uav.agent.infrastructure.tool.TaskCommandCreateTool;
import com.zhongyan.uav.agent.infrastructure.tool.TaskQueryTool;
import com.zhongyan.uav.task.infrastructure.mock.InMemoryTaskAttemptRepository;
import com.zhongyan.uav.task.infrastructure.mock.InMemoryTaskCommandRepository;
import com.zhongyan.uav.task.infrastructure.mock.InMemoryTaskEventRepository;
import com.zhongyan.uav.task.infrastructure.mock.InMemoryTaskRepository;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AgentPortTests {
    @Test
    void toolDeclaresMetadataRequiredByGatewayPolicy() {
        AgentTool tool = new TaskCommandCreateTool();

        assertThat(tool.name()).isEqualTo("taskCommand.create");
        assertThat(tool.description()).isNotBlank();
        assertThat(tool.riskLevel()).isEqualTo(ToolRiskLevel.HIGH);
        assertThat(tool.approvalRequired()).isTrue();
        assertThat(tool.requiredPermissions()).contains("AGENT_TOOL_APPROVE");
        assertThat(tool.inputSchema().requiredFields()).contains("taskId", "commandType", "reason");
    }

    @Test
    void inputSchemaRejectsMissingRequiredFieldsAndUnknownStrictFields() {
        AgentTool tool = new TaskCommandCreateTool();

        assertThatThrownBy(() -> tool.inputSchema().validate(Map.of("taskId", "task-1")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Missing required Agent tool input field");

        assertThatThrownBy(() -> tool.inputSchema().validate(Map.of(
                        "taskId", "task-1",
                        "commandType", "START_CAPTURE",
                        "reason", "operator requested",
                        "unexpected", true)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("unsupported");
    }

    @Test
    void queryToolCanDeclareOptionalInputOnly() {
        AgentTool tool = new TaskQueryTool(new InMemoryTaskRepository(), new InMemoryTaskEventRepository(),
                new InMemoryTaskAttemptRepository(), new InMemoryTaskCommandRepository());

        assertThat(tool.riskLevel()).isEqualTo(ToolRiskLevel.LOW);
        assertThat(tool.approvalRequired()).isFalse();
        assertThat(tool.inputSchema().requiredFields()).isEmpty();
        tool.inputSchema().validate(Map.of("taskId", "task-1", "includeEvents", true));
    }
}
