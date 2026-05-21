# 后端服务发布 API 文档

生成日期：2026-05-21  
适用服务：`zhongyan-uav-backend`  
服务基线：Spring Boot 4.0.6、Java 21、默认端口 `8088`、上下文前缀 `/v2`

## 1. 发布说明

### 1.1 基础地址

```text
http://{host}:8088/v2
```

示例：

```text
http://localhost:8088/v2/missions
```

### 1.2 认证与权限

当前安全基线为 stateless + HTTP Basic + JWT Token 能力 + 方法级权限。

- 获取 Token：`POST /v2/auth/token`
- 普通业务接口默认返回统一包装。
- 带 `@PreAuthorize` 的接口需要对应权限：
  - Agent 会话：`AGENT_CHAT`
  - Agent 工具审批：`AGENT_TOOL_APPROVE`
  - TaskCommand 写入：`WRITE`、`TASK_APPROVE` 或 `ROLE_ADMIN`
  - Task 审批：`TASK_APPROVE` 或 `ROLE_ADMIN`

### 1.3 统一返回结构

除 SSE 流接口外，大多数 API 通过 `@ResponseResult` 或显式 `ApiResult` 返回：

```json
{
  "code": 200,
  "message": "success",
  "data": {}
}
```

错误码枚举：

```text
200 SUCCESS
400 BAD_REQUEST
401 UNAUTHORIZED
403 FORBIDDEN
404 NOT_FOUND
409 CONFLICT
429 TOO_MANY_REQUESTS
500 INTERNAL_ERROR
```

下文的“返回结构”均表示 `data` 字段结构；SSE 接口单独说明。

### 1.4 运行环境注意

- 默认 `application.yml` 排除 DataSource 和 Flyway，使用 mock repository、mock device。
- `local` profile 才启用 PostgreSQL/Flyway、Redis、Kafka、MinIO、GeoServer 等真实依赖。
- 高风险设备动作、发布、删除、覆盖等必须走 TaskCommand 和审批链路；不要绕过 Controller 或 Agent Tool Gateway 直接调用设备适配器。

## 2. 公共结构

### 2.1 AuthToken

```json
{
  "tokenType": "Bearer",
  "accessToken": "jwt-token",
  "expiresIn": 28800
}
```

### 2.2 MissionView

```json
{
  "missionId": "string",
  "name": "string",
  "scenarioType": "string",
  "status": "DRAFT|ACTIVE|PAUSED|COMPLETED|CANCELLED|FAILED",
  "priority": 0,
  "createdBy": "string",
  "createdAt": "2026-05-21T00:00:00Z",
  "startedAt": "2026-05-21T00:00:00Z",
  "endedAt": "2026-05-21T00:00:00Z",
  "description": "string"
}
```

### 2.3 TaskView

```json
{
  "taskId": "string",
  "missionId": "string",
  "taskType": "CAPTURE|PROCESS|TRANSFER|CALCULATE_GEO_BOUNDARY|GENERATE_PREVIEW|PUBLISH_LAYER|DEMO_PLAYBACK|AGENT_ANALYSIS|REPORT_GENERATION",
  "status": "DRAFT|WAITING_APPROVAL|QUEUED|DISPATCHING|RUNNING|WAITING_ASSET|POST_PROCESSING|COMPLETED|FAILED|RETRYING|CANCELLED|TIMEOUT",
  "priority": 0,
  "deviceId": "string",
  "modelId": "string",
  "inputAssetIds": ["string"],
  "outputAssetIds": ["string"],
  "progress": 0,
  "createdBy": "string",
  "createdAt": "2026-05-21T00:00:00Z",
  "updatedAt": "2026-05-21T00:00:00Z",
  "startedAt": "2026-05-21T00:00:00Z",
  "endedAt": "2026-05-21T00:00:00Z",
  "errorCode": "string",
  "errorMessage": "string"
}
```

### 2.4 TaskCommandView

```json
{
  "commandId": "string",
  "taskId": "string",
  "missionId": "string",
  "deviceId": "string",
  "commandType": "START_CAPTURE|STOP_DEVICE|START_PROCESS|STOP_PROCESS|START_TRANSFER|CALCULATE_GEO_BOUNDARY|GENERATE_PREVIEW|CANCEL_TASK|RETRY_TASK|DELETE_ASSET|OVERWRITE_ASSET|PUBLISH_LAYER|START_DEMO_PLAYBACK|START_AGENT_ANALYSIS|GENERATE_REPORT|UPDATE_DEVICE_COMMAND|UPDATE_MODEL_COMMAND",
  "idempotencyKey": "string",
  "requestedBy": "string",
  "riskLevel": "LOW|MEDIUM|HIGH|CRITICAL",
  "requiresApproval": true,
  "approvedBy": "string",
  "approvedAt": "2026-05-21T00:00:00Z",
  "status": "PENDING_APPROVAL|PENDING_DISPATCH|APPROVED|REJECTED|DISPATCHED|COMPLETED|FAILED|CANCELLED",
  "createdAt": "2026-05-21T00:00:00Z",
  "dispatchedAt": "2026-05-21T00:00:00Z",
  "completedAt": "2026-05-21T00:00:00Z",
  "reason": "string"
}
```

