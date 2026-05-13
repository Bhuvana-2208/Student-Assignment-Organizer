package com.studentorganizer.service;

import com.studentorganizer.model.ActiveTimer;
import com.studentorganizer.model.Assignment;
import com.studentorganizer.model.StudySession;
import com.studentorganizer.model.Task;
import com.studentorganizer.model.TaskStatus;
import com.studentorganizer.storage.OrganizerStorage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class OrganizerService {
    private final OrganizerStorage storage;
    private final Clock clock;
    private List<Assignment> assignments;
    private ActiveTimer activeTimer;

    public OrganizerService(OrganizerStorage storage) {
        this(storage, Clock.systemDefaultZone());
    }

    public OrganizerService(OrganizerStorage storage, Clock clock) {
        this.storage = storage;
        this.clock = clock;
        this.assignments = new ArrayList<>();
    }

    public void load() throws IOException {
        assignments = storage.load();
    }

    public void save() throws IOException {
        storage.save(assignments);
    }

    public List<Assignment> getAssignments() {
        return assignments;
    }

    public Assignment createAssignment(String title, String subject, String dueDateText, String estimatedMinutesText) {
        String cleanTitle = required(title, "Assignment title is required.");
        String cleanSubject = required(subject, "Subject is required.");
        LocalDate dueDate = parseDate(dueDateText);
        int estimated = parseMinutes(estimatedMinutesText, "Estimated minutes must be a positive integer.");

        Assignment assignment = new Assignment(cleanTitle, cleanSubject, dueDate, estimated);
        assignments.add(assignment);
        return assignment;
    }

    public Task createTask(Assignment assignment, String title, String estimatedMinutesText) {
        if (assignment == null) {
            throw new ValidationException("Select an assignment first.");
        }
        String cleanTitle = required(title, "Task title is required.");
        int estimated = parseMinutes(estimatedMinutesText, "Task estimated minutes must be a positive integer.");

        Task task = new Task(cleanTitle, estimated);
        assignment.getTasks().add(task);
        return task;
    }

    public void startTimer(Task task) {
        if (task == null) {
            throw new ValidationException("Select a task first.");
        }
        if (activeTimer != null) {
            throw new ValidationException("A timer is already active. Stop it before starting another.");
        }
        LocalDateTime now = LocalDateTime.now(clock);
        activeTimer = new ActiveTimer(task.getId(), now);
        task.setStatus(TaskStatus.IN_PROGRESS);
    }

    public void pauseTimer() {
        ActiveTimer timer = requireActiveTimer();
        if (!timer.isRunning()) {
            throw new ValidationException("Timer is already paused.");
        }
        long elapsed = Duration.between(timer.getRunningSince(), LocalDateTime.now(clock)).getSeconds();
        timer.setAccumulatedSeconds(timer.getAccumulatedSeconds() + Math.max(0, elapsed));
        timer.setRunning(false);
    }

    public void resumeTimer() {
        ActiveTimer timer = requireActiveTimer();
        if (timer.isRunning()) {
            throw new ValidationException("Timer is already running.");
        }
        timer.setRunning(true);
        timer.setRunningSince(LocalDateTime.now(clock));
    }

    public void stopTimer() {
        ActiveTimer timer = requireActiveTimer();
        long seconds = timer.getAccumulatedSeconds();
        LocalDateTime now = LocalDateTime.now(clock);
        if (timer.isRunning()) {
            seconds += Math.max(0, Duration.between(timer.getRunningSince(), now).getSeconds());
        }

        Task task = findTaskById(timer.getTaskId());
        if (task == null) {
            activeTimer = null;
            throw new ValidationException("Active task not found.");
        }

        int minutes = (int) Math.max(1, Math.round(seconds / 60.0));
        task.addSession(new StudySession(task.getId(), timer.getStartedAt(), now, minutes));
        if (task.getSpentMinutes() >= task.getEstimatedMinutes()) {
            task.setStatus(TaskStatus.DONE);
        }
        activeTimer = null;
    }

    public long getActiveElapsedSeconds() {
        if (activeTimer == null) {
            return 0;
        }
        long seconds = activeTimer.getAccumulatedSeconds();
        if (activeTimer.isRunning()) {
            seconds += Math.max(0, Duration.between(activeTimer.getRunningSince(), LocalDateTime.now(clock)).getSeconds());
        }
        return seconds;
    }

    public Map<String, Integer> buildDailySummary(LocalDate date) {
        Map<String, Integer> summary = new LinkedHashMap<>();
        for (Assignment assignment : assignments) {
            int total = assignment.getTasks().stream()
                    .flatMap(task -> task.getSessions().stream())
                    .filter(session -> session.getStartTime().toLocalDate().equals(date))
                    .mapToInt(StudySession::getMinutes)
                    .sum();
            if (total > 0) {
                summary.put(assignment.getTitle(), total);
            }
        }
        return summary;
    }

    public Map<LocalDate, Integer> buildWeeklySummary(LocalDate selectedDate) {
        LocalDate weekStart = selectedDate.with(DayOfWeek.MONDAY);
        LocalDate weekEnd = weekStart.plusDays(6);
        Map<LocalDate, Integer> weekly = new LinkedHashMap<>();
        for (int i = 0; i < 7; i++) {
            weekly.put(weekStart.plusDays(i), 0);
        }

        for (Assignment assignment : assignments) {
            for (Task task : assignment.getTasks()) {
                for (StudySession session : task.getSessions()) {
                    LocalDate day = session.getStartTime().toLocalDate();
                    if (!day.isBefore(weekStart) && !day.isAfter(weekEnd)) {
                        weekly.put(day, weekly.get(day) + session.getMinutes());
                    }
                }
            }
        }
        return weekly;
    }

    public void exportWeeklyReport(LocalDate selectedDate, Path filePath) throws IOException {
        Map<LocalDate, Integer> weekly = buildWeeklySummary(selectedDate);
        List<String> lines = new ArrayList<>();
        lines.add("Weekly Study Report");
        lines.add("Week of " + selectedDate.with(DayOfWeek.MONDAY));
        lines.add("");
        weekly.forEach((date, minutes) -> lines.add(date + ": " + minutes + " min"));
        Files.write(filePath, lines);
    }

    private ActiveTimer requireActiveTimer() {
        if (activeTimer == null) {
            throw new ValidationException("No active timer found.");
        }
        return activeTimer;
    }

    private Task findTaskById(String taskId) {
        for (Assignment assignment : assignments) {
            for (Task task : assignment.getTasks()) {
                if (task.getId().equals(taskId)) {
                    return task;
                }
            }
        }
        return null;
    }

    private String required(String value, String message) {
        if (value == null || value.trim().isEmpty()) {
            throw new ValidationException(message);
        }
        return value.trim();
    }

    private LocalDate parseDate(String dueDateText) {
        try {
            return LocalDate.parse(required(dueDateText, "Due date is required (YYYY-MM-DD)."));
        } catch (DateTimeParseException ex) {
            throw new ValidationException("Due date must be in YYYY-MM-DD format.");
        }
    }

    private int parseMinutes(String text, String message) {
        try {
            int value = Integer.parseInt(required(text, message));
            if (value <= 0) {
                throw new ValidationException(message);
            }
            return value;
        } catch (NumberFormatException ex) {
            throw new ValidationException(message);
        }
    }

    public static String formatDuration(long seconds) {
        long h = seconds / 3600;
        long m = (seconds % 3600) / 60;
        long s = seconds % 60;
        return String.format("%02d:%02d:%02d", h, m, s);
    }
}
