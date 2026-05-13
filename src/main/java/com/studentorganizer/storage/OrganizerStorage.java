package com.studentorganizer.storage;

import com.studentorganizer.model.Assignment;
import com.studentorganizer.model.StudySession;
import com.studentorganizer.model.Task;
import com.studentorganizer.model.TaskStatus;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class OrganizerStorage {
    private final Path dataPath;

    public OrganizerStorage(Path dataPath) {
        this.dataPath = dataPath;
    }

    public List<Assignment> load() throws IOException {
        if (!Files.exists(dataPath)) {
            return new ArrayList<>();
        }

        Map<String, Assignment> assignments = new LinkedHashMap<>();
        Map<String, Task> tasks = new LinkedHashMap<>();
        Map<String, String> taskToAssignment = new LinkedHashMap<>();

        for (String line : Files.readAllLines(dataPath, StandardCharsets.UTF_8)) {
            if (line.isBlank()) {
                continue;
            }
            String[] parts = line.split("\\|", -1);
            switch (parts[0]) {
                case "A" -> {
                    Assignment assignment = new Assignment(
                            parts[1],
                            decode(parts[2]),
                            decode(parts[3]),
                            LocalDate.parse(parts[4]),
                            Integer.parseInt(parts[5]),
                            new ArrayList<>()
                    );
                    assignments.put(assignment.getId(), assignment);
                }
                case "T" -> {
                    Task task = new Task(
                            parts[2],
                            decode(parts[3]),
                            TaskStatus.valueOf(parts[4]),
                            Integer.parseInt(parts[5]),
                            Integer.parseInt(parts[6]),
                            new ArrayList<>()
                    );
                    tasks.put(task.getId(), task);
                    taskToAssignment.put(task.getId(), parts[1]);
                }
                case "S" -> {
                    Task task = tasks.get(parts[1]);
                    if (task != null) {
                        StudySession session = new StudySession(
                                task.getId(),
                                LocalDateTime.parse(parts[2]),
                                LocalDateTime.parse(parts[3]),
                                Integer.parseInt(parts[4])
                        );
                        task.getSessions().add(session);
                    }
                }
                default -> {
                }
            }
        }

        for (Map.Entry<String, Task> entry : tasks.entrySet()) {
            String assignmentId = taskToAssignment.get(entry.getKey());
            Assignment assignment = assignments.get(assignmentId);
            if (assignment != null) {
                assignment.getTasks().add(entry.getValue());
            }
        }

        return new ArrayList<>(assignments.values());
    }

    public void save(List<Assignment> assignments) throws IOException {
        Path parent = dataPath.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }

        List<String> lines = new ArrayList<>();
        for (Assignment assignment : assignments) {
            lines.add(String.join("|",
                    "A",
                    assignment.getId(),
                    encode(assignment.getTitle()),
                    encode(assignment.getSubject()),
                    assignment.getDueDate().toString(),
                    Integer.toString(assignment.getEstimatedMinutes())
            ));
            for (Task task : assignment.getTasks()) {
                lines.add(String.join("|",
                        "T",
                        assignment.getId(),
                        task.getId(),
                        encode(task.getTitle()),
                        task.getStatus().name(),
                        Integer.toString(task.getEstimatedMinutes()),
                        Integer.toString(task.getSpentMinutes())
                ));
                for (StudySession session : task.getSessions()) {
                    lines.add(String.join("|",
                            "S",
                            task.getId(),
                            session.getStartTime().toString(),
                            session.getEndTime().toString(),
                            Integer.toString(session.getMinutes())
                    ));
                }
            }
        }
        Files.write(dataPath, lines, StandardCharsets.UTF_8);
    }

    private String encode(String value) {
        return Base64.getEncoder().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    private String decode(String value) {
        return new String(Base64.getDecoder().decode(value), StandardCharsets.UTF_8);
    }
}
