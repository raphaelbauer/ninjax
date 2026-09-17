package org.r10r.ninjax.demo.todo;

import org.r10r.ninjax.demo.todo.tasks.TaskService;
import org.r10r.ninjax.demo.todo.tasks.TodoController;
import org.r10r.ninjax.demo.todo.tasks.TaskRepository;
import java.util.Optional;
import org.r10r.ninjax.core.NinjaJavaLogging;
import org.r10r.ninjax.core.Router;
import org.r10r.ninjax.core.properties.NinjaProperties;
import org.r10r.ninjax.core.server.NinjaHttpServer;
import org.r10r.ninjax.db.jdbi.NinjaJdbiImpl;
import org.r10r.ninjax.db.hikari.NinjaDbHikariProvider;
import org.r10r.ninjax.db.flyway.NinjaFlywayMigrator;
import org.r10r.ninjax.db.jdbc.NinjaDatasourcePropertiesExtractor;
import org.r10r.ninjax.jetty.NinjaJetty;
import org.r10r.ninjax.json.Json;

public class TodoApplication {

    public TodoApplication(MockableComponents mockableComponents) {
        NinjaJavaLogging.initialize();

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

        // Server startup. Pick the server with the property ninja.server (default: jdk).
        // Both servers block here until they are stopped.
        var server = ninjaProperties.get("ninja.server").orElse("jdk");
        try {
            switch (server) {
                case "jdk" -> new NinjaHttpServer(router, ninjaProperties);
                case "jetty" -> new NinjaJetty(router, ninjaProperties);
                default -> throw new IllegalArgumentException(
                        "Unknown ninja.server '" + server + "'. Use 'jdk' or 'jetty'.");
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to start server '" + server + "'", e);
        }
    }
    
    public TodoApplication() {
        this(MockableComponents.nothingMocked());
    }

    public void main() {
        new TodoApplication();
    }
    

    // For testing only
    public record MockableComponents(Optional<TaskService> taskService) {
        public static MockableComponents nothingMocked() {
            return new MockableComponents(Optional.empty());
        }
    }
}