### 2.5 AssetView

资产模块返回：

```json
{
  "assetId": "string",
  "assetType": "string",
  "status": "string",
  "name": "string",
  "url": "string",
  "previewUrl": "string",
  "geoStatus": "string",
  "layerUrl": "string",
  "createdAt": "2026-05-21T00:00:00Z"
}
```

Task 关联资产返回：

```json
{
  "assetId": "string",
  "missionId": "string",
  "taskId": "string",
  "assetType": "IMAGE|VIDEO|MODEL_RESULT|ATTEMPT_LOG|REPORT|ATTACHMENT|GEOMETRY|LAYER",
  "assetRole": "RAW|INPUT|OUTPUT|PREVIEW|LOG|REPORT|ATTACHMENT",
  "status": "CREATED|UPLOADING|AVAILABLE|FAILED|DELETED",
  "geoStatus": "NOT_REQUIRED|PENDING|CALCULATED|FAILED",
  "name": "string",
  "objectKey": "string",
  "previewObjectKey": "string",
  "layerUrl": "string",
  "createdAt": "2026-05-21T00:00:00Z"
}
```

### 2.6 Config Views

`DeviceConfigView`

```json
{
  "deviceId": "string",
  "deviceName": "string",
  "deviceType": "string",
  "host": "string",
  "status": "DRAFT|ACTIVE|DISABLED",
  "updatedAt": "2026-05-21T00:00:00Z"
}
```

`CameraConfigVersionView`

```json
{
  "cameraConfigId": "string",
  "version": 1,
  "cameraType": "string",
  "status": "DRAFT|ACTIVE|DISABLED",
  "fov": {},
  "updatedAt": "2026-05-21T00:00:00Z"
}
```

`ModelConfigVersionView`

```json
{
  "modelConfigId": "string",
  "version": 1,
  "modelType": "string",
  "runtimeType": "string",
  "status": "DRAFT|ACTIVE|DISABLED",
  "updatedAt": "2026-05-21T00:00:00Z"
}
```

`TransferConfigVersionView`

```json
{
  "transferConfigId": "string",
  "version": 1,
  "transferType": "string",
  "status": "DRAFT|ACTIVE|DISABLED",
  "updatedAt": "2026-05-21T00:00:00Z"
}
```

`ConfigValidationView`

```json
{
  "validationId": "string",
  "configType": "string",
  "configId": "string",
  "status": "PASSED|FAILED",
  "errors": ["string"],
  "createdAt": "2026-05-21T00:00:00Z"
}
```

`ConfigActionView`

```json
{
  "configType": "DEVICE|CAMERA|MODEL|TRANSFER",
  "configId": "string",
  "action": "ACTIVATE|DISABLE",
  "status": "ACCEPTED",
  "acceptedAt": "2026-05-21T00:00:00Z"
}
```

## 3. 认证 API

| 方法 | 地址 | 输入结构 | 返回结构 | 调用逻辑 |
| --- | --- | --- | --- | --- |
| POST | `/auth/token` | Body：`{"username":"string","password":"string"}` | `AuthToken` | `AuthController` 校验用户名密码非空，调用 `AuthenticationManager.authenticate`，成功后用 `JwtTokenService.createToken` 生成 JWT；失败返回 401。 |

## 4. Mission 模块 API

| 方法 | 地址 | 输入结构 | 返回结构 | 调用逻辑 |
| --- | --- | --- | --- | --- |
| POST | `/missions` | Body：`CreateMissionRequest` | `MissionView` | `MissionController` 将请求转为 `CreateMissionInput`，调用 `MissionApplicationService.createMission`，再转为 `MissionView`。 |
| GET | `/missions?status=DRAFT` | Query：`status`，默认 `DRAFT` | `MissionView[]` | 调用 `MissionQueryService.listByStatus(status)` 查询指定状态任务批次。 |
| GET | `/missions/{missionId}` | Path：`missionId` | `MissionView` | 调用 `MissionQueryService.getMission(missionId)` 查询详情。 |

`CreateMissionRequest`

```json
{
  "name": "string",
  "scenarioType": "string",
  "region": {},
  "priority": 0,
  "createdBy": "string",
  "description": "string"
}
```

## 5. Task 模块 API

