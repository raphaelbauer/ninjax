package org.ninjax.demo.todo.tasks;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.mapper.reflect.ConstructorMapper;
import org.ninjax.db.jdbi.NinjaJdbi;

public class TaskRepository {

    private final Jdbi jdbi;

    public TaskRepository(NinjaJdbi ninjaJdbi) {
        this.jdbi = ninjaJdbi.getJdbi("default");

        this.jdbi.registerRowMapper(ConstructorMapper.factory(Task.class));
    }

    public List<Task> findAny() {
        return jdbi.withHandle(handle ->
            handle.createQuery("SELECT id, title, description, created_at, completed FROM tasks ORDER BY created_at DESC")
                  .mapTo(Task.class)
                  .list()
        );
    }

    public Optional<Task> findById(long id) {
        return jdbi.withHandle(handle ->
            handle.createQuery("SELECT id, title, description, created_at, completed FROM tasks WHERE id = :id")
                  .bind("id", id)
                  .mapTo(Task.class)
                  .findFirst()
        );
    }

    public long insert(String title, String description, LocalDateTime createdAt, boolean completed) {
        return jdbi.withHandle(handle ->
            handle.createUpdate(
                    "INSERT INTO tasks (title, description, created_at, completed) " +
                    "VALUES (:title, :description, :createdAt, :completed)")
                  .bind("title", title)
                  .bind("description", description)
                  .bind("createdAt", createdAt)
                  .bind("completed", completed)
                  .executeAndReturnGeneratedKeys("id")
                  .mapTo(Long.class)
                  .one()
        );
    }

    public int deleteById(long id) {
        return jdbi.withHandle(handle ->
            handle.createUpdate("DELETE FROM tasks WHERE id = :id")
                  .bind("id", id)
                  .execute()
        );
    }

    public int updateCompleted(boolean completed, long id) {
        return jdbi.withHandle(handle ->
            handle.createUpdate("UPDATE tasks SET completed = :completed WHERE id = :id")
                  .bind("completed", completed)
                  .bind("id", id)
                  .execute()
        );
    }
}