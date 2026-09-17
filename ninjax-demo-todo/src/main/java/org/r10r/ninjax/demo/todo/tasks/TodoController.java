package org.r10r.ninjax.demo.todo.tasks;

import org.r10r.ninjax.core.Request;
import org.r10r.ninjax.core.Result;
import org.r10r.ninjax.demo.todo.tasks.views.TodoTemplateService;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.r10r.ninjax.json.Json;

public class TodoController {

    private static final Logger logger = Logger.getLogger(TodoController.class.getName());

    private final TaskService taskService;
    private final TodoTemplateService templateService;
    private final Json json;

    public TodoController(TaskService taskService, Json json) {
        this.taskService = taskService;
        this.templateService = new TodoTemplateService();
        this.json = json;
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
            return internalServerError("Error showing tasks", e);
        }
    }

    public Result addTask(Request request) {
        try {
            String title = request.getParameters().get("title").orElse("");
            
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
            return internalServerError("Error adding task", e);
        }
    }

    public Result deleteTask(Request request) {
        try {
            String idStr = request.getParameters().get("id").orElse("");
            
            long id = Long.parseLong(idStr);
            taskService.delete(id);
            
            return Result.builder()
                    .redirect("/")
                    .build();
        } catch (Exception e) {
            return internalServerError("Error deleting task", e);
        }
    }

    public Result getTasksJson(Request request) {
        try {
            List<Task> tasks = taskService.findAny();
            
            return Result.builder()
                    .status(Result.SC_200_OK)
                    .json(json.json(tasks))
                    .build();
        } catch (Exception e) {
            return internalServerError("Error getting tasks", e);
        }
    }

    public Result toggleTaskCompletion(Request request) {
        try {
            String idStr = request.getParameters().get("id").orElse("");
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
            return internalServerError("Error toggling task", e);
        }
    }

    /**
     * Logs the details and answers with a generic message. Exception messages can contain internals
     * (SQL, table names, file paths) and must not be sent to the client.
     */
    private static Result internalServerError(String whatFailed, Exception e) {
        logger.log(Level.SEVERE, whatFailed, e);
        return Result.builder()
                .status(Result.SC_500_INTERNAL_SERVER_ERROR)
                .text(whatFailed + ". Please try again later.")
                .build();
    }
}
