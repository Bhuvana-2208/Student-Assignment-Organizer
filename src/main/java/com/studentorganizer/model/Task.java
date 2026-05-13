package com.studentorganizer.model;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Task {
    private final String id;
    private final String title;
    private TaskStatus status;
    private final int estimatedMinutes;
    private int spentMinutes;
    private final List<StudySession> sessions;

    public Task(String title, int estimatedMinutes) {
        this(UUID.randomUUID().toString(), title, TaskStatus.TODO, estimatedMinutes, 0, new ArrayList<>());
    }

    public Task(String id, String title, TaskStatus status, int estimatedMinutes, int spentMinutes, List<StudySession> sessions) {
        this.id = id;
        this.title = title;
        this.status = status;
        this.estimatedMinutes = estimatedMinutes;
        this.spentMinutes = spentMinutes;
        this.sessions = sessions;
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public TaskStatus getStatus() {
        return status;
    }

    public int getEstimatedMinutes() {
        return estimatedMinutes;
    }

    public int getSpentMinutes() {
        return spentMinutes;
    }

    public List<StudySession> getSessions() {
        return sessions;
    }

    public void setStatus(TaskStatus status) {
        this.status = status;
    }

    public void addSession(StudySession session) {
        this.sessions.add(session);
        this.spentMinutes += session.getMinutes();
    }

    @Override
    public String toString() {
        return title;
    }
}
