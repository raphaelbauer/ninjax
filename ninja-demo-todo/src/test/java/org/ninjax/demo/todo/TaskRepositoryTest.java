package org.ninjax.demo.todo;

import org.ninjax.demo.todo.tasks.TaskRepository;
import com.google.common.truth.Truth;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.ninjax.db.flyway.NinjaFlywayMigrator;
import org.ninjax.db.hikari.NinjaDbHikariProvider;
import org.ninjax.db.jdbc.NinjaDatasourceProperties;
import org.ninjax.db.jdbc.NinjaDatasourcesProperties;
import org.ninjax.db.jdbi.NinjaJdbiImpl;


public class TaskRepositoryTest {

    private TaskRepository taskRepository;

    public TaskRepositoryTest() {

    }

    @BeforeEach
    public void beforeAll() {

        var migrationConfiguration = new NinjaDatasourceProperties.MigrationConfiguration("sa", "");

        var uuid = UUID.randomUUID();
        var ninjaDatasourceProperties = new NinjaDatasourceProperties(
                "default",
                Optional.empty(),
                "jdbc:h2:./target/test-db-" + uuid,
                "sa",
                "",
                Optional.of(migrationConfiguration),
                Map.of());

        var ninjaDatasourcesProperties = new NinjaDatasourcesProperties(List.of(ninjaDatasourceProperties));
        var ninjaFlywayMigrator = new NinjaFlywayMigrator(ninjaDatasourcesProperties);
        var ninjaDbHikariProvider = new NinjaDbHikariProvider(ninjaDatasourcesProperties);
        var ninjaJdbi = new NinjaJdbiImpl(ninjaDbHikariProvider.get());
        
        this.taskRepository = new TaskRepository(ninjaJdbi);
    }

    @Test
    public void testFindAny_whenEmtpy() {
        // given 
        // an empty database
        
        //when
        var result = taskRepository.findAny();
        
        // then
        Truth.assertThat(result).isEmpty();
          
    }
    
    @Test
    public void testFindAny_withData() {
        // given 
        taskRepository.insert("a title 1", "a description", LocalDateTime.now(), true);
        taskRepository.insert("a title 2", "a description", LocalDateTime.now(), true);
        
        //when
        var result = taskRepository.findAny();
        
        // then
        Truth.assertThat(result).hasSize(2);
          
    }

}
