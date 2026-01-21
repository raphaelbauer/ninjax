package org.ninjax.demo.todo.tasks;

import java.util.List;

public class TaskService {

    private final TaskRepository taskRepository;

    public TaskService(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    public List<Task> findAny() {
        return taskRepository.findAny();
    }

    public Task create(Task task) {
        var id = taskRepository.insert(task.title(), task.description(), task.createdAt(), task.completed());
        return task.withId(id);
    }

    public boolean delete(long id) {
        return taskRepository.deleteById(id) > 0;
    }

    public boolean toggleCompleted(long id) {
        java.util.Optional<Task> task = taskRepository.findById(id);
        if (task.isPresent()) {
            Task currentTask = task.get();
            return taskRepository.updateCompleted(!currentTask.completed(), id) > 0;
        }
        return false;
    }
}
