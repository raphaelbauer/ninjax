package org.ninja.demo.todo;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.sqlobject.config.RegisterConstructorMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.GetGeneratedKeys;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.ninja.db.jdbi.NinjaJdbi;

public class TaskService {

    public interface TaskDaoInterface {

        @SqlQuery("SELECT id, title, description, created_at, completed FROM tasks ORDER BY created_at DESC")
        @RegisterConstructorMapper(Task.class)
        List<Task> findAny();

        @SqlQuery("SELECT id, title, description, created_at, completed FROM tasks WHERE id = :id")
        @RegisterConstructorMapper(Task.class)
        Optional<Task> findById(long id);

        @SqlUpdate("INSERT INTO tasks (title, description, created_at, completed) "
                + "VALUES (:title, :description, :createdAt, :completed)")
        @GetGeneratedKeys
        long insert(@Bind("title") String title, @Bind("description") String description, @Bind("createdAt") LocalDateTime createdAt, @Bind("completed") boolean completed);

        @SqlUpdate("DELETE FROM tasks WHERE id = :id")
        int deleteById(@Bind("id") long id);

        @SqlUpdate("UPDATE tasks SET completed = :completed WHERE id = :id")
        int updateCompleted(@Bind("id") long id, @Bind("completed") boolean completed);
    }

    private final Jdbi jdbi;
    private final TaskDaoInterface taskDaoInterface;

    public TaskService(NinjaJdbi ninjaJdbi) {
        this.jdbi = ninjaJdbi.getJdbi("default");
        this.taskDaoInterface = this.jdbi.onDemand(TaskDaoInterface.class);
    }

    public List<Task> findAny() {
        return taskDaoInterface.findAny();
    }

    public Task create(Task task) {
        var id = taskDaoInterface.insert(task.title(), task.description(), task.createdAt(), task.completed());
        return task.withId(id);
    }

    public boolean delete(long id) {
        return taskDaoInterface.deleteById(id) > 0;
    }

    public boolean toggleCompleted(long id) {
        java.util.Optional<Task> task = taskDaoInterface.findById(id);
        if (task.isPresent()) {
            Task currentTask = task.get();
            return taskDaoInterface.updateCompleted(id, !currentTask.completed()) > 0;
        }
        return false;
    }
}