| 方法 | 地址 | 输入结构 | 返回结构 | 调用逻辑 |
| --- | --- | --- | --- | --- |
| POST | `/missions/{missionId}/tasks` | Path：`missionId`；Body：`CreateTaskRequest` | `TaskView` | `TaskController` 将请求转为 `CreateTaskInput`，调用 `TaskApplicationService.createTask`。 |
| GET | `/missions/{missionId}/tasks` | Path：`missionId` | `TaskView[]` | 调用 `TaskQueryService.listTasksByMission(missionId)`。 |
| GET | `/tasks/{taskId}` | Path：`taskId` | `TaskView` | 调用 `TaskQueryService.getTask(taskId)`。 |
| POST | `/tasks/{taskId}/submit` | Path：`taskId`；Body 可空：`{"submittedBy":"string"}` | `TaskView` | 调用 `TaskApplicationService.submitTask(taskId, submittedBy)`，空提交人默认 `mock-user`。 |
| POST | `/tasks/{taskId}/retry` | Path：`taskId`；Body 可空：`{"requestedBy":"string"}` | `TaskView` | 调用 `TaskApplicationService.retryTask(taskId, requestedBy)`，空请求人默认 `mock-user`。 |
| POST | `/tasks/{taskId}/cancel` | Path：`taskId`；Body 可空：`{"cancelledBy":"string"}` | `TaskView` | 调用 `TaskApplicationService.cancelTask(taskId, cancelledBy)`，空取消人默认 `mock-user`。 |
| GET | `/tasks/{taskId}/assets` | Path：`taskId` | Task 关联 `AssetView[]` | 调用 `TaskQueryService.listAssets(taskId)` 查询任务绑定资产。 |
| GET | `/tasks/{taskId}/attempts` | Path：`taskId` | `TaskAttemptView[]` | `TaskAttemptController` 调用 `TaskQueryService.listAttempts(taskId)` 查询执行尝试。 |
| GET | `/tasks/{taskId}/events` | Path：`taskId` | `TaskEventView[]` | `TaskEventController` 调用 `TaskQueryService.listEvents(taskId)` 查询任务事件时间线。 |

`CreateTaskRequest`

```json
{
  "taskType": "CAPTURE",
  "priority": 0,
  "deviceId": "string",
  "modelId": "string",
  "configSnapshot": {},
  "inputAssetIds": ["string"],
  "createdBy": "string"
}
```

`TaskAttemptView`

```json
{
  "attemptId": "string",
  "taskId": "string",
  "attemptNo": 1,
  "executorNode": "string",
  "startedAt": "2026-05-21T00:00:00Z",
  "endedAt": "2026-05-21T00:00:00Z",
  "result": "RUNNING|SUCCESS|FAILED|TIMEOUT|CANCELLED",
  "errorCode": "string",
  "errorMessage": "string",
  "rawLogObjectKey": "string"
}
```

`TaskEventView`

```json
{
  "eventId": "string",
  "taskId": "string",
  "eventType": "CREATED|SUBMITTED|COMMAND_CREATED|APPROVED|DISPATCHED|STARTED|PROGRESS_CHANGED|ASSET_BOUND|ASSET_CREATED|GEO_CALCULATED|GEO_FAILED|LAYER_PENDING_APPROVAL|LAYER_PUBLISHED|COMPLETED|FAILED|RETRYING|CANCELLED|TIMEOUT",
  "statusBefore": "DRAFT",
  "statusAfter": "QUEUED",
  "payload": {},
  "createdAt": "2026-05-21T00:00:00Z"
}
```

## 6. TaskCommand 与审批 API

| 方法 | 地址 | 输入结构 | 返回结构 | 调用逻辑 |
| --- | --- | --- | --- | --- |
| POST | `/tasks/{taskId}/commands` | Path：`taskId`；Body：`CreateTaskCommandRequest` | `TaskCommandView` | `TaskCommandController` 调用 `TaskCommandApplicationService.createCommand`；高风险命令会进入审批状态。需要 `WRITE`、`TASK_APPROVE` 或 `ROLE_ADMIN`。 |
| GET | `/tasks/{taskId}/commands` | Path：`taskId` | `TaskCommandView[]` | 调用 `TaskQueryService.listCommands(taskId)`。 |
| GET | `/approvals/pending` | 无 | `TaskCommandView[]` | `TaskApprovalController` 调用 `TaskApprovalService.listPendingApprovals` 查询待审批命令。需要 `TASK_APPROVE` 或 `ROLE_ADMIN`。 |
| GET | `/approvals/{commandId}` | Path：`commandId` | `TaskCommandView` | 调用 `TaskApprovalService.getCommand(commandId)`。 |
| POST | `/approvals/{commandId}/approve` | Body 可空：`{"approver":"string","reason":"string"}` | `TaskCommandView` | 调用 `TaskApprovalService.approveCommand(commandId, approver)`；空审批人默认 `mock-approver`。 |
| POST | `/approvals/{commandId}/reject` | Body 可空：`{"approver":"string","reason":"string"}` | `TaskCommandView` | 调用 `TaskApprovalService.rejectCommand(commandId, approver)`；当前 Controller 只传审批人，`reason` 未下传。 |

`CreateTaskCommandRequest`

