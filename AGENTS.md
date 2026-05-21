# AGENTS.md

## 项目基线

- 项目类型：Spring Boot 4.0.6 单体后端，Java 21，Maven Wrapper。
- 入口类：`src/main/java/com/zhongyan/uav/ZhongyanUavApplication.java`。
- 根包名：`com.zhongyan.uav`。
- 默认端口：`8088`。
- 服务上下文前缀：`/v2`。
- 当前是清洁重构项目，不保留旧接口、旧返回结构和旧代码组织。

## 当前环境事实

- `pom.xml` 已升级到 Spring Boot 4.x 和 Java 21。
- `application.yml` 默认排除 `DataSourceAutoConfiguration` 和 `FlywayAutoConfiguration`，默认走 mock 仓储和 mock 设备能力。
- `application-test.yml` 使用 mock repository、mock device、关闭 Flyway、关闭真实 Agent、UDP、Kafka、GeoServer 和对象存储。
- `application-local.yml` 打开 PostgreSQL、Flyway、Redis、Kafka、MinIO、GeoServer 等本地依赖，仓储默认 `jpa`。
- `application-agent-local.yml` 和 `application-agent-online.yml` 用于启用真实 Agent 模型调用。
- 本地依赖编排位于 `deploy/docker-compose.local.yml`，包含 PostgreSQL/PostGIS/pgvector、Redis、Kafka、MinIO、GeoServer，可选 Ollama。
- `src/main/resources/static/images` 是历史业务数据目录，不是正式业务主存储；`pom.xml` 打包时已排除 `static/images/**`。

## 已落地模块

当前 `com.zhongyan.uav` 下已有以下新模块，不要重新创建平行实现：

- `common`：配置、统一响应、错误处理、安全基础能力。
- `mission`：Mission API、应用服务、领域模型、mock/JPA 仓储。
- `task`：Task、TaskCommand、TaskEvent、TaskAttempt、审批、调度和执行器。
- `configcenter`：设备、相机、模型、传输配置及版本。
- `device`：设备命令、mock/SSH/HTTP 适配。
- `asset`：资产、任务资产、预览、MinIO/mock 存储。
- `geo`：坐标、高程、图层发布、GeoServer/mock 端口。
- `telemetry`：遥测写入、查询、UDP/Kafka/mock。
- `event`：outbox、Kafka/mock 事件发布、回放、死信诊断。
- `realtime`：SSE/WebSocket 推送。
- `agent`：会话、消息、工具网关、RAG、报告草稿、Spring AI 适配、Agent 工具和审计。

## 仍未完成或仅部分落地

后续任务只围绕真实缺口增量推进，不再补重复脚手架。

- JWT/RBAC 仍未完成；当前安全基线是 stateless + HTTP Basic + 方法安全。
- 真实 PostgreSQL/Flyway/pgvector、Kafka、Redis、MinIO、GeoServer、模型服务、真实设备需要按 profile 明确验收。
- 默认 mock 环境可以用于单元和应用服务测试，但不能代表 real profile 验收。
- `application.yml` 中仍保留旧设备、资源路径、UDP 等历史线索；迁移到配置中心前不得把它们当作新业务设计来源。
- `static/images` 仍有大量历史数据，新测试不得直接读写该目录；需要样例时放到测试 fixture。
- Agent 后续只收敛一个真实闭环：模型调用、必要上下文摘要、Tool Gateway 审批、实时事件、报告资产和外部依赖验收；不扩展多 Agent、多轮工具规划或复杂限流矩阵。
- 真实设备启动、停止、删除、发布、覆盖等高风险动作必须通过 TaskCommand 和审批，不能在 Controller、Agent 工具或适配器调用点绕过。

## 目录与分层规则

