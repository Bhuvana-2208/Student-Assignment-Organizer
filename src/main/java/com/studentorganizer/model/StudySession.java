package com.studentorganizer.model;

import java.time.LocalDateTime;

public class StudySession {
    private final String taskId;
    private final LocalDateTime startTime;
    private final LocalDateTime endTime;
    private final int minutes;

    public StudySession(String taskId, LocalDateTime startTime, LocalDateTime endTime, int minutes) {
        this.taskId = taskId;
        this.startTime = startTime;
        this.endTime = endTime;
        this.minutes = minutes;
    }

    public String getTaskId() {
        return taskId;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public int getMinutes() {
        return minutes;
    }
}
