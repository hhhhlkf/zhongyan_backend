package com.zhongyan.uav.task.infrastructure.mock;

import com.zhongyan.uav.task.domain.TaskAttempt;
import com.zhongyan.uav.task.domain.TaskAttemptRepository;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class InMemoryTaskAttemptRepository implements TaskAttemptRepository {
    private final Map<String, TaskAttempt> attempts = new ConcurrentHashMap<>();

    /**
     * 将 TaskAttempt 保存到内存 Map。
     */
    @Override
    public TaskAttempt save(TaskAttempt attempt) {
        attempts.put(attempt.attemptId(), attempt);
        return attempt;
    }

    /**
     * 从内存中按 Attempt 编号查找执行尝试。
     */
    @Override
    public Optional<TaskAttempt> findById(String attemptId) {
        return Optional.ofNullable(attempts.get(attemptId));
    }

    /**
     * 从内存中查询某个 Task 的全部执行尝试，并按尝试次数排序。
     */
    @Override
    public List<TaskAttempt> findByTaskId(String taskId) {
        return attempts.values().stream()
                .filter(attempt -> attempt.taskId().equals(taskId))
                .sorted(Comparator.comparingInt(TaskAttempt::attemptNo))
                .collect(Collectors.toList());
    }
}
