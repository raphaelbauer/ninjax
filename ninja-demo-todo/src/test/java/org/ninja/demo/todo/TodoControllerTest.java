package org.ninja.demo.todo;

import java.time.LocalDateTime;
import static org.ninja.test.ResultAssertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.ninja.core.Request;
import org.ninja.core.Result;
import java.util.*;
import org.mockito.Mockito;
import org.ninja.test.TestRequest;

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
        taskService = Mockito.mock(TaskService.class);
        controller = new TodoController(taskService);
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
