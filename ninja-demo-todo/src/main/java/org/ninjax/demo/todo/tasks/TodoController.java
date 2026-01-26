package org.ninjax.demo.todo.tasks;

import org.ninjax.core.Request;
import org.ninjax.core.Result;
import org.ninjax.demo.todo.tasks.views.TodoTemplateService;

import java.util.List;

public class TodoController {

    private final TaskService taskService;
    private final TodoTemplateService templateService;

    public TodoController(TaskService taskService) {
        this.taskService = taskService;
        this.templateService = new TodoTemplateService();
    }

    public Result showTasks(Request request) {
        try {
            List<Task> tasks = taskService.findAny();
            
            String html = templateService.generateTodoPage(tasks);
            
            return Result.builder()
                    .status(Result.SC_200_OK)
                    .html(html)
                    .build();
        } catch (Exception e) {
            return Result.builder()
                    .status(Result.SC_500_INTERNAL_SERVER_ERROR)
                    .text("Error: " + e.getMessage())
                    .build();
        }
    }

    public Result addTask(Request request) {
        try {
            String title = request.parameters().get("title").orElse("");
            
            if (title.trim().isEmpty()) {
                return Result.builder()
                        .badRequest()
                        .text("Title cannot be empty")
                        .build();
            }
            
            Task task = new Task(null, title.trim(), "", java.time.LocalDateTime.now(), false);
            taskService.create(task);
            
            return Result.builder()
                    .redirect("/")
                    .build();
        } catch (Exception e) {
            return Result.builder()
                    .status(Result.SC_500_INTERNAL_SERVER_ERROR)
                    .text("Error adding task: " + e.getMessage())
                    .build();
        }
    }

    public Result deleteTask(Request request) {
        try {
            String idStr = request.parameters().get("id").orElse("");
            
            long id = Long.parseLong(idStr);
            taskService.delete(id);
            
            return Result.builder()
                    .redirect("/")
                    .build();
        } catch (Exception e) {
            return Result.builder()
                    .status(Result.SC_500_INTERNAL_SERVER_ERROR)
                    .text("Error deleting task: " + e.getMessage())
                    .build();
        }
    }

    public Result getTasksJson(Request request) {
        try {
            List<Task> tasks = taskService.findAny();
            
            return Result.builder()
                    .status(Result.SC_200_OK)
                    .json(tasks)
                    .build();
        } catch (Exception e) {
            return Result.builder()
                    .status(Result.SC_500_INTERNAL_SERVER_ERROR)
                    .text("Error getting tasks: " + e.getMessage())
                    .build();
        }
    }

    public Result toggleTaskCompletion(Request request) {
        try {
            String idStr = request.parameters().get("id").orElse("");
            long id = Long.parseLong(idStr);
            boolean success = taskService.toggleCompleted(id);
            
            if (success) {
                return Result.builder()
                    .redirect("/")
                    .build();
            } else {
                return Result.builder()
                    .notFound()
                    .text("Task not found")
                    .build();
            }
        } catch (Exception e) {
            return Result.builder()
                .status(Result.SC_500_INTERNAL_SERVER_ERROR)
                .text("Error toggling task: " + e.getMessage())
                .build();
        }
    }
}