```json
{
  "commandType": "START_CAPTURE",
  "payload": {},
  "idempotencyKey": "string",
  "requestedBy": "string",
  "riskLevel": "LOW|MEDIUM|HIGH|CRITICAL",
  "deviceId": "string",
  "reason": "string"
}
```

## 7. Config Center 模块 API

### 7.1 Device Config

| 方法 | 地址 | 输入结构 | 返回结构 | 调用逻辑 |
| --- | --- | --- | --- | --- |
| POST | `/config/devices` | Body：`CreateDeviceConfigRequest` | `DeviceConfigView` | `DeviceConfigController` 调用 `DeviceConfigApplicationService.createDevice` 创建设备配置。 |
| GET | `/config/devices` | 无 | `DeviceConfigView[]` | 调用 `DeviceConfigApplicationService.listDevices`。 |
| GET | `/config/devices/{deviceId}` | Path：`deviceId` | `DeviceConfigView` | 调用 `DeviceConfigApplicationService.getDevice`。 |
| POST | `/config/devices/{deviceId}/validate` | Path：`deviceId` | `ConfigValidationView` | 调用 `ConfigValidationService.validateDevice`。 |
| POST | `/config/devices/{deviceId}/disable` | Path：`deviceId` | `ConfigActionView` | 调用 `DeviceConfigApplicationService.disableDevice`，返回禁用动作已接收。 |

`CreateDeviceConfigRequest`

```json
{
  "deviceId": "string",
  "deviceName": "string",
  "deviceType": "string",
  "host": "string",
  "connection": {},
  "capabilities": {},
  "createdBy": "string"
}
```

### 7.2 Camera Config

| 方法 | 地址 | 输入结构 | 返回结构 | 调用逻辑 |
| --- | --- | --- | --- | --- |
| POST | `/config/cameras` | Body：`CreateCameraConfigRequest` | `CameraConfigVersionView` | 调用 `CameraConfigApplicationService.createCamera`。 |
| GET | `/config/cameras` | 无 | `CameraConfigVersionView[]` | 调用 `CameraConfigApplicationService.listCameras`。 |
| GET | `/config/cameras/{cameraConfigId}` | Path：`cameraConfigId` | `CameraConfigVersionView` | 调用 `CameraConfigApplicationService.getCamera`。 |
| POST | `/config/cameras/{cameraConfigId}/versions` | Body：`CreateCameraConfigVersionRequest` | `CameraConfigVersionView` | 调用 `CameraConfigApplicationService.createVersion`。 |
| POST | `/config/cameras/{cameraConfigId}/validate` | Path：`cameraConfigId` | `ConfigValidationView` | 调用 `ConfigValidationService.validateCamera`。 |
| POST | `/config/cameras/{cameraConfigId}/activate` | Path：`cameraConfigId` | `ConfigActionView` | 调用 `CameraConfigApplicationService.activateCamera`。 |
| POST | `/config/cameras/{cameraConfigId}/disable` | Path：`cameraConfigId` | `ConfigActionView` | 调用 `CameraConfigApplicationService.disableCamera`。 |

`CreateCameraConfigRequest`

```json
{
  "cameraName": "string",
  "cameraType": "string",
  "fov": {},
  "parameters": {},
  "createdBy": "string"
}
```

`CreateCameraConfigVersionRequest`

```json
{
  "version": 1,
  "cameraType": "string",
  "fov": {},
  "parameters": {},
  "createdBy": "string"
}
```

### 7.3 Model Config

| 方法 | 地址 | 输入结构 | 返回结构 | 调用逻辑 |
| --- | --- | --- | --- | --- |
| POST | `/config/models` | Body：`CreateModelConfigRequest` | `ModelConfigVersionView` | 调用 `ModelConfigApplicationService.createModel`。 |
| GET | `/config/models` | 无 | `ModelConfigVersionView[]` | 调用 `ModelConfigApplicationService.listModels`。 |
| GET | `/config/models/{modelConfigId}` | Path：`modelConfigId` | `ModelConfigVersionView` | 调用 `ModelConfigApplicationService.getModel`。 |
| POST | `/config/models/{modelConfigId}/versions` | Body：`CreateModelConfigVersionRequest` | `ModelConfigVersionView` | 调用 `ModelConfigApplicationService.createVersion`。 |
| POST | `/config/models/{modelConfigId}/artifacts` | Body：`CreateModelArtifactRequest` | `ModelArtifactView` | 调用 `ModelConfigApplicationService.createArtifact` 登记模型制品。 |
| POST | `/config/models/{modelConfigId}/validate` | Path：`modelConfigId` | `ConfigValidationView` | 调用 `ConfigValidationService.validateModel`。 |
| POST | `/config/models/{modelConfigId}/activate` | Path：`modelConfigId` | `ConfigActionView` | 调用 `ModelConfigApplicationService.activateModel`。 |
| POST | `/config/models/{modelConfigId}/disable` | Path：`modelConfigId` | `ConfigActionView` | 调用 `ModelConfigApplicationService.disableModel`。 |

