package com.zhongyan.uav.agent.application;

import com.zhongyan.uav.agent.infrastructure.PgVectorStoreAdapter;
import com.zhongyan.uav.agent.infrastructure.tool.ReportGenerateDraftTool;
import com.zhongyan.uav.agent.port.AgentToolContext;
import com.zhongyan.uav.agent.port.AgentToolResult;
import com.zhongyan.uav.agent.port.RagDocument;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class RagAndReportDraftServiceTests {
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-05-19T08:00:00Z"), ZoneOffset.UTC);

    @Test
    void indexesMissionTaskDeviceLogAndReportMaterialWithTraceableMetadata() {
        CapturingEmbeddingPort embeddingPort = new CapturingEmbeddingPort();
        RagService service = new RagService(embeddingPort, new PgVectorStoreAdapter(), CLOCK);

        RagDocument mission = service.indexMissionSummary("mission-1",
                "mission-1 detected landslide risk near the west ridge", Map.of("area", "west"));
        service.indexTaskTimeline("mission-1", "task-1",
                List.of("capture completed", "processing found risk"), Map.of());
        service.indexDeviceDocument("device-1", "RGB camera manual",
                "camera health and capture configuration", Map.of("missionId", "mission-1"));
        service.indexTaskLog("task-1", "attempt-1", "processing log",
                "model output contains risk score 0.82", Map.of("missionId", "mission-1"));
        service.indexReportMaterial("report-source-1", "Historical report",
                "previous mitigation report material", Map.of("missionId", "mission-1"));

        assertThat(mission.sourceType()).isEqualTo("MISSION_SUMMARY");
        assertThat(mission.snippet()).contains("landslide risk");
        assertThat(mission.metadata()).containsEntry("embeddingDimension", 3);
        assertThat(mission.metadata()).containsEntry("indexedAt", CLOCK.instant().toString());
        assertThat(embeddingPort.embeddedTexts()).hasSize(5);

        List<RagDocument> missionResults = service.search("risk", 10, Map.of("missionId", "mission-1"));
        assertThat(missionResults).extracting(RagDocument::sourceType)
                .contains("MISSION_SUMMARY", "TASK_LOG");
        assertThat(service.search("risk", 10, Map.of("missionId", "other"))).isEmpty();
    }

    @Test
    void reportDraftContainsStructuredSectionsCitationsAndConfirmationNotice() {
        ReportDraftService service = new ReportDraftService(CLOCK);
        RagDocument source = new RagDocument("doc-1", "TASK_TIMELINE", "task-1",
                "Task timeline", "capture completed", "capture completed", Map.of("missionId", "mission-1"));

        ReportDraft draft = service.generate(new ReportDraftRequest("任务总结报告", "MISSION_SUMMARY",
                "mission-1", "task-1", "任务完成采集和处理。",
                List.of("08:00 采集完成"), List.of("rgb-output.tif"),
                List.of("识别到疑似滑坡风险"), List.of("传输日志正常"),
                List.of("建议复核高风险区域"), List.of(source)));

        assertThat(draft.status()).isEqualTo("DRAFT_REQUIRES_CONFIRMATION");
        assertThat(draft.markdown()).contains("# 任务总结报告");
        assertThat(draft.markdown()).contains("## 引用来源");
        assertThat(draft.markdown()).contains("[S1]");
        assertThat(draft.markdown()).contains("正式报告文件必须经用户确认");
        assertThat(draft.citations()).hasSize(1);
        assertThat(draft.structured()).containsEntry("confirmationRequired", true);
    }

    @Test
    void reportGenerateDraftToolUsesRagSourcesAndReturnsMarkdownDraft() {
        RagService ragService = new RagService(text -> List.of(1.0, 2.0), new PgVectorStoreAdapter(), CLOCK);
        ragService.indexMissionSummary("mission-1", "mission-1 has processed RGB and HSI outputs.", Map.of());
        ReportGenerateDraftTool tool = new ReportGenerateDraftTool(new ReportDraftService(CLOCK), ragService);

        AgentToolResult result = tool.execute(new AgentToolContext("session-1", "analyst-1",
                "agent:session-1", Set.of("AGENT_REPORT_DRAFT"), Map.of()),
                Map.of("missionId", "mission-1", "reportType", "MISSION_SUMMARY",
                        "agentFindings", List.of("需要复核输出资产")));

        assertThat(result.success()).isTrue();
        assertThat(result.output()).containsEntry("status", "DRAFT_REQUIRES_CONFIRMATION");
        assertThat(String.valueOf(result.output().get("markdown"))).contains("Agent 任务报告草稿");
        assertThat(result.output()).containsEntry("citationCount", 1);
    }

    private static final class CapturingEmbeddingPort implements com.zhongyan.uav.agent.port.EmbeddingPort {
        private final List<String> embeddedTexts = new ArrayList<>();

        @Override
        public List<Double> embed(String text) {
            embeddedTexts.add(text);
            return List.of(0.1, 0.2, 0.3);
        }

        private List<String> embeddedTexts() {
            return embeddedTexts;
        }
    }
}
