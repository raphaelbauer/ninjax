package org.ninjax.demo.todo.tasks;

import java.time.LocalDateTime;

public record Task(
        Long id,
        String title,
        String description,
        LocalDateTime createdAt,
        boolean completed
        ) {

    public Task withId(Long id) {
        return new Task(id, this.title, this.description, this.createdAt, this.completed);
    }
}
