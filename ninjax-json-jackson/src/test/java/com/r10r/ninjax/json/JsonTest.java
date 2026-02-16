package com.r10r.ninjax.json;

import com.fasterxml.jackson.core.type.TypeReference;
import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.r10r.ninjax.core.Request;
import com.r10r.ninjax.core.Result;
import com.r10r.ninjax.core.Router;
import com.r10r.ninjax.test.TestRequest;
import com.r10r.ninjax.json.testhelper.JsonTestHelper;
import com.r10r.ninjax.json.testhelper.JsonTestHelper.TestPerson;
import com.r10r.ninjax.json.testhelper.JsonTestHelper.TestRecord;

class JsonTest {

    private Json json;

    @BeforeEach
    void setUp() {
        json = new Json();
    }

    @Test
    void getJsonBody_validPersonJson_returnsOptionalWithPerson() {
        // Given
        String personJson = JsonTestHelper.createValidPersonJson();
        Request.InputStreamGetter inputStreamGetter = () -> new ByteArrayInputStream(personJson.getBytes(StandardCharsets.UTF_8));
        Request request = createTestRequest(inputStreamGetter);

        // When
        Optional<TestPerson> result = json.getJsonBody(request, new TypeReference<TestPerson>() {});

        // Then
        assertThat(result).isPresent();
        TestPerson person = result.get();
        assertThat(person.getName()).isEqualTo("Jane Smith");
        assertThat(person.getAge()).isEqualTo(25);
        assertThat(person.getEmail()).hasValue("jane@example.com");
        assertThat(person.getTags()).containsExactly("designer", "ui", "ux");
    }

    @Test
    void getJsonBody_validRecordJson_returnsOptionalWithRecord() {
        // Given
        String recordJson = JsonTestHelper.createValidRecordJson();
        Request.InputStreamGetter inputStreamGetter = () -> new ByteArrayInputStream(recordJson.getBytes(StandardCharsets.UTF_8));
        Request request = createTestRequest(inputStreamGetter);

        // When
        Optional<TestRecord> result = json.getJsonBody(request, new TypeReference<TestRecord>() {});

        // Then
        assertThat(result).isPresent();
        TestRecord record = result.get();
        assertThat(record.id()).isEqualTo("record-456");
        assertThat(record.description()).hasValue("Test record description");
        assertThat(record.scores()).containsExactly(100, 85, 90);
    }

    @Test
    void getJsonBody_complexObjectWithOptionalFields_correctlyDeserializes() {
        // Given
        TestPerson originalPerson = JsonTestHelper.createSamplePerson();
        String personJson = serializeToJson(originalPerson);
        Request.InputStreamGetter inputStreamGetter = () -> new ByteArrayInputStream(personJson.getBytes(StandardCharsets.UTF_8));
        Request request = createTestRequest(inputStreamGetter);

        // When
        Optional<TestPerson> result = json.getJsonBody(request, new TypeReference<TestPerson>() {});

        // Then
        assertThat(result).isPresent();
        TestPerson deserializedPerson = result.get();
        assertThat(deserializedPerson).isEqualTo(originalPerson);
        assertThat(deserializedPerson.getEmail()).hasValue("john@example.com");
        assertThat(deserializedPerson.getCreatedAt()).isNotNull();
    }

