package org.ninjax.demo.todo;

import org.ninjax.core.NinjaJetty;
import org.ninjax.core.Router;
import org.ninjax.core.properties.NinjaProperties;
import org.ninjax.db.jdbi.NinjaJdbiImpl;
import org.ninjax.db.hikari.NinjaDbHikariProvider;
import org.ninjax.db.flyway.NinjaFlywayMigrator;
import org.ninjax.db.jdbc.NinjaDatasourcePropertiesExtractor;

public class TodoApplication {
    
    public final NinjaProperties ninjaProperties = new NinjaProperties();
    
    ////////////////////////////////////////////////////////////////////////////
    // DB Configuration
    final private NinjaDatasourcePropertiesExtractor ninjaDatasourceConfigProvider = new NinjaDatasourcePropertiesExtractor(ninjaProperties);
    
    final private NinjaFlywayMigrator ninjaFlywayMigrator = new NinjaFlywayMigrator(ninjaDatasourceConfigProvider.get());
    
    final private NinjaDbHikariProvider ninjaDbHikariProvider = new NinjaDbHikariProvider(ninjaDatasourceConfigProvider.get());
    final private NinjaJdbiImpl ninjaJdbiImpl = new NinjaJdbiImpl(ninjaDbHikariProvider.get());
    // end
    ////////////////////////////////////////////////////////////////////////////
    
    public final TaskService taskService = new TaskService(ninjaJdbiImpl);
    public final TodoController todoController = new TodoController(taskService);
    
    public final Router router = new Router();
    {
        router.GET("/").with(todoController::showTasks);
        router.POST("/tasks").with(todoController::addTask);
        router.POST("/tasks/delete").with(todoController::deleteTask);
        router.GET("/tasks.json").with(todoController::getTasksJson);
    }
    
    public final NinjaJetty ninja;
    {
        try {
            ninja = new NinjaJetty(router, ninjaProperties);
        } catch (Exception e) {
            throw new RuntimeException("Failed to start NinjaJetty", e);
        }
    }

    public static void main(String [] a) {
        new TodoApplication();
    }
}