`CreateModelConfigRequest`

```json
{
  "modelName": "string",
  "modelType": "string",
  "runtimeType": "string",
  "parameters": {},
  "createdBy": "string"
}
```

`CreateModelConfigVersionRequest`

```json
{
  "version": 1,
  "modelType": "string",
  "runtimeType": "string",
  "parameters": {},
  "createdBy": "string"
}
```

`CreateModelArtifactRequest`

```json
{
  "artifactType": "string",
  "objectKey": "string",
  "checksum": "string",
  "metadata": {},
  "createdBy": "string"
}
```

`ModelArtifactView`

```json
{
  "artifactId": "string",
  "modelConfigId": "string",
  "artifactType": "string",
  "status": "string",
  "createdAt": "2026-05-21T00:00:00Z"
}
```

### 7.4 Transfer Config

| 方法 | 地址 | 输入结构 | 返回结构 | 调用逻辑 |
| --- | --- | --- | --- | --- |
| POST | `/config/transfers` | Body：`CreateTransferConfigRequest` | `TransferConfigVersionView` | 调用 `TransferConfigApplicationService.createTransfer`。 |
| GET | `/config/transfers` | 无 | `TransferConfigVersionView[]` | 调用 `TransferConfigApplicationService.listTransfers`。 |
| GET | `/config/transfers/{transferConfigId}` | Path：`transferConfigId` | `TransferConfigVersionView` | 调用 `TransferConfigApplicationService.getTransfer`。 |
| POST | `/config/transfers/{transferConfigId}/versions` | Body：`CreateTransferConfigVersionRequest` | `TransferConfigVersionView` | 调用 `TransferConfigApplicationService.createVersion`。 |
| POST | `/config/transfers/{transferConfigId}/validate` | Path：`transferConfigId` | `ConfigValidationView` | 调用 `ConfigValidationService.validateTransfer`。 |
| POST | `/config/transfers/{transferConfigId}/activate` | Path：`transferConfigId` | `ConfigActionView` | 调用 `TransferConfigApplicationService.activateTransfer`。 |
| POST | `/config/transfers/{transferConfigId}/disable` | Path：`transferConfigId` | `ConfigActionView` | 调用 `TransferConfigApplicationService.disableTransfer`。 |

`CreateTransferConfigRequest`

```json
{
  "transferName": "string",
  "transferType": "string",
  "endpoint": {},
  "parameters": {},
  "createdBy": "string"
}
```

`CreateTransferConfigVersionRequest`

```json
{
  "version": 1,
  "transferType": "string",
  "endpoint": {},
  "parameters": {},
  "createdBy": "string"
}
```

### 7.5 Config Validation

| 方法 | 地址 | 输入结构 | 返回结构 | 调用逻辑 |
| --- | --- | --- | --- | --- |
| GET | `/config/validations/{validationId}` | Path：`validationId` | `ConfigValidationView` | 调用 `ConfigValidationService.getValidation`。 |
| GET | `/config/validations?configType=&configId=` | Query：`configType`、`configId` 可选 | `ConfigValidationView[]` | 调用 `ConfigValidationService.listValidations(configType, configId)`。 |

## 8. Asset 与 Geo 模块 API

| 方法 | 地址 | 输入结构 | 返回结构 | 调用逻辑 |
| --- | --- | --- | --- | --- |
| POST | `/assets` | Body：`CreateAssetRequest` | 资产模块 `AssetView` | `AssetController` 构造 `CreateAssetInput`，调用 `AssetApplicationService.createAsset`，再由 `AssetQueryService.toView` 转响应。 |
| GET | `/assets?missionId=&taskId=&status=` | Query：`missionId`、`taskId`、`status` 可选 | 资产模块 `AssetView[]` | 调用 `AssetQueryService.listAssets`。 |
| GET | `/assets/{assetId}` | Path：`assetId` | 资产模块 `AssetView` | 调用 `AssetQueryService.getAsset`。 |
| POST | `/assets/{assetId}/preview` | Body 可空：`AssetActionRequest` | `AssetActionView` | 调用 `PreviewApplicationService.generatePreview`，返回 `GENERATE_PREVIEW` 已接收。 |
| POST | `/assets/{assetId}/geo-boundary` | Body 可空：`AssetActionRequest` | `AssetActionView` | 调用 `GeoBoundaryApplicationService.calculateBoundary`，返回 `CALCULATE_GEO_BOUNDARY` 已接收。 |
| POST | `/assets/{assetId}/publish-layer` | Body 可空：`AssetActionRequest` | `AssetActionView` | 调用 `LayerPublishApplicationService.publishLayer`；若返回 `PENDING_APPROVAL`，响应状态也为 `PENDING_APPROVAL`，否则为 `ACCEPTED`。 |
| DELETE | `/assets/{assetId}` | Path：`assetId` | `AssetActionView` | 当前仅调用 `AssetApplicationService.findRequired` 校验资产存在，返回 `DELETE/PENDING_APPROVAL`，实际删除应走审批链路。 |
| GET | `/assets/stats` | 无 | `AssetStatsView` | 调用 `AssetQueryService.getStats`。 |
| GET | `/assets/{assetId}/layers` | Path：`assetId` | `AssetLayerView[]` | `AssetLayerController` 调用 `AssetQueryService.listLayers`。 |
| GET | `/assets/{assetId}/layers/{layerId}` | Path：`assetId`、`layerId` | `AssetLayerView` | 调用 `AssetQueryService.getLayer`。 |

