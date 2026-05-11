package com.zhongyan.uav.task.api;

import com.zhongyan.uav.common.response.ResponseResult;
import com.zhongyan.uav.task.api.request.CancelTaskRequest;
import com.zhongyan.uav.task.api.request.CreateTaskRequest;
import com.zhongyan.uav.task.api.request.RetryTaskRequest;
import com.zhongyan.uav.task.api.request.SubmitTaskRequest;
import com.zhongyan.uav.task.api.response.AssetView;
import com.zhongyan.uav.task.api.response.TaskView;
import com.zhongyan.uav.task.application.TaskApplicationService;
import com.zhongyan.uav.task.application.TaskQueryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@ResponseResult
@RestController
public class TaskController {
    private final TaskApplicationService taskApplicationService;
    private final TaskQueryService taskQueryService;

    /**
     * 创建 Task API 控制器。
     */
    public TaskController(TaskApplicationService taskApplicationService,
                          TaskQueryService taskQueryService) {
        this.taskApplicationService = taskApplicationService;
        this.taskQueryService = taskQueryService;
    }

    /**
     * 在 Mission 下创建 Task 草稿。
     */
    @PostMapping("/missions/{missionId}/tasks")
    public TaskView createTask(@PathVariable String missionId,
                               @RequestBody CreateTaskRequest request) {
        return TaskView.from(taskApplicationService.createTask(request.toInput(missionId)));
    }

    /**
     * 查询 Mission 下的 Task 列表。
     */
    @GetMapping("/missions/{missionId}/tasks")
    public List<TaskView> listTasksByMission(@PathVariable String missionId) {
        return taskQueryService.listTasksByMission(missionId).stream()
                .map(TaskView::from)
                .collect(Collectors.toList());
    }

    /**
     * 查询 Task 详情。
     */
    @GetMapping("/tasks/{taskId}")
    public TaskView getTask(@PathVariable String taskId) {
        return TaskView.from(taskQueryService.getTask(taskId));
    }

    /**
     * 提交 Task，使其进入排队状态。
     */
    @PostMapping("/tasks/{taskId}/submit")
    public TaskView submitTask(@PathVariable String taskId,
                               @RequestBody(required = false) SubmitTaskRequest request) {
        String submittedBy = request == null ? "mock-user" : request.submittedByOrDefault();
        return TaskView.from(taskApplicationService.submitTask(taskId, submittedBy));
    }

    @PostMapping("/tasks/{taskId}/retry")
    public TaskView retryTask(@PathVariable String taskId,
                              @RequestBody(required = false) RetryTaskRequest request) {
        String requestedBy = request == null ? "mock-user" : request.requestedByOrDefault();
        return TaskView.from(taskApplicationService.retryTask(taskId, requestedBy));
    }

    @PostMapping("/tasks/{taskId}/cancel")
    public TaskView cancelTask(@PathVariable String taskId,
                               @RequestBody(required = false) CancelTaskRequest request) {
        String cancelledBy = request == null ? "mock-user" : request.cancelledByOrDefault();
        return TaskView.from(taskApplicationService.cancelTask(taskId, cancelledBy));
    }

    /**
     * 查询 Task 关联资产。
     */
    @GetMapping("/tasks/{taskId}/assets")
    public List<AssetView> listAssets(@PathVariable String taskId) {
        return taskQueryService.listAssets(taskId).stream()
                .map(AssetView::from)
                .collect(Collectors.toList());
    }
}
