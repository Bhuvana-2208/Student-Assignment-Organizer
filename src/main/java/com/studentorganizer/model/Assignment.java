package com.studentorganizer.model;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Assignment {
    private final String id;
    private final String title;
    private final String subject;
    private final LocalDate dueDate;
    private final int estimatedMinutes;
    private final List<Task> tasks;

    public Assignment(String title, String subject, LocalDate dueDate, int estimatedMinutes) {
        this(UUID.randomUUID().toString(), title, subject, dueDate, estimatedMinutes, new ArrayList<>());
    }

    public Assignment(String id, String title, String subject, LocalDate dueDate, int estimatedMinutes, List<Task> tasks) {
        this.id = id;
        this.title = title;
        this.subject = subject;
        this.dueDate = dueDate;
        this.estimatedMinutes = estimatedMinutes;
        this.tasks = tasks;
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getSubject() {
        return subject;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public int getEstimatedMinutes() {
        return estimatedMinutes;
    }

    public List<Task> getTasks() {
        return tasks;
    }

    public int getTotalSpentMinutes() {
        return tasks.stream().mapToInt(Task::getSpentMinutes).sum();
    }

    @Override
    public String toString() {
        return title + " (" + subject + ")";
    }
}