`CreateAssetRequest`

```json
{
  "missionId": "string",
  "taskId": "string",
  "assetType": "string",
  "role": "string",
  "name": "string",
  "objectKey": "string",
  "contentType": "string",
  "sizeBytes": 0,
  "checksum": "string",
  "metadata": {},
  "createdBy": "string"
}
```

`AssetActionRequest`

```json
{
  "requestedBy": "string",
  "reason": "string",
  "parameters": {}
}
```

`AssetActionView`

```json
{
  "assetId": "string",
  "action": "GENERATE_PREVIEW|CALCULATE_GEO_BOUNDARY|PUBLISH_LAYER|DELETE",
  "status": "ACCEPTED|PENDING_APPROVAL",
  "acceptedAt": "2026-05-21T00:00:00Z"
}
```

`AssetLayerView`

```json
{
  "assetId": "string",
  "layerId": "string",
  "layerUrl": "string",
  "status": "string",
  "updatedAt": "2026-05-21T00:00:00Z"
}
```

`AssetStatsView`

```json
{
  "totalCount": 0,
  "imageCount": 0,
  "videoCount": 0,
  "modelResultCount": 0,
  "reportCount": 0,
  "attachmentCount": 0
}
```

## 9. Telemetry 模块 API

| 方法 | 地址 | 输入结构 | 返回结构 | 调用逻辑 |
| --- | --- | --- | --- | --- |
| POST | `/uavs/{uavId}/telemetry` | Path：`uavId`；Body：`IngestTelemetryRequest` | `UavTelemetryView` | `UavTelemetryController` 构造 `IngestTelemetryCommand`，调用 `UavTelemetryIngestService.ingest` 写入遥测。 |
| GET | `/uavs/{uavId}/telemetry/latest` | Path：`uavId` | `UavTelemetryView` | 调用 `UavTelemetryQueryService.latest(uavId)`。 |
| GET | `/uavs/{uavId}/telemetry?from=&to=&limit=` | Query：`from`、`to`、`limit` 可选 | `UavTelemetryView[]` | 调用 `UavTelemetryQueryService.range(uavId, from, to, limit)`。 |
| GET | `/uavs/{uavId}/track?missionId=&limit=` | Query：`missionId`、`limit` 可选 | `UavTrackView` | 调用 `UavTelemetryQueryService.track(uavId, missionId, limit)`，包装成轨迹点列表。 |

`IngestTelemetryRequest`

```json
{
  "missionId": "string",
  "taskId": "string",
  "latitude": 0,
  "longitude": 0,
  "altitudeMeters": 0,
  "speedMetersPerSecond": 0,
  "headingDegrees": 0,
  "reportedAt": "2026-05-21T00:00:00Z",
  "rawPayload": {}
}
```

`UavTelemetryView`

```json
{
  "telemetryId": "string",
  "uavId": "string",
  "missionId": "string",
  "taskId": "string",
  "latitude": 0,
  "longitude": 0,
  "altitudeMeters": 0,
  "speedMetersPerSecond": 0,
  "headingDegrees": 0,
  "status": "RECORDED",
  "reportedAt": "2026-05-21T00:00:00Z"
}
```

`UavTrackView`

```json
{
  "uavId": "string",
  "missionId": "string",
  "points": []
}
```

## 10. Realtime 模块 API

以下接口返回 `text/event-stream`，不使用统一 `ApiResult` 包装。

| 方法 | 地址 | 输入结构 | 返回结构 | 调用逻辑 |
| --- | --- | --- | --- | --- |
| GET | `/realtime/missions/{missionId}/events` | Path：`missionId` | SSE stream | 调用 `RealtimePushService.subscribe("task-events", missionId)`。 |
| GET | `/realtime/tasks/{taskId}/events` | Path：`taskId` | SSE stream | 调用 `RealtimePushService.subscribe("task-events", taskId)`。 |
| GET | `/realtime/uavs/{uavId}/telemetry` | Path：`uavId` | SSE stream | 调用 `RealtimePushService.subscribe("uav-telemetry", uavId)`。 |
| GET | `/realtime/agent/sessions/{sessionId}/events` | Path：`sessionId` | SSE stream | 调用 `RealtimePushService.subscribe("agent-events", sessionId)`。 |

## 11. Agent 模块 API

### 11.1 Agent Session

