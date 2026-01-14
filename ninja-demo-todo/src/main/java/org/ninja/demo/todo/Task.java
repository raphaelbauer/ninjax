package org.ninja.demo.todo;

import java.time.LocalDateTime;

public record Task(
    Long id,
    String title,
    String description,
    LocalDateTime created_at,
    boolean completed
) {

    public Task withId(Long id) {
        return new Task(id, this.title, this.description, this.created_at, this.completed);
    }
}