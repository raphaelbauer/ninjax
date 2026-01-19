//package org.ninja.demo.todo.testutil;
//
//import java.util.ArrayList;
//import java.util.LinkedHashMap;
//import java.util.List;
//import java.util.Map;
//import org.ninja.demo.todo.Task;
//import org.ninja.demo.todo.TaskService;
//
//
//
///**
// * You likely want to use Mockito in real world applications.
// * 
// * We stick to plain Java to make upgrades painless and not to 
// * have too many dependencies.
// */
//public class TaskServiceFake extends TaskService {
//
//    private final Map<Long, Task> tasks = new LinkedHashMap<>();
//    private long nextId = 1L;
//    private boolean shouldThrowException = false;
//
//    public TaskServiceFake() {
//        super(createStubNinjaJdbi());
//    }
//
//    private static org.ninja.db.jdbi.NinjaJdbi createStubNinjaJdbi() {
//        org.jdbi.v3.core.Jdbi jdbi = org.jdbi.v3.core.Jdbi.create("jdbc:h2:mem:stub");
//        return datasourceName -> jdbi;
//    }
//
//    public void addTask(Task task) {
//        long id = task.id() != null ? task.id() : nextId++;
//        tasks.put(id, task.withId(id));
//    }
//
//    public void setShouldThrowException(boolean shouldThrow) {
//        this.shouldThrowException = shouldThrow;
//    }
//
//    public Task findById(long id) {
//        return tasks.get(id);
//    }
//
//    @Override
//    public List<Task> findAny() {
//        if (shouldThrowException) {
//            throw new RuntimeException("Simulated exception");
//        }
//        return new ArrayList<>(tasks.values());
//    }
//
//    @Override
//    public Task create(Task task) {
//        if (shouldThrowException) {
//            throw new RuntimeException("Simulated exception");
//        }
//        long id = nextId++;
//        Task created = task.withId(id);
//        tasks.put(id, created);
//        return created;
//    }
//
//    @Override
//    public boolean delete(long id) {
//        if (shouldThrowException) {
//            throw new RuntimeException("Simulated exception");
//        }
//        return tasks.remove(id) != null;
//    }
//
//    @Override
//    public boolean toggleCompleted(long id) {
//        if (shouldThrowException) {
//            throw new RuntimeException("Simulated exception");
//        }
//        Task task = tasks.get(id);
//        if (task != null) {
//            Task updated = new Task(task.id(), task.title(), task.description(),
//                    task.createdAt(), !task.completed());
//            tasks.put(id, updated);
//            return true;
//        }
//        return false;
//    }
//
//}
