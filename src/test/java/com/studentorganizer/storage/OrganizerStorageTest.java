package com.studentorganizer.storage;

import com.studentorganizer.model.Assignment;
import com.studentorganizer.model.StudySession;
import com.studentorganizer.model.Task;
import com.studentorganizer.model.TaskStatus;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OrganizerStorageTest {

    @Test
    void saveAndLoad_roundTripsAssignmentsTasksAndSessions() throws Exception {
        Path tempFile = Files.createTempFile("organizer", ".txt");
        OrganizerStorage storage = new OrganizerStorage(tempFile);

        Task task = new Task("t1", "Draft intro", TaskStatus.IN_PROGRESS, 30, 10, new ArrayList<>());
        task.getSessions().add(new StudySession("t1", LocalDateTime.of(2026, 5, 12, 9, 0), LocalDateTime.of(2026, 5, 12, 9, 10), 10));

        Assignment assignment = new Assignment("a1", "Essay", "English", LocalDate.of(2026, 5, 20), 120, new ArrayList<>(List.of(task)));
        storage.save(List.of(assignment));

        List<Assignment> loaded = storage.load();
        assertEquals(1, loaded.size());
        assertEquals("Essay", loaded.get(0).getTitle());
        assertEquals(1, loaded.get(0).getTasks().size());
        assertEquals(1, loaded.get(0).getTasks().get(0).getSessions().size());
        assertEquals(10, loaded.get(0).getTasks().get(0).getSpentMinutes());
    }
}