- 新代码必须放在 `com.zhongyan.uav` 下，并优先复用已存在模块。
- API 层只做请求 DTO、响应 DTO、参数校验和鉴权入口。
- Application 层负责编排、事务边界、权限、事件和跨领域用例。
- Domain 层不得依赖 Spring、JPA、Kafka、Redis、MinIO、JSch、HTTP、文件系统。
- Port 只在真实外部依赖边界出现时新增；普通业务逻辑不得套端口/适配器。
- Infrastructure 负责 JPA、Kafka、Redis、MinIO、GeoServer、SSH、HTTP、UDP、Spring AI 等具体实现。
- 不恢复旧 `controller`、`service.impl`、`dao`、`utils`、`conf`、`response`、`exception`、`filter` 等根级组织。

## 接口与业务约束

- 所有新接口仍通过 `/v2` 上下文暴露。
- 禁止恢复旧路径，例如 `/camera/control`、`/data/select`、`/data/recent`、`/uav/info/realtime`。
- 不新增旧接口兼容层、旧字段兼容层、旧返回结构兼容层。
- 高风险动作必须进入 TaskCommand 和审批链路。
- Agent 只能通过 Tool Gateway 调用系统能力；写操作必须可审计、可回放。
- 设备、共享目录、UDP、IP 地址、远程 shell 等真实依赖必须通过已有端口或适配器隔离。

## 修改前检查

开始改动前至少确认：

- 读取 `pom.xml`、`src/main/resources/application.yml`、相关 profile 配置和 `后端重构计划.md`。
- 搜索并复用已有 Controller、Service、DTO、Command、Response、Exception、Config、Domain model、Port、Adapter。
- 多文件改动前确认是否影响 `/v2` 路由、静态资源路径、数据库 migration、远程设备命令或 Agent 工具审批。
- 工作区可能已有用户改动；不得还原无关文件。

## 代码增长控制

- 默认采用最小可落地修改。
- 单次任务默认最多新增 3 个 Java 文件。
- 单次任务默认最多修改 5 个文件。
- 超过上述范围时，先输出拟新增/修改清单和原因，等待确认。
- 禁止顺手做架构升级、目录重组、统一风格、类重命名、兼容层补充或大规模重构。
- 除非已有两个以上真实调用场景，否则禁止新增 Manager、Helper、Util、Adapter、Wrapper、Factory、Strategy、Context、Resolver、Executor、Handler 等抽象。
- 如果确实需要新增抽象，必须说明为什么已有类不能承载、当前调用方数量、不新增会造成的具体问题。

## 删除与迁移

- 新实现替代旧实现时，同步删除无用代码。
- 禁止留下未使用 Controller、Service、DTO、配置项、工具类或大段注释旧代码。
- 发现旧代码引用时，优先删除或迁移，不新增桥接层。
- 涉及 `static/images` 时默认只读，不批量重命名、移动、清理或作为测试输出目录。

## 编码与文本

- 仓库存在中文注释或文档在终端中显示乱码的情况；未确认编码前不要批量重写文件编码。
- 编辑时保持原文件风格和换行方式。
- 中文业务文档使用 UTF-8。

## 运行与验证

常用命令：

```powershell
.\mvnw.cmd test
.\mvnw.cmd clean package
.\mvnw.cmd spring-boot:run
.\mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=local
.\mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=local,agent-local
```

本地依赖：

```powershell
docker compose -f deploy/docker-compose.local.yml up -d
docker compose -f deploy/docker-compose.local.yml --profile agent up -d agent-model
```

验证要求：

- 做代码改动后至少运行 `.\mvnw.cmd test`。
- 如果缺少 `JAVA_HOME`、Docker、本地依赖、模型服务或真实设备，要在结果说明中明确。
- 设备、共享目录、UDP、GeoServer、MinIO、Kafka、pgvector、真实模型调用必须区分 mock 与 real 环境。
- 不允许为了通过测试新增绕路逻辑；测试失败时先定位真实原因。

## 提交说明必须包含

每次完成后说明：

1. 改了什么，为什么这样改。
2. 新增了哪些文件，为什么必须新增。
3. 删除了哪些旧代码。
4. 复用了哪些已有结构。
5. 是否引入新的抽象层。
6. 运行了哪些验证。
7. 哪些设备、网络或外部依赖没有实际验证。