| 方法 | 地址 | 输入结构 | 返回结构 | 调用逻辑 |
| --- | --- | --- | --- | --- |
| POST | `/agent/sessions` | Body：`CreateAgentSessionRequest` | `AgentSessionView` | `AgentController` 构造 `CreateAgentSessionCommand`，调用 `AgentApplicationService.createSession`；只建立上下文，不直接执行工具。需要 `AGENT_CHAT`。 |
| GET | `/agent/sessions/{sessionId}` | Path：`sessionId` | `AgentSessionView` | 调用 `AgentApplicationService.findSession`。需要 `AGENT_CHAT`。 |
| POST | `/agent/sessions/{sessionId}/messages` | Body：`SendAgentMessageRequest` | `AgentConversationView` | 调用 `AgentApplicationService.sendMessageAndReply`，完成用户消息、模型回复、工具调用计划和审计。需要 `AGENT_CHAT`。 |
| GET | `/agent/sessions/{sessionId}/messages` | Path：`sessionId` | `AgentMessageView[]` | 调用 `AgentApplicationService.listMessages`。需要 `AGENT_CHAT`。 |
| GET | `/agent/sessions/{sessionId}/events` | Path：`sessionId` | `AgentEventView[]` | 调用 `AgentApplicationService.listSessionEvents`，实时流走 `/realtime/agent/sessions/{sessionId}/events`。需要 `AGENT_CHAT`。 |

`CreateAgentSessionRequest`

```json
{
  "missionId": "string",
  "taskId": "string",
  "createdBy": "string",
  "title": "string",
  "context": {}
}
```

`SendAgentMessageRequest`

```json
{
  "role": "USER",
  "content": "string",
  "attachments": {},
  "createdBy": "string"
}
```

`AgentSessionView`

```json
{
  "sessionId": "string",
  "missionId": "string",
  "taskId": "string",
  "userId": "string",
  "title": "string",
  "status": "string",
  "createdAt": "2026-05-21T00:00:00Z",
  "updatedAt": "2026-05-21T00:00:00Z"
}
```

`AgentMessageView`

```json
{
  "messageId": "string",
  "sessionId": "string",
  "role": "SYSTEM|USER|ASSISTANT|TOOL",
  "content": "string",
  "status": "SAVED",
  "metadata": {},
  "createdAt": "2026-05-21T00:00:00Z"
}
```

`AgentConversationView`

```json
{
  "userMessage": {},
  "assistantMessage": {},
  "toolCalls": []
}
```

`AgentEventView`

```json
{
  "eventId": "string",
  "sessionId": "string",
  "eventType": "string",
  "payload": {},
  "occurredAt": "2026-05-21T00:00:00Z"
}
```

### 11.2 Agent Tool Gateway

| 方法 | 地址 | 输入结构 | 返回结构 | 调用逻辑 |
| --- | --- | --- | --- | --- |
| POST | `/agent/tool-calls` | Body：`InvokeAgentToolRequest` | `AgentToolCallView` | `AgentToolController` 构造 `InvokeAgentToolCommand`，调用 `ToolGatewayService.invoke`；权限、风险、审批、审计由 Tool Gateway 统一处理。需要 `AGENT_CHAT`。 |
| GET | `/agent/tool-calls/{toolCallId}` | Path：`toolCallId` | `AgentToolCallView` | 调用 `ToolGatewayService.getToolCall` 查询审计记录。需要 `AGENT_CHAT` 或 `AGENT_TOOL_APPROVE`。 |
| POST | `/agent/tool-calls/{toolCallId}/approve` | Body 可空：`{"reviewer":"string","reason":"string"}` | `AgentToolCallView` | 调用 `ToolGatewayService.approve`；审批通过后由 Tool Gateway 继续执行原工具调用。需要 `AGENT_TOOL_APPROVE`。 |
| POST | `/agent/tool-calls/{toolCallId}/reject` | Body 可空：`{"reviewer":"string","reason":"string"}` | `AgentToolCallView` | 调用 `ToolGatewayService.reject`，终止工具调用。需要 `AGENT_TOOL_APPROVE`。 |

`InvokeAgentToolRequest`

```json
{
  "sessionId": "string",
  "messageId": "string",
  "userId": "string",
  "toolName": "string",
  "input": {},
  "permissions": ["string"]
}
```

`AgentToolCallView`

```json
{
  "toolCallId": "string",
  "sessionId": "string",
  "toolName": "string",
  "riskLevel": "LOW|MEDIUM|HIGH|CRITICAL",
  "approvalRequired": true,
  "requestedBy": "string",
  "approvedBy": "string",
  "approvedAt": "2026-05-21T00:00:00Z",
  "status": "CREATED|WAITING_APPROVAL|RUNNING|COMPLETED|REJECTED|BLOCKED|FAILED",
  "errorMessage": "string",
  "input": {},
  "output": {},
  "createdAt": "2026-05-21T00:00:00Z",
  "updatedAt": "2026-05-21T00:00:00Z"
}
```

