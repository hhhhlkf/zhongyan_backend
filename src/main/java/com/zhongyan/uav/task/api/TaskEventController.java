package com.zhongyan.uav.task.api;

import com.zhongyan.uav.common.response.ResponseResult;
import com.zhongyan.uav.task.api.response.TaskEventView;
import com.zhongyan.uav.task.application.TaskQueryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@ResponseResult
@RestController
@RequestMapping("/tasks/{taskId}/events")
public class TaskEventController {
    private final TaskQueryService taskQueryService;

    /**
     * 创建 Task 事件 API 控制器。
     */
    public TaskEventController(TaskQueryService taskQueryService) {
        this.taskQueryService = taskQueryService;
    }

    /**
     * 查询 Task 的事件时间线。
     */
    @GetMapping
    public List<TaskEventView> listEvents(@PathVariable String taskId) {
        return taskQueryService.listEvents(taskId).stream()
                .map(TaskEventView::from)
                .collect(Collectors.toList());
    }
}
