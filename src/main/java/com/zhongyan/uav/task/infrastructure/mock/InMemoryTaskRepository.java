package com.zhongyan.uav.task.infrastructure.mock;

import com.zhongyan.uav.task.domain.Task;
import com.zhongyan.uav.task.domain.TaskRepository;
import com.zhongyan.uav.task.domain.TaskStatus;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class InMemoryTaskRepository implements TaskRepository {
    private final Map<String, Task> tasks = new ConcurrentHashMap<>();

    /**
     * 将 Task 保存到内存 Map。
     */
    @Override
    public Task save(Task task) {
        tasks.put(task.taskId(), task);
        return task;
    }

    /**
     * 从内存中按 Task 编号查找聚合。
     */
    @Override
    public Optional<Task> findById(String taskId) {
        return Optional.ofNullable(tasks.get(taskId));
    }

    /**
     * 从内存中查询某个 Mission 下的全部 Task。
     */
    @Override
    public List<Task> findByMissionId(String missionId) {
        return tasks.values().stream()
                .filter(task -> task.missionId().equals(missionId))
                .sorted(Comparator.comparing(Task::createdAt))
                .collect(Collectors.toList());
    }

    /**
     * 从内存中按 Task 状态筛选任务。
     */
    @Override
    public List<Task> findByStatus(TaskStatus status) {
        return tasks.values().stream()
                .filter(task -> task.status() == status)
                .sorted(Comparator.comparing(Task::createdAt))
                .collect(Collectors.toList());
    }

    /**
     * 判断内存仓储中是否存在指定 Task。
     */
    @Override
    public boolean existsById(String taskId) {
        return tasks.containsKey(taskId);
    }
}