    @Test
    void getJsonBody_invalidJsonSyntax_returnsOptionalEmpty() {
        // Given
        String invalidJson = JsonTestHelper.createInvalidJson();
        Request.InputStreamGetter inputStreamGetter = () -> new ByteArrayInputStream(invalidJson.getBytes(StandardCharsets.UTF_8));
        Request request = createTestRequest(inputStreamGetter);

        // When
        Optional<TestPerson> result = json.getJsonBody(request, new TypeReference<TestPerson>() {});

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void getJsonBody_typeMismatchJson_returnsOptionalEmpty() {
        // Given
        String typeMismatchJson = JsonTestHelper.createTypeMismatchJson();
        Request.InputStreamGetter inputStreamGetter = () -> new ByteArrayInputStream(typeMismatchJson.getBytes(StandardCharsets.UTF_8));
        Request request = createTestRequest(inputStreamGetter);

        // When
        Optional<TestPerson> result = json.getJsonBody(request, new TypeReference<TestPerson>() {});

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void getJsonBody_emptyInputStream_returnsOptionalEmpty() {
        // Given
        Request.InputStreamGetter inputStreamGetter = () -> new ByteArrayInputStream(new byte[0]);
        Request request = createTestRequest(inputStreamGetter);

        // When
        Optional<TestPerson> result = json.getJsonBody(request, new TypeReference<TestPerson>() {});

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void getJsonBody_ioException_returnsOptionalEmpty() throws Exception {
        // Given
        InputStream failingStream = new InputStream() {
            @Override
            public int read() throws IOException {
                throw new IOException("Simulated IO error");
            }
        };
        Request.InputStreamGetter inputStreamGetter = () -> failingStream;
        Request request = createTestRequest(inputStreamGetter);

        // When
        Optional<TestPerson> result = json.getJsonBody(request, new TypeReference<TestPerson>() {});

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void json_complexObject_returnsCorrectJsonOutputStream() throws Exception {
        // Given
        TestPerson person = JsonTestHelper.createSamplePerson();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        // When
        Result.OutputStreamRenderer renderer = json.json(person);
        renderer.streamTo(outputStream);

        // Then
        String jsonOutput = outputStream.toString(StandardCharsets.UTF_8);
        assertThat(jsonOutput).contains("\"name\":\"John Doe\"");
        assertThat(jsonOutput).contains("\"age\":30");
        assertThat(jsonOutput).contains("\"email\":\"john@example.com\"");
        assertThat(jsonOutput).contains("\"tags\":[\"developer\",\"java\",\"testing\"]");
    }

    @Test
    void json_recordObject_returnsCorrectJsonOutputStream() throws Exception {
        // Given
        TestRecord record = JsonTestHelper.createSampleRecord();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        // When
        Result.OutputStreamRenderer renderer = json.json(record);
        renderer.streamTo(outputStream);

        // Then
        String jsonOutput = outputStream.toString(StandardCharsets.UTF_8);
        assertThat(jsonOutput).contains("\"id\":\"rec-123\"");
        assertThat(jsonOutput).contains("\"description\":\"Sample test record\"");
        assertThat(jsonOutput).contains("\"scores\":[95,87,92]");
    }

    @Test
    void json_nullObject_returnsEmptyJsonOutput() throws Exception {
        // Given
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        // When
        Result.OutputStreamRenderer renderer = json.json(null);
        renderer.streamTo(outputStream);

        // Then
        String jsonOutput = outputStream.toString(StandardCharsets.UTF_8);
        assertThat(jsonOutput).isEqualTo("null");
    }

    @Test
    void json_writeFailure_handlesIOExceptionGracefully() throws Exception {
        // Given
        TestPerson person = JsonTestHelper.createSamplePerson();
        ByteArrayOutputStream failingOutputStream = new ByteArrayOutputStream() {
            @Override
            public void write(byte[] b) throws IOException {
                throw new IOException("Simulated write error");
            }
        };

        // When/Then - Should not throw exception
        Result.OutputStreamRenderer renderer = json.json(person);
        assertDoesNotThrow(() -> renderer.streamTo(failingOutputStream));
    }

    @Test
    void json_stringObject_returnsCorrectJsonOutputStream() throws Exception {
        // Given
        String message = "Hello, World!";
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        // When
        Result.OutputStreamRenderer renderer = json.json(message);
        renderer.streamTo(outputStream);

        // Then
        String jsonOutput = outputStream.toString(StandardCharsets.UTF_8);
        assertThat(jsonOutput).isEqualTo("\"Hello, World!\"");
    }

    @Test
    void json_collectionObject_returnsCorrectJsonOutputStream() throws Exception {
        // Given
        List<String> items = List.of("item1", "item2", "item3");
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        // When
        Result.OutputStreamRenderer renderer = json.json(items);
        renderer.streamTo(outputStream);

        // Then
        String jsonOutput = outputStream.toString(StandardCharsets.UTF_8);
        assertThat(jsonOutput).isEqualTo("[\"item1\",\"item2\",\"item3\"]");
    }

    private Request createTestRequest(Request.InputStreamGetter inputStreamGetter) {
        return TestRequest.builderFromBasic()
                .inputStreamGetter(inputStreamGetter)
                .build();
    }

    private String serializeToJson(Object object) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        new Json().json(object).streamTo(baos);
        return baos.toString(StandardCharsets.UTF_8);
    }

    private void assertDoesNotThrow(Runnable runnable) {
        try {
            runnable.run();
        } catch (Exception e) {
            throw new AssertionError("Expected no exception, but got: " + e, e);
        }
    }
}