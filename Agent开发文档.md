# Agent 模块开发文档

## 目标边界

Agent 模块只承担系统内 AI 编排入口，不拆分多个常驻 Agent，不直接执行设备、资产删除、图层发布、数据库写入等高风险动作。

Agent 的职责：

- 管理会话、消息、工具调用和审计事件。
- 基于 Mission、Task、Asset、Telemetry、TaskEvent、日志和 RAG 材料回答问题。
- 通过 Tool Gateway 调用已有系统能力。
- 对查询、统计、诊断等低风险工具自动执行。
- 对启动、停止、删除、发布、覆盖、真实设备命令等写操作创建待审批链路。
- 生成带来源引用的报告草稿。

不做：

- 不恢复旧接口或旧字段兼容。
- 不让 Agent 绕过 Tool Gateway。
- 不在 Agent 内直接调用 SSH、MinIO、GeoServer、Kafka、JPA Repository 执行业务写操作。
- 不为了 Agent 单独复制 Mission、Task、Asset、Telemetry 的业务逻辑。
- 不新增多 Agent 编排框架，除非已有单入口方案无法覆盖真实需求。

## 当前实现状态

已落地：

- `agent.api`：会话、消息、工具调用、审批相关 REST 入口和 DTO。
- `agent.application`：`AgentApplicationService`、`ToolGatewayService`、`RagService`、`ReportDraftService`。
- `agent.domain`：会话、消息、工具调用、状态、风险等级和仓储接口。
- `agent.port`：Chat、Embedding、VectorStore、AgentTool、AgentEventPublisher、运行期防护等端口。
- `agent.infrastructure`：Spring AI Chat/Embedding 适配、pgvector JDBC 适配、Kafka 事件发布、Redis/Noop runtime guard、JPA/mock 仓储。
- `agent.infrastructure.tool`：Mission、Task、Asset、Telemetry、诊断、报告草稿等工具。
- `application-agent-local.yml`、`application-agent-online.yml`：真实模型 profile 配置。
- `deploy/docker-compose.local.yml`：可选 Ollama `agent-model` 服务。

当前主链路：

1. 创建 Agent 会话。
2. 保存用户消息。
3. 汇聚会话、最近消息、RAG 和用户上下文。
4. 调用 `ChatModelPort`。
5. 解析模型规划的工具调用或 metadata 中的工具调用。
6. 所有工具统一交给 `ToolGatewayService`。
7. 保存助手回复和工具调用审计。
8. 发布 Agent 事件到统一事件/实时推送链路。

## 配置与运行

默认测试环境关闭真实 Agent：

```yaml
bms:
  agent:
    enabled: false
```

本地启用 Agent：

```powershell
docker compose -f deploy/docker-compose.local.yml up -d
docker compose -f deploy/docker-compose.local.yml --profile agent up -d agent-model
.\mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=local,agent-local
```

本地模型需要手动拉取：

```powershell
docker exec zhongyan-uav-agent-model ollama pull qwen3:8b
docker exec zhongyan-uav-agent-model ollama pull qwen3-embedding:0.6b
```

线上 OpenAI-compatible 服务：

```powershell
$env:BMS_AGENT_BASE_URL="https://api.example.com/v1"
$env:BMS_AGENT_API_KEY="sk-xxxx"
$env:BMS_AGENT_CHAT_MODEL="your-chat-model"
$env:BMS_AGENT_EMBEDDING_MODEL="your-embedding-model"
.\mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=local,agent-online
```

关键配置：

- `bms.agent.enabled`：是否创建真实 Spring AI 模型 Bean。
- `bms.agent.base-url`：OpenAI-compatible API 地址。
- `bms.agent.chat-model`：对话模型。
- `bms.agent.embedding-model`：向量模型。
- `bms.agent.approval-required-for-write`：写操作是否强制审批。
- `bms.agent.redis.enabled`：是否启用 Redis 运行期防护。
- `bms.agent.events-topic`：Agent 事件 topic，默认 `agent-events`。

## 开发规则

- Controller 只做参数校验、当前用户解析和 DTO 转换。
- Agent 应用服务只编排，不复制其他模块业务规则。
- 新 Agent 工具必须实现 `AgentTool`，并声明名称、输入 schema、权限、风险等级和审批策略。
- 工具实现优先调用已有 application service 或 query service。
- 写操作工具必须经过审批策略和审计记录。
- 模型规划工具调用可以来自 Spring AI 原生 tool call，也可以来自兼容 JSON，但执行入口只能是 `ToolGatewayService`。
- RAG 文档必须带 `sourceType`、`sourceId`、`title`、`snippet`，报告草稿中的结论必须可追溯。
- 真实外部依赖只能放在 infrastructure；domain 和 application 不直接依赖 Spring AI、Redis、Kafka、JPA、MinIO、GeoServer。

## 已落地闭环

Agent 当前只保留一个可验收闭环，不拆成大量独立增强项：

```text
用户消息 -> 上下文摘要 -> 模型回复 -> 工具调用/审批 -> 事件推送 -> 报告草稿或报告资产
```

已落地：

- 模型调用失败会保存用户消息，并生成明确的助手错误回复，不阻塞会话主链路。
- 工具调用统一进入 `ToolGatewayService`；低风险工具自动执行，高风险工具只进入待审批。
- 上下文汇聚只保留必要摘要：会话、最近消息、绑定 Mission/Task、最近事件、相关资产、遥测概况和 RAG 引用。
- SSE 是当前主实时通道；事件包含消息、工具状态、审批请求、上下文摘要和最终回答。
- `AGENT_ANALYSIS` 执行器已调用 Agent 主链路，产出可追溯分析结果和报告草稿元数据；真实执行动作仍走 TaskCommand。
- 报告草稿工具在 `confirmed=true` 时创建 `REPORT_GENERATION` 草稿任务；正式报告由该任务写入对象存储并创建 `REPORT` Asset。
- `agent-local`、`agent-online` profile 已作为真实依赖验收入口；本地自动测试默认仍走 mock 或内存适配。

明确不做：

- 不做复杂多 Agent 协作。
- 不做无限多轮工具规划；单次用户消息最多执行一轮模型规划工具，后续由用户再次确认或追问。
- 不做复杂限流矩阵；只保留用户级基础限流和工具调用幂等。
- 不同时维护 SSE 与 WebSocket 两套完整语义；保留项目当前可稳定验收的一种为主，另一种只做兼容推送。
- 不为每类模型异常设计独立业务分支，统一归一为模型不可用、调用超时、输出不可解析三类。
- 不新增与现有 Mission、Task、Asset、Telemetry 重复的查询或写入服务。

## 测试要求

最小验证：

```powershell
.\mvnw.cmd test
```

Agent 相关改动至少覆盖：

- 低风险工具自动执行。
- 高风险工具进入待审批。
- 拒绝后不执行。
- 审批后才允许进入写操作链路。
- 工具调用写入 `agent_tool_call`。
- Agent 事件可查询或发布。
- 模型不可用时返回明确错误，不阻塞主链路。

真实依赖验证必须记录 profile、服务地址、关键配置和未验证项。
