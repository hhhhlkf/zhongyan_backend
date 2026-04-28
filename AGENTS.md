# AGENTS.md

## 项目概览

- 项目类型：`Spring Boot 2.7.5` 单体后端，`Java 11`，`Maven Wrapper`
- 当前重构方向：清洁重构，不保留旧接口、旧返回结构和旧代码组织
- 入口类：`src/main/java/com/zhongyan/uav/ZhongyanUavApplication.java`
- 根包名：`com.zhongyan.uav`
- 默认端口：`8088`
- 服务前缀：`/v2`
- 当前仍排除 `DataSourceAutoConfiguration`

## 目录约定

- `src/main/java/com/zhongyan/uav/common`：重构后的通用基础能力
- `src/main/java/com/zhongyan/uav/common/config`：Spring MVC、安全等基础配置
- `src/main/java/com/zhongyan/uav/common/error`：错误码、业务异常、全局异常处理
- `src/main/java/com/zhongyan/uav/common/response`：统一响应包装
- `src/main/java/com/zhongyan/uav/common/security`：当前用户、权限、安全相关模型
- `src/test/java/com/zhongyan/uav`：测试代码
- `src/main/resources/application.yml`：核心运行配置
- `src/main/resources/static/images`：历史业务数据目录，重构后不作为正式业务主存储

后续新增业务模块按 `后端重构计划.md` 组织，例如：

- `mission`
- `task`
- `configcenter`
- `device`
- `asset`
- `geo`
- `telemetry`
- `event`
- `realtime`
- `agent`

## 运行与验证

- 启动开发环境：`.\mvnw.cmd spring-boot:run`
- 打包：`.\mvnw.cmd clean package`
- 测试：`.\mvnw.cmd test`

如果在类 Unix 环境中工作，可使用 `./mvnw`。

## 当前代码事实

- 旧 `controller`、`service`、`service.impl`、`dao`、`utils`、`conf`、`response`、`exception`、`filter` 组织已开始删除。
- 当前基础配置位于 `common.config.SecurityConfig` 和 `common.config.WebMvcConfig`。
- 当前安全配置不再是 `permitAll`，默认需要认证；正式 JWT/RBAC 仍待后续实现。
- `application.yml` 中仍保留旧设备、资源、UDP 等配置线索，后续应进入配置中心或被删除。
- `static/images` 中的历史数据不要在测试或重构脚本中直接改写。

## 修改原则

- 按清洁重构推进，不新增旧接口兼容层。
- 不恢复旧根包名和旧组织结构。
- 新代码放在 `com.zhongyan.uav` 下，并按领域模块分层。
- 领域层不依赖 Spring、JPA、Kafka、MinIO、JSch。
- 设备控制、远程 shell、共享目录、UDP、IP 地址等真实依赖必须通过端口和适配器封装。
- 涉及 `static/images` 时默认视为历史业务数据，不批量重命名、移动或清理。
- 不默认启用数据库、Redis、Kafka、MinIO、GeoServer；接入时按阶段和配置明确完成。

## 编码与文本注意事项

- 仓库中存在中文注释或 README 在终端中显示乱码的情况。
- 未确认文件原始编码前，不要批量重写现有文件编码。
- 编辑时优先保持原文件风格和换行方式。

## 接口与业务约束

- 新接口仍通过上下文前缀 `/v2` 暴露。
- 不保留旧路径，例如 `/camera/control`、`/data/select`、`/data/recent`、`/uav/info/realtime`。
- 新接口按 Mission、Task、Config、Asset、Telemetry、Agent 等模块重新定义。
- 高风险动作必须通过 TaskCommand 和审批机制，不允许直接执行。

## 测试策略

- 做代码改动后至少运行 `.\mvnw.cmd test`。
- 如果当前环境缺少 `JAVA_HOME` 或真实设备不可用，要在提交说明中明确写出。
- 设备、共享目录、UDP、GeoServer、MinIO、Kafka 相关验证必须区分 mock 与 real 环境。

## 对代理的工作要求

- 在开始改动前，先读 `pom.xml`、`application.yml`、`后端重构计划.md` 和相关新模块文件。
- 搜索优先聚焦 `com.zhongyan.uav` 下的新模块。
- 做多文件改动时，先确认是否会影响 `/v2` 路由、静态资源路径或远程设备命令。
- 提交结果时说明：
  - 改了什么
  - 为什么这样改
  - 运行了哪些验证
  - 哪些设备/网络相关部分没有实际验证
