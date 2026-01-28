package org.ninjax.demo.todo;

import org.ninjax.demo.todo.tasks.TaskService;
import org.ninjax.demo.todo.tasks.TodoController;
import org.ninjax.demo.todo.tasks.TaskRepository;
import java.util.Optional;
import org.ninjax.core.NinjaJetty;
import org.ninjax.core.Router;
import org.ninjax.core.properties.NinjaProperties;
import org.ninjax.db.jdbi.NinjaJdbiImpl;
import org.ninjax.db.hikari.NinjaDbHikariProvider;
import org.ninjax.db.flyway.NinjaFlywayMigrator;
import org.ninjax.db.jdbc.NinjaDatasourcePropertiesExtractor;
import org.ninjax.json.Json;

public class TodoApplication {

    public TodoApplication(MockableComponents mockableComponents) {
        // Ninja properties
        var ninjaProperties = new NinjaProperties();

        // DB configuration
        var ninjaDatasourceConfigProvider = new NinjaDatasourcePropertiesExtractor(ninjaProperties);
        var ninjaFlywayMigrator = new NinjaFlywayMigrator(ninjaDatasourceConfigProvider.get());
        var ninjaDbHikariProvider = new NinjaDbHikariProvider(ninjaDatasourceConfigProvider.get());
        var ninjaJdbiImpl = new NinjaJdbiImpl(ninjaDbHikariProvider.get());

        // App wiring
        var json = new Json();
        var taskRepository = new TaskRepository(ninjaJdbiImpl);
        var taskService = mockableComponents.taskService.orElseGet(() -> new TaskService(taskRepository));
        var todoController = new TodoController(taskService, json);

        var router = new Router();
        router.GET("/").with(todoController::showTasks);
        router.POST("/tasks").with(todoController::addTask);
        router.POST("/tasks/delete").with(todoController::deleteTask);
        router.POST("/tasks/toggle").with(todoController::toggleTaskCompletion);
        router.GET("/tasks.json").with(todoController::getTasksJson);

        // NinjaJetty startup
        try {
            new NinjaJetty(router, ninjaProperties);
        } catch (Exception e) {
            throw new RuntimeException("Failed to start NinjaJetty", e);
        }
    }
    
    public TodoApplication() {
        this(MockableComponents.nothingMocked());
    }

    public static void main(String[] args) {
        new TodoApplication();
    }
    

    // For testing only
    public record MockableComponents(Optional<TaskService> taskService) {
        public static MockableComponents nothingMocked() {
            return new MockableComponents(Optional.empty());
        }
    }
}