## 12. Event 诊断 API

| 方法 | 地址 | 输入结构 | 返回结构 | 调用逻辑 |
| --- | --- | --- | --- | --- |
| GET | `/events/outbox/pending?limit=50` | Query：`limit` 默认 50 | `EventEnvelope[]` | `EventDiagnosticsController` 调用 `OutboxRepository.findByStatus(PENDING, limit)`。 |
| GET | `/events/outbox/failed?limit=50` | Query：`limit` 默认 50 | `EventEnvelope[]` | 调用 `OutboxRepository.findByStatus(FAILED, limit)`。 |
| GET | `/events/dead-letters?limit=50` | Query：`limit` 默认 50 | `EventEnvelope[]` | 调用 `DeadLetterEventService.listDeadLetters(limit)`。 |
| POST | `/events/outbox/publish?limit=50` | Query：`limit` 默认 50 | `OutboxPublishResult[]` | 调用 `OutboxPublishService.publishPending(limit)` 发布待发送 outbox。 |
| POST | `/events/outbox/retry-failed?limit=50` | Query：`limit` 默认 50 | `OutboxPublishResult[]` | 调用 `OutboxPublishService.retryFailed(limit)` 重试失败 outbox。 |
| POST | `/events/replay/{aggregateType}/{aggregateId}` | Path：`aggregateType`、`aggregateId` | `EventEnvelope[]` | 调用 `EventReplayService.replayAggregate` 回放指定聚合事件。 |

`EventEnvelope`

```json
{
  "eventId": "string",
  "aggregateType": "string",
  "aggregateId": "string",
  "eventType": "TASK_EVENT|DEVICE_COMMAND|ASSET_EVENT|UAV_TELEMETRY|AGENT_EVENT|DEAD_LETTER",
  "topic": "string",
  "messageKey": "string",
  "payload": {},
  "headers": {},
  "status": "PENDING|PUBLISHED|FAILED",
  "createdAt": "2026-05-21T00:00:00Z",
  "publishedAt": "2026-05-21T00:00:00Z",
  "retryCount": 0,
  "lastError": "string"
}
```

`OutboxPublishResult`

```json
{
  "eventId": "string",
  "success": true,
  "message": "string"
}
```

## 13. 模块调用边界汇总

| 模块 | API 入口 | 主要应用服务/端口 | 调用边界 |
| --- | --- | --- | --- |
| 认证 | `AuthController` | `AuthenticationManager`、`JwtTokenService` | 负责登录校验与 Token 签发。 |
| Mission | `MissionController` | `MissionApplicationService`、`MissionQueryService` | 创建和查询 Mission，不直接操作 Task 或设备。 |
| Task | `TaskController`、`TaskAttemptController`、`TaskEventController` | `TaskApplicationService`、`TaskQueryService` | 管理 Task 生命周期、查询尝试和事件。 |
| TaskCommand/审批 | `TaskCommandController`、`TaskApprovalController` | `TaskCommandApplicationService`、`TaskApprovalService` | 所有高风险设备/资产/发布动作进入命令与审批链路。 |
| Config Center | 各 `Config*Controller` | `*ConfigApplicationService`、`ConfigValidationService` | 设备、相机、模型、传输配置版本化管理和校验。 |
| Asset/Geo | `AssetController`、`AssetLayerController` | `AssetApplicationService`、`AssetQueryService`、`PreviewApplicationService`、`GeoBoundaryApplicationService`、`LayerPublishApplicationService` | 资产登记、预览、坐标边界、图层发布；真实 MinIO/GeoServer 取决于 profile。 |
| Telemetry | `UavTelemetryController` | `UavTelemetryIngestService`、`UavTelemetryQueryService` | 遥测写入、最新点、时间范围和轨迹查询。 |
| Realtime | `RealtimeController` | `RealtimePushService` | SSE 订阅任务、无人机遥测和 Agent 事件。 |
| Agent | `AgentController`、`AgentToolController` | `AgentApplicationService`、`ToolGatewayService` | Agent 会话、消息编排、工具调用、审批和审计；写操作只能通过 Tool Gateway。 |
| Event | `EventDiagnosticsController` | `OutboxRepository`、`OutboxPublishService`、`EventReplayService`、`DeadLetterEventService` | outbox 诊断、发布、重试、死信和事件回放。 |

## 14. 当前文档生成依据

本文档根据以下源码生成：

- `src/main/java/com/zhongyan/uav/**/api/*Controller.java`
- `src/main/java/com/zhongyan/uav/**/api/request/*.java`
- `src/main/java/com/zhongyan/uav/**/api/response/*.java`
- `src/main/java/com/zhongyan/uav/common/response/*.java`
- `src/main/resources/application*.yml`

未包含 Spring Boot Actuator 的 `/actuator/*` 运维端点；它们由 `management.endpoints.web.exposure.include=health,info,metrics,prometheus` 控制，不属于业务 `/v2` API。
