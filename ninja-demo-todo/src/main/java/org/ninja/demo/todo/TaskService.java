package org.ninja.demo.todo;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.mapper.reflect.ConstructorMapper;
import org.jdbi.v3.core.mapper.reflect.ReflectionMappers;
import org.jdbi.v3.sqlobject.config.RegisterConstructorMapper;
import org.jdbi.v3.sqlobject.config.RegisterRowMapperFactory;
import org.jdbi.v3.sqlobject.statement.GetGeneratedKeys;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.ninja.db.jdbi.NinjaJdbi;

public class TaskService {
    
public interface TaskDaoInterface {

    @SqlQuery("SELECT id, title, description, created_at, completed FROM tasks ORDER BY created_at DESC")
    List<Task> findAny();

    @SqlQuery("SELECT id, title, description, created_at, completed FROM tasks WHERE id = :id")
    Optional<Task> findById(long id);

    @SqlUpdate("INSERT INTO tasks (title, description, created_at, completed) " +
               "VALUES (:title, :description, :createdAt, :completed)")
    @GetGeneratedKeys
    long insert(String title, String description,
                LocalDateTime createdAt, boolean completed);

    @SqlUpdate("DELETE FROM tasks WHERE id = :id")
    int deleteById(long id);

    @SqlUpdate("UPDATE tasks SET completed = :completed WHERE id = :id")
    int updateCompleted(long id, boolean completed);
}

    private final Jdbi jdbi;
    private final TaskDaoInterface taskDaoInterface;
            
    public TaskService(NinjaJdbi ninjaJdbi) {
        this.jdbi = ninjaJdbi.getJdbi("default");
        this.taskDaoInterface = jdbi.onDemand(TaskDaoInterface.class);
    }

    public List<Task> findAny() {
        return this.taskDaoInterface.findAny();
    }

    public Task create(Task task) {
        TaskDaoInterface dao = jdbi.open().attach(TaskDaoInterface.class);
        long id = dao.insert(task.title(), task.description(), task.created_at(), task.completed());
        return task.withId(id);
    }

    public boolean delete(long id) {
        return jdbi.open().attach(TaskDaoInterface.class).deleteById(id) > 0;
    }
    
    public boolean toggleCompleted(long id) {
        TaskDaoInterface dao = jdbi.open().attach(TaskDaoInterface.class);
        java.util.Optional<Task> task = dao.findById(id);
        if (task.isPresent()) {
            Task currentTask = task.get();
            return dao.updateCompleted(id, !currentTask.completed()) > 0;
        }
        return false;
    }
}