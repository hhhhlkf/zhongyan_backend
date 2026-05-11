-- 任务场景、任务、资产主链路表。
COMMENT ON TABLE mission IS '任务场景聚合表。表示一次应急、演示、训练或测试流程，用于归集相关任务和资产。';
COMMENT ON TABLE task IS '任务聚合表。表示系统中最小可执行业务单元，例如采集、处理、传输、预览、图层发布、智能体分析或报告生成。';
COMMENT ON TABLE task_attempt IS '任务执行尝试表。记录任务的每一次可重试执行尝试，包括执行节点、执行结果、错误信息和原始日志对象键。';
COMMENT ON TABLE task_command IS '任务命令审计表。记录任务或设备动作命令，高风险动作在下发前必须进入审批流程。';
COMMENT ON TABLE task_event IS '任务事件时间线表。记录任务生命周期状态变化、命令事件、进度变化、失败、重试、取消和超时等审计事件。';
COMMENT ON TABLE task_dependency IS '任务依赖关系表。声明一个任务在执行前依赖另一个任务完成。';
COMMENT ON TABLE task_resource_lease IS '运行时资源租约表。用于避免多个任务同时占用同一设备或同一业务资源。';
COMMENT ON TABLE asset IS '资产元数据表。保存图片、视频、模型结果、日志、报告、附件、几何数据和已发布图层的数据库元信息；对象文件本体存放在对象存储中。';
COMMENT ON TABLE task_asset IS '任务资产绑定关系表。记录某个资产作为任务的输入、输出、日志、预览、报告或附件被引用。';

-- 配置中心表。
COMMENT ON TABLE device_config IS '设备配置注册表。保存设备身份、连接信息、能力描述和配置生命周期状态。';
COMMENT ON TABLE camera_config IS '相机版本化配置表。按版本保存相机类型、视场角元数据和相机运行参数。';
COMMENT ON TABLE model_config IS '模型版本化配置表。按版本保存模型类型、运行时类型和执行参数。';
COMMENT ON TABLE transfer_config IS '传输版本化配置表。按版本保存传输端点和传输运行参数。';
COMMENT ON TABLE model_artifact IS '模型制品元数据表。保存模型权重、模型包或相关制品在对象存储中的引用和校验信息。';
COMMENT ON TABLE config_validation IS '配置校验结果表。记录设备、相机、模型或传输配置的校验状态和错误详情。';

-- 遥测、事务事件箱和智能体表。
COMMENT ON TABLE uav_telemetry IS '无人机遥测时序表。保存无人机最新和历史的位置、姿态、速度、电量以及原始遥测载荷。';
COMMENT ON TABLE event_outbox IS '事务事件箱表。保存待发布到消息总线或供实时消费者回放的领域事件。';
COMMENT ON TABLE agent_session IS '智能体会话表。归集一次面向用户的智能体工作流中的消息和工具调用。';
COMMENT ON TABLE agent_message IS '智能体消息表。保存智能体会话中的用户、助手、系统或工具消息。';
COMMENT ON TABLE agent_tool_call IS '智能体工具调用审计表。保存工具调用载荷、风险等级、审批状态和执行结果。';
