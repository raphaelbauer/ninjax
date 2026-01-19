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

public class TaskRepository {

    public static interface TaskRepositoryInterface {

        @SqlQuery("SELECT id, title, description, created_at, completed FROM tasks ORDER BY created_at DESC")
        @RegisterConstructorMapper(Task.class)
        List<Task> findAny();

        @SqlQuery("SELECT id, title, description, created_at, completed FROM tasks WHERE id = :id")
        @RegisterConstructorMapper(Task.class)
        Optional<Task> findById(@Bind("id") long id);

        @SqlUpdate("INSERT INTO tasks (title, description, created_at, completed) "
                + "VALUES (:title, :description, :createdAt, :completed)")
        @GetGeneratedKeys
        long insert(@Bind("title") String title, @Bind("description") String description, @Bind("createdAt") LocalDateTime createdAt, @Bind("completed") boolean completed);

        @SqlUpdate("DELETE FROM tasks WHERE id = :id")
        int deleteById(@Bind("id") long id);

        @SqlUpdate("UPDATE tasks SET completed = :completed WHERE id = :id")
        int updateCompleted(@Bind("completed") boolean completed, @Bind("id") long id);
    }

    private final Jdbi jdbi;
    private final TaskRepositoryInterface taskRepositoryInterface;

    public TaskRepository(NinjaJdbi ninjaJdbi) {
        this.jdbi = ninjaJdbi.getJdbi("default");
        this.taskRepositoryInterface = this.jdbi.onDemand(TaskRepositoryInterface.class);
    }

    public List<Task> findAny() {
        return this.taskRepositoryInterface.findAny();
    }

    public Optional<Task> findById(@Bind("id") long id) {
        return this.taskRepositoryInterface.findById(id);

    }

    public long insert(@Bind("title") String title, @Bind("description") String description, @Bind("createdAt") LocalDateTime createdAt, @Bind("completed") boolean completed) {
        return this.taskRepositoryInterface.insert(title, description, createdAt, completed);
    }

    public int deleteById(@Bind("id") long id) {
        return this.taskRepositoryInterface.deleteById(id);
    }

    public int updateCompleted(@Bind("completed") boolean completed, @Bind("id") long id) {
        return this.taskRepositoryInterface.updateCompleted(completed, id);
    }

}
