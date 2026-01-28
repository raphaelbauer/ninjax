package org.ninjax.json.testhelper;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Test helper classes for JSON serialization/deserialization tests
 */
public class JsonTestHelper {

    public static class TestPerson {
        private String name;
        private int age;
        private Optional<String> email;
        private LocalDateTime createdAt;
        private List<String> tags;

        public TestPerson() {}

        public TestPerson(String name, int age, Optional<String> email, LocalDateTime createdAt, List<String> tags) {
            this.name = name;
            this.age = age;
            this.email = email;
            this.createdAt = createdAt;
            this.tags = tags;
        }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public int getAge() { return age; }
        public void setAge(int age) { this.age = age; }

        public Optional<String> getEmail() { return email; }
        public void setEmail(Optional<String> email) { this.email = email; }

        public LocalDateTime getCreatedAt() { return createdAt; }
        public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

        public List<String> getTags() { return tags; }
        public void setTags(List<String> tags) { this.tags = tags; }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (!(obj instanceof TestPerson)) return false;
            TestPerson other = (TestPerson) obj;
            return java.util.Objects.equals(name, other.name) &&
                   age == other.age &&
                   java.util.Objects.equals(email, other.email) &&
                   java.util.Objects.equals(createdAt, other.createdAt) &&
                   java.util.Objects.equals(tags, other.tags);
        }

        @Override
        public int hashCode() {
            return java.util.Objects.hash(name, age, email, createdAt, tags);
        }
    }

    public record TestRecord(
        String id,
        Optional<String> description,
        LocalDateTime timestamp,
        List<Integer> scores
    ) {}

    public static TestPerson createSamplePerson() {
        return new TestPerson(
            "John Doe",
            30,
            Optional.of("john@example.com"),
            LocalDateTime.of(2024, 1, 15, 10, 30),
            List.of("developer", "java", "testing")
        );
    }

    public static TestRecord createSampleRecord() {
        return new TestRecord(
            "rec-123",
            Optional.of("Sample test record"),
            LocalDateTime.of(2024, 1, 15, 10, 30),
            List.of(95, 87, 92)
        );
    }

    public static String createValidPersonJson() {
        return """
            {
                "name": "Jane Smith",
                "age": 25,
                "email": "jane@example.com",
                "createdAt": "2024-01-20T14:45:30",
                "tags": ["designer", "ui", "ux"]
            }
            """;
    }

    public static String createValidRecordJson() {
        return """
            {
                "id": "record-456",
                "description": "Test record description",
                "timestamp": "2024-01-20T15:00:00",
                "scores": [100, 85, 90]
            }
            """;
    }

    public static String createInvalidJson() {
        return "{ invalid json syntax }";
    }

    public static String createTypeMismatchJson() {
        return """
            {
                "name": "Test",
                "age": "not-a-number",
                "email": null,
                "createdAt": "2024-01-20T14:45:30",
                "tags": "not-a-list"
            }
            """;
    }
}