package com.zhongyan.uav.task.api;

import com.zhongyan.uav.common.response.ResponseResult;
import com.zhongyan.uav.task.api.response.TaskAttemptView;
import com.zhongyan.uav.task.application.TaskQueryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@ResponseResult
@RestController
@RequestMapping("/tasks/{taskId}/attempts")
public class TaskAttemptController {
    private final TaskQueryService taskQueryService;

    public TaskAttemptController(TaskQueryService taskQueryService) {
        this.taskQueryService = taskQueryService;
    }

    @GetMapping
    public List<TaskAttemptView> listAttempts(@PathVariable String taskId) {
        return taskQueryService.listAttempts(taskId).stream()
                .map(TaskAttemptView::from)
                .collect(Collectors.toList());
    }
}
