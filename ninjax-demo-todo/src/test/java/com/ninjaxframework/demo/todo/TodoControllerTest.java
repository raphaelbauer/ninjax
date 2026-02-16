package com.ninjaxframework.demo.todo;

import com.ninjaxframework.demo.todo.tasks.TaskService;
import com.ninjaxframework.demo.todo.tasks.Task;
import com.ninjaxframework.demo.todo.tasks.TodoController;
import java.time.LocalDateTime;
import static com.ninjaxframework.test.ResultAssertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.ninjaxframework.core.Request;
import com.ninjaxframework.core.Result;
import java.util.*;
import org.mockito.Mockito;
import com.ninjaxframework.json.Json;
import com.ninjaxframework.test.TestRequest;

/**
 * Unit tests for TodoController. Uses a fake TaskService to avoid database
 * dependencies.
 */
class TodoControllerTest {

    private TaskService taskService;
    private TodoController controller;

    @BeforeEach
    void setUp() {
        // given
        var json = new Json();
        taskService = Mockito.mock(TaskService.class);
        controller = new TodoController(taskService, json);
    }

    // showTasks endpoint
    @Test
    void showTasks_whenNoTasks_returns200WithEmptyList() {
        // given
        Request request = TestRequest.basic();

        // when
        Result result = controller.showTasks(request);

        // then
        assertThat(result).hasStatus(200);
        assertThat(result).hasHtmlContent();
    }

    @Test
    void showTasks_whenTasksExist_returns200WithHtmlContent() {

        // given
        Mockito.when(taskService.findAny()).thenReturn(List.of(
                new Task(1L, "title", "descriptio", LocalDateTime.now(), true),
                new Task(2L, "title", "descriptio", LocalDateTime.now(), true)));

        Request request = TestRequest.basic();

        // when
        Result result = controller.showTasks(request);

        // then
        assertThat(result).hasStatus(200);
        assertThat(result).hasHtmlContent();
        assertThat(result).hasContent();
    }

}
