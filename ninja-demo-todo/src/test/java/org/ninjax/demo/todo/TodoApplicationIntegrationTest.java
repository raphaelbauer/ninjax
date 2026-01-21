package org.ninjax.demo.todo;

import static com.google.common.truth.Truth.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.ninjax.test.HttpTestClient;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import static org.junit.jupiter.api.AssertionsKt.fail;

/**
 * Integration tests for the TodoApplication. Tests the full application stack
 * with real HTTP requests.
 */
class TodoApplicationIntegrationTest {

    private static int TEST_PORT;
    private static Thread serverThread;
    private static TodoApplication application;

    private HttpTestClient client;
    private ObjectMapper objectMapper;

    @BeforeAll
    static void startApplication() throws Exception {
        // given
        // Set test port via system property
        TEST_PORT = findAvailablePort(1000);
        System.setProperty("ninja.port", String.valueOf(TEST_PORT));

        String dbConnectString = "jdbc:h2:./target/test-db-" + UUID.randomUUID();
        System.setProperty("application.datasource.default.url", dbConnectString);

        // Start application in background thread
        serverThread = new Thread(() -> {
            application = new TodoApplication();
        });
        serverThread.setDaemon(true);
        serverThread.start();

        waitForServer("http://localhost:" + TEST_PORT);
    }

    @BeforeEach
    void setUp() throws IOException {
        // given
        client = HttpTestClient.localhost(TEST_PORT);
        objectMapper = new ObjectMapper();

        // Clear all tasks before each test
        HttpTestClient.HttpTestResponse response = client.get("/tasks.json");
        if (response.statusCode() == 200) {
            JsonNode tasks = objectMapper.readTree(response.body());
            for (JsonNode task : tasks) {
                long id = task.get("id").asLong();
                try {
                    client.post("/tasks/delete", Map.of("id", String.valueOf(id)));
                } catch (Exception e) {
                    // Ignore errors during cleanup
                }
            }
        }
    }

    @Test
    void fullWorkflow_createReadUpdateDelete() throws IOException {
        // given
        // Database is empty

        // when - Create a task
        HttpTestClient.HttpTestResponse createResponse = client.post("/tasks", Map.of("title", "Buy milk"));

        // then
        assertThat(createResponse.statusCode()).isEqualTo(303);
        assertThat(createResponse.headers().get("Location")).containsExactly("/");

        // when - Get tasks as JSON
        HttpTestClient.HttpTestResponse getResponse = client.get("/tasks.json");

        // then
        assertThat(getResponse.statusCode()).isEqualTo(200);
        JsonNode tasks = objectMapper.readTree(getResponse.body());
        assertThat(tasks.size()).isEqualTo(1);
        assertThat(tasks.get(0).get("title").asText()).isEqualTo("Buy milk");
        assertThat(tasks.get(0).get("completed").asBoolean()).isFalse();

        long taskId = tasks.get(0).get("id").asLong();

        // when - Toggle completion
        HttpTestClient.HttpTestResponse toggleResponse = client.post("/tasks/toggle", Map.of("id", String.valueOf(taskId)));

        // then
        assertThat(toggleResponse.statusCode()).isEqualTo(303);

        // Verify task is completed
        HttpTestClient.HttpTestResponse getAfterToggle = client.get("/tasks.json");
        JsonNode tasksAfterToggle = objectMapper.readTree(getAfterToggle.body());
        assertThat(tasksAfterToggle.get(0).get("completed").asBoolean()).isTrue();

        // when - Delete task
        HttpTestClient.HttpTestResponse deleteResponse = client.post("/tasks/delete", Map.of("id", String.valueOf(taskId)));

        // then
        assertThat(deleteResponse.statusCode()).isEqualTo(303);

        // Verify task is deleted
        HttpTestClient.HttpTestResponse getAfterDelete = client.get("/tasks.json");
        JsonNode tasksAfterDelete = objectMapper.readTree(getAfterDelete.body());
        assertThat(tasksAfterDelete.size()).isEqualTo(0);
    }

