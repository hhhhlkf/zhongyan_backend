package com.zhongyan.uav.task.api;

import com.zhongyan.uav.common.response.ResponseResult;
import com.zhongyan.uav.task.api.request.CreateTaskCommandRequest;
import com.zhongyan.uav.task.api.response.TaskCommandView;
import com.zhongyan.uav.task.application.TaskCommandApplicationService;
import com.zhongyan.uav.task.application.TaskQueryService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@ResponseResult
@RestController
@RequestMapping("/tasks/{taskId}/commands")
public class TaskCommandController {
    private final TaskCommandApplicationService taskCommandApplicationService;
    private final TaskQueryService taskQueryService;

    /**
     * 创建 Task 命令 API 控制器。
     */
    public TaskCommandController(TaskCommandApplicationService taskCommandApplicationService,
                                 TaskQueryService taskQueryService) {
        this.taskCommandApplicationService = taskCommandApplicationService;
        this.taskQueryService = taskQueryService;
    }

    /**
     * 创建 TaskCommand。
     */
    @PostMapping
    @PreAuthorize("hasAnyAuthority('WRITE', 'TASK_APPROVE', 'ROLE_ADMIN')")
    public TaskCommandView createCommand(@PathVariable String taskId,
                                         @RequestBody CreateTaskCommandRequest request) {
        return TaskCommandView.from(taskCommandApplicationService.createCommand(taskId, request.toInput()));
    }

    /**
     * 查询 Task 下的全部命令。
     */
    @GetMapping
    public List<TaskCommandView> listCommands(@PathVariable String taskId) {
        return taskQueryService.listCommands(taskId).stream()
                .map(TaskCommandView::from)
                .collect(Collectors.toList());
    }
}
