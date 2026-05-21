package com.zhongyan.uav.task.api;

import com.zhongyan.uav.common.response.ResponseResult;
import com.zhongyan.uav.task.api.request.ApproveCommandRequest;
import com.zhongyan.uav.task.api.response.TaskCommandView;
import com.zhongyan.uav.task.application.TaskApprovalService;
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
@RequestMapping("/approvals")
@PreAuthorize("hasAnyAuthority('TASK_APPROVE', 'ROLE_ADMIN')")
public class TaskApprovalController {
    private final TaskApprovalService taskApprovalService;

    /**
     * 创建 Task 审批 API 控制器。
     */
    public TaskApprovalController(TaskApprovalService taskApprovalService) {
        this.taskApprovalService = taskApprovalService;
    }

    /**
     * 查询待审批命令列表。
     */
    @GetMapping("/pending")
    public List<TaskCommandView> listPendingApprovals() {
        return taskApprovalService.listPendingApprovals().stream()
                .map(TaskCommandView::from)
                .collect(Collectors.toList());
    }

    /**
     * 查询审批命令详情。
     */
    @GetMapping("/{commandId}")
    public TaskCommandView getCommand(@PathVariable String commandId) {
        return TaskCommandView.from(taskApprovalService.getCommand(commandId));
    }

    /**
     * 审批通过命令。
     */
    @PostMapping("/{commandId}/approve")
    public TaskCommandView approveCommand(@PathVariable String commandId,
                                          @RequestBody(required = false) ApproveCommandRequest request) {
        String approver = request == null ? "mock-approver" : request.approverOrDefault();
        return TaskCommandView.from(taskApprovalService.approveCommand(commandId, approver));
    }

    /**
     * 拒绝命令。
     */
    @PostMapping("/{commandId}/reject")
    public TaskCommandView rejectCommand(@PathVariable String commandId,
                                         @RequestBody(required = false) ApproveCommandRequest request) {
        String approver = request == null ? "mock-approver" : request.approverOrDefault();
        return TaskCommandView.from(taskApprovalService.rejectCommand(commandId, approver));
    }
}
