package com.studentorganizer.model;

import java.time.LocalDateTime;

public class ActiveTimer {
    private final String taskId;
    private final LocalDateTime startedAt;
    private LocalDateTime runningSince;
    private long accumulatedSeconds;
    private boolean running;

    public ActiveTimer(String taskId, LocalDateTime startedAt) {
        this.taskId = taskId;
        this.startedAt = startedAt;
        this.runningSince = startedAt;
        this.accumulatedSeconds = 0;
        this.running = true;
    }

    public String getTaskId() {
        return taskId;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public LocalDateTime getRunningSince() {
        return runningSince;
    }

    public long getAccumulatedSeconds() {
        return accumulatedSeconds;
    }

    public boolean isRunning() {
        return running;
    }

    public void setRunningSince(LocalDateTime runningSince) {
        this.runningSince = runningSince;
    }

    public void setAccumulatedSeconds(long accumulatedSeconds) {
        this.accumulatedSeconds = accumulatedSeconds;
    }

    public void setRunning(boolean running) {
        this.running = running;
    }
}