    @Test
    void showTasks_returnsHtmlPage() throws IOException {
        // given
        client.post("/tasks", Map.of("title", "Test Task"));

        // when
        HttpTestClient.HttpTestResponse response = client.get("/");

        // then
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("Test Task");
        assertThat(response.body()).contains("html");
    }

    @Test
    void addTask_createsTaskAndRedirects() throws IOException {
        // given
        // Database is empty

        // when
        HttpTestClient.HttpTestResponse response = client.post("/tasks", Map.of("title", "New Task"));

        // then
        assertThat(response.statusCode()).isEqualTo(303);
        assertThat(response.headers().get("Location")).containsExactly("/");

        // Verify task was created
        HttpTestClient.HttpTestResponse getResponse = client.get("/tasks.json");
        JsonNode tasks = objectMapper.readTree(getResponse.body());
        assertThat(tasks.size()).isEqualTo(1);
        assertThat(tasks.get(0).get("title").asText()).isEqualTo("New Task");
    }

    @Test
    void getTasksJson_returnsJsonArray() throws IOException {
        // given
        client.post("/tasks", Map.of("title", "Task 1"));
        client.post("/tasks", Map.of("title", "Task 2"));
        client.post("/tasks", Map.of("title", "Task 3"));

        // when
        HttpTestClient.HttpTestResponse response = client.get("/tasks.json");

        // then
        assertThat(response.statusCode()).isEqualTo(200);
        JsonNode tasks = objectMapper.readTree(response.body());
        assertThat(tasks.size()).isEqualTo(3);

        // Verify order (newest first)
        assertThat(tasks.get(0).get("title").asText()).isEqualTo("Task 3");
        assertThat(tasks.get(1).get("title").asText()).isEqualTo("Task 2");
        assertThat(tasks.get(2).get("title").asText()).isEqualTo("Task 1");
    }

    @Test
    void toggleTask_changesCompletionStatus() throws IOException {
        // given
        client.post("/tasks", Map.of("title", "Task to toggle"));

        HttpTestClient.HttpTestResponse getResponse = client.get("/tasks.json");
        JsonNode tasks = objectMapper.readTree(getResponse.body());
        long taskId = tasks.get(0).get("id").asLong();
        boolean initialCompleted = tasks.get(0).get("completed").asBoolean();

        // when
        HttpTestClient.HttpTestResponse toggleResponse = client.post("/tasks/toggle", Map.of("id", String.valueOf(taskId)));

        // then
        assertThat(toggleResponse.statusCode()).isEqualTo(303);

        // Verify status changed
        HttpTestClient.HttpTestResponse getAfterToggle = client.get("/tasks.json");
        JsonNode tasksAfterToggle = objectMapper.readTree(getAfterToggle.body());
        boolean afterCompleted = tasksAfterToggle.get(0).get("completed").asBoolean();
        assertThat(afterCompleted).isEqualTo(!initialCompleted);
    }

    @Test
    void deleteTask_removesTask() throws IOException {
        // given
        client.post("/tasks", Map.of("title", "Task to delete"));
        client.post("/tasks", Map.of("title", "Task to keep"));

        HttpTestClient.HttpTestResponse getResponse = client.get("/tasks.json");
        JsonNode tasks = objectMapper.readTree(getResponse.body());
        assertThat(tasks.size()).isEqualTo(2);

        // Tasks are ordered DESC by created_at, so index 0 is newest ("Task to keep"), index 1 is oldest ("Task to delete")
        long taskIdToDelete = tasks.get(1).get("id").asLong();

        // when
        HttpTestClient.HttpTestResponse deleteResponse = client.post("/tasks/delete", Map.of("id", String.valueOf(taskIdToDelete)));

        // then
        assertThat(deleteResponse.statusCode()).isEqualTo(303);

        // Verify task was deleted
        HttpTestClient.HttpTestResponse getAfterDelete = client.get("/tasks.json");
        JsonNode tasksAfterDelete = objectMapper.readTree(getAfterDelete.body());
        assertThat(tasksAfterDelete.size()).isEqualTo(1);
        assertThat(tasksAfterDelete.get(0).get("title").asText()).isEqualTo("Task to keep");
    }

