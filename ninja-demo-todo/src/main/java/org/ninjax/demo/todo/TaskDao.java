//package org.ninjax.demo.todo;
//
//import org.jdbi.v3.sqlobject.config.RegisterBeanMapper;
//import org.jdbi.v3.sqlobject.statement.SqlQuery;
//import org.jdbi.v3.sqlobject.statement.SqlUpdate;
//import org.jdbi.v3.sqlobject.statement.GetGeneratedKeys;
//
//import java.util.List;
//import java.util.Optional;
//
//@RegisterBeanMapper(Task.class)
//public interface TaskDao {
//
//    @SqlQuery("SELECT * FROM tasks ORDER BY created_at DESC")
//    List<Task> findAny();
//
//    @SqlQuery("SELECT * FROM tasks WHERE id = :id")
//    Optional<Task> findById(long id);
//
//    @SqlUpdate("INSERT INTO tasks (title, description, created_at, completed) VALUES (:title, :description, :createdAt, :completed)")
//    @GetGeneratedKeys
//    long insert(String title, String description, java.time.LocalDateTime createdAt, boolean completed);
//
//    @SqlUpdate("DELETE FROM tasks WHERE id = :id")
//    int deleteById(long id);
//
//    @SqlUpdate("UPDATE tasks SET completed = :completed WHERE id = :id")
//    int updateCompleted(long id, boolean completed);
//
//    @SqlUpdate("UPDATE tasks SET title = :title, description = :description WHERE id = :id")
//    int update(long id, String title, String description);
//
//    default Task create(Task task) {
//        long id = insert(task.title(), task.description(), task.createdAt(), task.completed());
//        return task.withId(id);
//    }
//
//    default boolean delete(long id) {
//        return deleteById(id) > 0;
//    }
//
//    default boolean toggleCompleted(long id) {
//        Optional<Task> task = findById(id);
//        if (task.isPresent()) {
//            Task currentTask = task.get();
//            return updateCompleted(id, !currentTask.completed()) > 0;
//        }
//        return false;
//    }
//}