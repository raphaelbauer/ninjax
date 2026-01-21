package org.ninjax.demo.todo;

import org.ninjax.demo.todo.tasks.TaskRepository;
import org.ninjax.demo.todo.tasks.TaskService;
import org.ninjax.demo.todo.tasks.Task;
import static com.google.common.truth.Truth.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import org.mockito.Mockito;


class TaskServiceTest {

    private TaskService taskService;
    private TaskRepository taskRepository;

    @BeforeEach
    void setUp() {
        // given
        taskRepository = Mockito.mock(TaskRepository.class);
        taskService = new TaskService(taskRepository);
    }
    // findAny() tests

    @Test
    void findAny_whenNoTasks_returnsEmptyList() {
        // given
        
        Mockito.when(taskRepository.findAny()).thenReturn(List.of());
        // database is empty after migrations

        // when
        List<Task> tasks = taskService.findAny();

        // then
        assertThat(tasks).isEmpty();
    }

    @Test
    void findAny_whenTasksExist_returnsAllTasks() {
        // given

        Task task1 = new Task(1L, "title", "descriptio", LocalDateTime.now(), true);
        Task task2 = new Task(2L, "title", "descriptio", LocalDateTime.now(), true);
        Task task3 = new Task(3L, "title", "descriptio", LocalDateTime.now(), true);

        
        Mockito.when(taskRepository.findAny()).thenReturn(List.of(task1, task2, task3));

        // when
        List<Task> tasks = taskService.findAny();

        // then
        assertThat(tasks).hasSize(3);
        assertThat(tasks.get(0).id()).isEqualTo(1); 
        assertThat(tasks.get(1).id()).isEqualTo(2); 
        assertThat(tasks.get(2).id()).isEqualTo(3); 
    }

   
}