    @Test
    void multipleTasksWorkflow() throws IOException {
        // given
        // Create multiple tasks
        client.post("/tasks", Map.of("title", "Task 1"));
        client.post("/tasks", Map.of("title", "Task 2"));
        client.post("/tasks", Map.of("title", "Task 3"));

        // when - Get all tasks
        HttpTestClient.HttpTestResponse getResponse = client.get("/tasks.json");

        // then
        JsonNode tasks = objectMapper.readTree(getResponse.body());
        assertThat(tasks.size()).isEqualTo(3);

        // when - Toggle first and third task
        long task1Id = tasks.get(0).get("id").asLong();
        long task3Id = tasks.get(2).get("id").asLong();
        client.post("/tasks/toggle", Map.of("id", String.valueOf(task1Id)));
        client.post("/tasks/toggle", Map.of("id", String.valueOf(task3Id)));

        // then - Verify completion status
        HttpTestClient.HttpTestResponse getAfterToggle = client.get("/tasks.json");
        JsonNode tasksAfterToggle = objectMapper.readTree(getAfterToggle.body());
        assertThat(tasksAfterToggle.get(0).get("completed").asBoolean()).isTrue();
        assertThat(tasksAfterToggle.get(1).get("completed").asBoolean()).isFalse();
        assertThat(tasksAfterToggle.get(2).get("completed").asBoolean()).isTrue();

        // when - Delete middle task
        long task2Id = tasksAfterToggle.get(1).get("id").asLong();
        client.post("/tasks/delete", Map.of("id", String.valueOf(task2Id)));

        // then - Verify deletion
        HttpTestClient.HttpTestResponse getAfterDelete = client.get("/tasks.json");
        JsonNode tasksAfterDelete = objectMapper.readTree(getAfterDelete.body());
        assertThat(tasksAfterDelete.size()).isEqualTo(2);
        assertThat(tasksAfterDelete.get(0).get("title").asText()).isEqualTo("Task 3");
        assertThat(tasksAfterDelete.get(1).get("title").asText()).isEqualTo("Task 1");
    }

    private static void waitForServer(String url) throws InterruptedException {
        
        Duration timeout = Duration.ofSeconds(2);
        Duration pollInterval = Duration.ofMillis(100);
        
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(500))
                .build();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .timeout(Duration.ofMillis(500))
                .build();

        long deadline = System.nanoTime() + timeout.toNanos();

        while (System.nanoTime() < deadline) {
            try {
                HttpResponse<Void> response
                        = client.send(request, HttpResponse.BodyHandlers.discarding());

                // Adjust condition if you need a specific status (e.g., 200 only)
                if (response.statusCode() >= 200 && response.statusCode() < 500) {
                    return; // server is responding
                }
            } catch (Exception ignored) {
                // server not yet up or connection failed
            }

            Thread.sleep(pollInterval.toMillis());
        }

        throw new RuntimeException("Server did not start within " + timeout.toMillis() + " ms");
    }

    private static int findAvailablePort(int minPort) throws IOException {
        // Bind to port 0 (let OS choose any free port), ensure it's >= minPort
        // If the chosen port is < minPort (very unlikely for 0), retry.
        int attempts = 0;
        while (true) {
            attempts++;
            if (attempts > 50) {
                throw new IOException("Unable to find available port >= " + minPort);
            }
            try (ServerSocket socket
                    = new ServerSocket(0, 0, InetAddress.getByName("127.0.0.1"))) {
                int port = socket.getLocalPort();
                if (port >= minPort) {
                    return port;
                }
            }
        }
    }

}
