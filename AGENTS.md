# AGENTS.md

## 项目概览

- 项目类型：`Spring Boot 2.7.5` 单体后端，`Java 11`，`Maven Wrapper`
- 入口类：`src/main/java/com/gosling/bms/BmsApplication.java`
- 默认端口：`8088`
- 服务前缀：`/v2`
- 当前配置关闭了数据库自动装配，项目主要依赖本地静态资源、文件系统、局域网共享目录、远程设备命令和若干模拟数据接口

## 目录约定

- `src/main/java/com/gosling/bms/controller`：HTTP 接口层
- `src/main/java/com/gosling/bms/service`：业务接口
- `src/main/java/com/gosling/bms/service/impl`：业务实现，包含设备控制、文件分发、UDP 接收等逻辑
- `src/main/java/com/gosling/bms/conf`：Spring MVC、安全、跨域、相机配置等
- `src/main/java/com/gosling/bms/response`：统一返回包装
- `src/main/java/com/gosling/bms/exception`：业务异常和全局异常处理
- `src/main/java/com/gosling/bms/utils`：文件、JWT、网络、字符串等工具类
- `src/main/resources/application.yml`：核心运行配置
- `src/main/resources/static`：静态资源
- `src/main/resources/static/images`：项目关键数据目录，包含演示图片、视频、切片、日志和处理结果
- `src/test/java/com/gosling/bms`：测试代码，目前只有基础上下文测试

## 运行与验证

- 启动开发环境：`.\mvnw.cmd spring-boot:run`
- 打包：`.\mvnw.cmd clean package`
- 测试：`.\mvnw.cmd test`

如果在类 Unix 环境中工作，可使用 `./mvnw`。

## 当前代码事实

- 安全配置位于 `SpringSecurityConfig`，当前 `anyRequest().permitAll()`，JWT 过滤器已定义但未实际接入过滤链
- `application.yml` 中排除了 `DataSourceAutoConfiguration`，不要默认引入数据库相关改造
- 服务有明显的设备/网络依赖：
  - 摄像头与传输主机通过 `server-param` 配置远程地址和 shell 命令
  - 部分逻辑依赖 UNC 路径，例如 `\\\\192.168.101.100\\FileRecv_Shared\\Rate`
  - `FileUtils` 会根据本机网卡推导静态资源访问地址
- 数据和演示资源直接保存在仓库内的 `src/main/resources/static/images`

## 修改原则

- 优先做小而准的改动，不要顺手重构整条链路
- 修改接口时，保持已有返回结构稳定，优先复用 `@ResponseResult` 和现有异常体系
- 涉及 `static/images` 时默认视为业务数据，不要批量重命名、移动或清理
- 涉及设备控制、远程 shell、共享目录、UDP、IP 地址时，先确认是否为真实运行依赖，再决定是否改动
- 不要默认启用数据库、Redis、MyBatis、JPA；这些配置在当前仓库中不是激活状态
- 如需新增配置，优先放入 `application.yml` 并保持与现有层次一致

## 编码与文本注意事项

- 仓库中存在中文注释或 README 在终端中显示乱码的情况
- 未确认文件原始编码前，不要批量重写现有文件编码，不要做“顺手修复乱码”的大范围改动
- 编辑时优先保持原文件风格和换行方式

## 接口与业务约束

- 所有接口实际访问路径都要带上上下文前缀 `/v2`
- 当前接口风格以 `GET` 为主，即使包含控制类动作，也不要未经确认擅自改成 `POST`
- `DeviceController`、`DataController`、`EmergencyController`、`CollAndProcController`、`Process2Controller` 是主要入口，改动前先确认调用路径是否被前端或外部脚本依赖

## 测试策略

- 当前自动化测试覆盖很弱；做功能改动时，至少运行 `.\mvnw.cmd test`
- 如果改动涉及文件路径、设备通信或共享目录，尽量增加可本地执行的单元测试或最小化验证
- 无法在当前环境验证真实设备时，要在提交说明中明确写出未验证项

## 对代理的工作要求

- 在开始改动前，先读 `pom.xml`、`application.yml` 和相关 controller/service 文件，不要凭 Spring Boot 常识猜测
- 搜索优先聚焦 `controller`、`service/impl`、`conf`、`utils/FileUtils.java`
- 做多文件改动时，先确认是否会影响 `/v2` 路由、静态资源路径或远程设备命令
- 提交结果时说明：
  - 改了什么
  - 为什么这样改
  - 运行了哪些验证
  - 哪些设备/网络相关部分没有实际验证
