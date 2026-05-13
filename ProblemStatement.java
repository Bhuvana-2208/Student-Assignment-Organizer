
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ProblemStatement {
	private static final String DATA_FILE = "assignment_data.txt";
	private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ISO_LOCAL_DATE;

	private final BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
	private final Map<Integer, Assignment> assignments = new LinkedHashMap<>();
	private final List<StudySession> sessions = new ArrayList<>();

	private int nextAssignmentId = 1;
	private int nextTaskId = 1;
	private ActiveTimer activeTimer;

	public static void main(String[] args) {
		new ProblemStatement().run();
	}

	private void run() {
		System.out.println("Student Assignment Organizer - Track your study time and stay focused!");
		load();

		while (true) {
			printMenu();
			String choice = prompt("Choose an option: ");
			if (choice == null) {
				continue;
			}
			switch (choice) {
				case "1":
					addAssignment();
					break;
				case "2":
					listAssignments();
					break;
				case "3":
					editAssignment();
					break;
				case "4":
					deleteAssignment();
					break;
				case "5":
					addTaskToAssignment();
					break;
				case "6":
					listTasksForAssignment();
					break;
				case "7":
					editTask();
					break;
				case "8":
					deleteTask();
					break;
				case "9":
					startTimer();
					break;
				case "10":
					pauseTimer();
					break;
				case "11":
					resumeTimer();
					break;
				case "12":
					stopTimer();
					break;
				case "13":
					markTaskDone();
					break;
				case "14":
					showDailySummary();
					break;
				case "15":
					exportWeeklyReport();
					break;
				case "16":
					if (handleExit()) {
						return;
					}
					break;
				default:
					System.out.println("Invalid option.");
					break;
			}
		}
	}

	private void printMenu() {
		System.out.println();
		String timerStatus = getActiveTimerStatus();
		if (timerStatus != null) {
			System.out.println("Active timer: " + timerStatus);
		} else {
			System.out.println("Active timer: none");
		}
		System.out.println("1) Add assignment");
		System.out.println("2) List assignments");
		System.out.println("3) Edit assignment");
		System.out.println("4) Delete assignment");
		System.out.println("5) Add task to assignment");
		System.out.println("6) List tasks for assignment");
		System.out.println("7) Edit task");
		System.out.println("8) Delete task");
		System.out.println("9) Start timer for task");
		System.out.println("10) Pause timer");
		System.out.println("11) Resume timer");
		System.out.println("12) Stop active timer");
		System.out.println("13) Mark task done");
		System.out.println("14) Show daily summary");
		System.out.println("15) Export weekly report");
		System.out.println("16) Save and exit");
	}

	private void addAssignment() {
		String title = promptRequired("Title: ");
		if (title == null) {
			return;
		}
		String subject = promptRequired("Subject: ");
		if (subject == null) {
			return;
		}
		LocalDate dueDate = promptDate("Due date (YYYY-MM-DD): ");
		if (dueDate == null) {
			return;
		}
		Double hours = promptDouble("Estimated hours (blank for 0): ", true);
		int estMinutes = hours == null ? 0 : (int) Math.round(hours * 60.0);

		Assignment assignment = new Assignment(nextAssignmentId++, title, subject, dueDate, estMinutes);
		assignments.put(assignment.id, assignment);
		save();
		System.out.println("Added assignment with id " + assignment.id + ".");
	}

	private void listAssignments() {
		if (assignments.isEmpty()) {
			System.out.println("No assignments yet.");
			return;
		}
		System.out.println("Assignments:");
		for (Assignment assignment : assignments.values()) {
			int spent = assignment.totalSpentMinutes();
			int est = assignment.estMinutes;
			String overdue = assignment.dueDate.isBefore(LocalDate.now()) ? " (overdue)" : "";
			System.out.println(
				"ID " + assignment.id + " | " + assignment.title + " | " + assignment.subject +
					" | Due " + assignment.dueDate + overdue +
					" | Tasks " + assignment.tasks.size() +
					" | Spent " + spent + "m" +
					" | Est " + est + "m"
			);
		}
	}

	private void editAssignment() {
		Assignment assignment = selectAssignment();
		if (assignment == null) {
			return;
		}
		String title = promptWithDefault("Title [" + assignment.title + "]: ", assignment.title);
		if (title == null) {
			return;
		}
		String subject = promptWithDefault("Subject [" + assignment.subject + "]: ", assignment.subject);
		if (subject == null) {
			return;
		}
		LocalDate dueDate = promptDateWithDefault(
			"Due date (YYYY-MM-DD) [" + assignment.dueDate + "]: ", assignment.dueDate
		);
		if (dueDate == null) {
			return;
		}
		double currentHours = assignment.estMinutes / 60.0;
		Double hours = promptDoubleWithDefault(
			"Estimated hours [" + formatHours(currentHours) + "]: ", currentHours
		);
		if (hours == null) {
			return;
		}
		assignment.title = title;
		assignment.subject = subject;
		assignment.dueDate = dueDate;
		assignment.estMinutes = (int) Math.round(hours * 60.0);
		save();
		System.out.println("Assignment updated.");
	}

	private void deleteAssignment() {
		Assignment assignment = selectAssignment();
		if (assignment == null) {
			return;
		}
		if (activeTimer != null && isTaskInAssignment(activeTimer.taskId, assignment.id)) {
			String answer = prompt("Active timer is in this assignment. Stop and save? (y/n): ");
			if (answer != null && answer.equalsIgnoreCase("y")) {
				stopTimer();
			} else {
				System.out.println("Delete canceled.");
				return;
			}
		}
		String confirm = prompt("Delete assignment '" + assignment.title + "' and all tasks? (y/n): ");
		if (confirm == null || !confirm.equalsIgnoreCase("y")) {
			return;
		}
		List<Integer> taskIds = new ArrayList<>();
		for (Task task : assignment.tasks) {
			taskIds.add(task.id);
		}
		assignments.remove(assignment.id);
		if (!taskIds.isEmpty()) {
			List<StudySession> kept = new ArrayList<>();
			for (StudySession session : sessions) {
				if (!taskIds.contains(session.taskId)) {
					kept.add(session);
				}
			}
			sessions.clear();
			sessions.addAll(kept);
		}
		save();
		System.out.println("Assignment deleted.");
	}

	private void addTaskToAssignment() {
		Assignment assignment = selectAssignment();
		if (assignment == null) {
			return;
		}
		String title = promptRequired("Task title: ");
		if (title == null) {
			return;
		}
		Integer estMinutes = promptInt("Estimated minutes (blank for 0): ", true);
		int minutes = estMinutes == null ? 0 : estMinutes;
		Task task = new Task(nextTaskId++, assignment.id, title, TaskStatus.TODO, minutes, 0);
		assignment.tasks.add(task);
		save();
		System.out.println("Added task with id " + task.id + ".");
	}

	private void listTasksForAssignment() {
		Assignment assignment = selectAssignment();
		if (assignment == null) {
			return;
		}
		if (assignment.tasks.isEmpty()) {
			System.out.println("No tasks for this assignment.");
			return;
		}
		System.out.println("Tasks for assignment " + assignment.title + ":");
		for (Task task : assignment.tasks) {
			System.out.println(
				"ID " + task.id + " | " + task.title +
					" | " + task.status +
					" | Spent " + task.spentMinutes + "m" +
					" | Est " + task.estMinutes + "m"
			);
		}
	}

	private void editTask() {
		Task task = selectTask();
		if (task == null) {
			return;
		}
		String title = promptWithDefault("Task title [" + task.title + "]: ", task.title);
		if (title == null) {
			return;
		}
		Integer estMinutes = promptIntWithDefault(
			"Estimated minutes [" + task.estMinutes + "]: ", task.estMinutes
		);
		if (estMinutes == null) {
			return;
		}
		TaskStatus status = promptStatusWithDefault(
			"Status (TODO/DONE) [" + task.status + "]: ", task.status
		);
		if (status == null) {
			return;
		}
		task.title = title;
		task.estMinutes = estMinutes;
		task.status = status;
		save();
		System.out.println("Task updated.");
	}

	private void deleteTask() {
		Task task = selectTask();
		if (task == null) {
			return;
		}
		if (activeTimer != null && activeTimer.taskId == task.id) {
			String answer = prompt("Active timer is for this task. Stop and save? (y/n): ");
			if (answer != null && answer.equalsIgnoreCase("y")) {
				stopTimer();
			} else {
				System.out.println("Delete canceled.");
				return;
			}
		}
		String confirm = prompt("Delete task '" + task.title + "'? (y/n): ");
		if (confirm == null || !confirm.equalsIgnoreCase("y")) {
			return;
		}
		Assignment assignment = assignments.get(task.assignmentId);
		if (assignment != null) {
			assignment.tasks.removeIf(item -> item.id == task.id);
		}
		sessions.removeIf(session -> session.taskId == task.id);
		save();
		System.out.println("Task deleted.");
	}

	private void startTimer() {
		if (activeTimer != null) {
			System.out.println("A timer already exists. Pause, resume, or stop it first.");
			return;
		}
		Task task = selectTask();
		if (task == null) {
			return;
		}
		activeTimer = new ActiveTimer(task.id, System.currentTimeMillis());
		System.out.println("Timer started for task " + task.id + " (" + task.title + ").");
	}

	private void pauseTimer() {
		if (activeTimer == null) {
			System.out.println("No active timer.");
			return;
		}
		if (activeTimer.paused) {
			System.out.println("Timer is already paused.");
			return;
		}
		long now = System.currentTimeMillis();
		activeTimer.accumulatedMillis += Math.max(0, now - activeTimer.lastStartMillis);
		activeTimer.paused = true;
		activeTimer.lastStartMillis = 0L;
		System.out.println("Timer paused.");
	}

	private void resumeTimer() {
		if (activeTimer == null) {
			System.out.println("No active timer.");
			return;
		}
		if (!activeTimer.paused) {
			System.out.println("Timer is already running.");
			return;
		}
		activeTimer.lastStartMillis = System.currentTimeMillis();
		activeTimer.paused = false;
		System.out.println("Timer resumed.");
	}

	private void stopTimer() {
		if (activeTimer == null) {
			System.out.println("No active timer.");
			return;
		}
		Task task = findTaskById(activeTimer.taskId);
		if (task == null) {
			System.out.println("Active task not found. Timer discarded.");
			activeTimer = null;
			return;
		}
		long endMillis = System.currentTimeMillis();
		long durationMillis = getActiveTimerElapsedMillis(endMillis);
		int durationMinutes = (int) Math.max(1, (durationMillis + 59999) / 60000);
		task.spentMinutes += durationMinutes;
		sessions.add(new StudySession(task.id, activeTimer.sessionStartMillis, endMillis, durationMinutes));
		activeTimer = null;
		save();
		System.out.println("Timer stopped. Added " + durationMinutes + " minute(s) to task " + task.id + ".");
	}

	private void markTaskDone() {
		Task task = selectTask();
		if (task == null) {
			return;
		}
		task.status = TaskStatus.DONE;
		save();
		System.out.println("Task " + task.id + " marked as DONE.");
	}

	private void showDailySummary() {
		if (sessions.isEmpty()) {
			System.out.println("No study sessions logged yet.");
			return;
		}
		LocalDate today = LocalDate.now();
		Map<Integer, Integer> minutesByAssignment = new LinkedHashMap<>();
		int total = 0;
		for (StudySession session : sessions) {
			LocalDate sessionDate = Instant.ofEpochMilli(session.endMillis)
				.atZone(ZoneId.systemDefault())
				.toLocalDate();
			if (!sessionDate.equals(today)) {
				continue;
			}
			Task task = findTaskById(session.taskId);
			if (task == null) {
				continue;
			}
			minutesByAssignment.merge(task.assignmentId, session.durationMinutes, Integer::sum);
			total += session.durationMinutes;
		}
		if (minutesByAssignment.isEmpty()) {
			System.out.println("No sessions for today.");
			return;
		}
		System.out.println("Today summary:");
		for (Map.Entry<Integer, Integer> entry : minutesByAssignment.entrySet()) {
			Assignment assignment = assignments.get(entry.getKey());
			String title = assignment == null ? "Unknown" : assignment.title;
			System.out.println("- " + title + ": " + entry.getValue() + "m");
		}
		System.out.println("Total today: " + total + "m");
	}

	private void exportWeeklyReport() {
		LocalDate today = LocalDate.now();
		LocalDate start = today.minusDays(6);
		Map<LocalDate, Integer> dailyTotals = new LinkedHashMap<>();
		for (int i = 0; i < 7; i++) {
			dailyTotals.put(start.plusDays(i), 0);
		}
		Map<Integer, Integer> assignmentTotals = new LinkedHashMap<>();
		int totalMinutes = 0;
		for (StudySession session : sessions) {
			LocalDate sessionDate = Instant.ofEpochMilli(session.endMillis)
				.atZone(ZoneId.systemDefault())
				.toLocalDate();
			if (sessionDate.isBefore(start) || sessionDate.isAfter(today)) {
				continue;
			}
			totalMinutes += session.durationMinutes;
			dailyTotals.put(sessionDate, dailyTotals.get(sessionDate) + session.durationMinutes);
			Task task = findTaskById(session.taskId);
			if (task != null) {
				assignmentTotals.merge(task.assignmentId, session.durationMinutes, Integer::sum);
			}
		}
		List<String> lines = new ArrayList<>();
		lines.add("Weekly Report");
		lines.add("Range: " + start + " to " + today);
		lines.add("Total minutes: " + totalMinutes);
		lines.add("");
		lines.add("Daily totals:");
		for (Map.Entry<LocalDate, Integer> entry : dailyTotals.entrySet()) {
			lines.add(entry.getKey() + ": " + entry.getValue() + "m");
		}
		lines.add("");
		lines.add("By assignment:");
		if (assignmentTotals.isEmpty()) {
			lines.add("No assignment sessions.");
		} else {
			for (Map.Entry<Integer, Integer> entry : assignmentTotals.entrySet()) {
				Assignment assignment = assignments.get(entry.getKey());
				String title = assignment == null ? "Unknown" : assignment.title;
				lines.add(title + " (ID " + entry.getKey() + "): " + entry.getValue() + "m");
			}
		}
		String fileName = "weekly_report_" + today + ".txt";
		Path path = Paths.get(fileName);
		try {
			Files.write(path, lines, StandardCharsets.UTF_8,
				StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
			System.out.println("Weekly report saved to " + fileName);
		} catch (IOException e) {
			System.out.println("Failed to write report: " + e.getMessage());
		}
	}

	private boolean handleExit() {
		if (activeTimer != null) {
			Task task = findTaskById(activeTimer.taskId);
			String taskName = task == null ? "unknown" : task.title;
			String answer = prompt("Timer running for task '" + taskName + "'. Stop and save? (y/n): ");
			if (answer != null && answer.equalsIgnoreCase("y")) {
				stopTimer();
				return true;
			}
			String confirm = prompt("Exit without stopping timer? (y/n): ");
			if (confirm == null || !confirm.equalsIgnoreCase("y")) {
				return false;
			}
		}
		save();
		System.out.println("Saved. Goodbye.");
		return true;
	}

	private Assignment selectAssignment() {
		if (assignments.isEmpty()) {
			System.out.println("No assignments available.");
			return null;
		}
		listAssignments();
		Integer id = promptInt("Enter assignment id (blank to cancel): ", true);
		if (id == null) {
			return null;
		}
		Assignment assignment = assignments.get(id);
		if (assignment == null) {
			System.out.println("Assignment not found.");
		}
		return assignment;
	}

	private Task selectTask() {
		if (!listAllTasks()) {
			return null;
		}
		Integer id = promptInt("Enter task id (blank to cancel): ", true);
		if (id == null) {
			return null;
		}
		Task task = findTaskById(id);
		if (task == null) {
			System.out.println("Task not found.");
		}
		return task;
	}

	private boolean listAllTasks() {
		boolean any = false;
		for (Assignment assignment : assignments.values()) {
			for (Task task : assignment.tasks) {
				any = true;
				System.out.println(
					"ID " + task.id + " | " + task.title +
						" | " + task.status +
						" | " + assignment.title +
						" | Spent " + task.spentMinutes + "m" +
						" | Est " + task.estMinutes + "m"
				);
			}
		}
		if (!any) {
			System.out.println("No tasks available.");
		}
		return any;
	}

	private Task findTaskById(int id) {
		for (Assignment assignment : assignments.values()) {
			for (Task task : assignment.tasks) {
				if (task.id == id) {
					return task;
				}
			}
		}
		return null;
	}

	private String getActiveTimerStatus() {
		if (activeTimer == null) {
			return null;
		}
		Task task = findTaskById(activeTimer.taskId);
		long elapsedMillis = getActiveTimerElapsedMillis(System.currentTimeMillis());
		String elapsed = formatDuration(elapsedMillis);
		String taskName = task == null ? "unknown" : task.title;
		String status = activeTimer.paused ? "paused" : "running";
		return "Task " + activeTimer.taskId + " (" + taskName + ") " + elapsed + " [" + status + "]";
	}

	private long getActiveTimerElapsedMillis(long nowMillis) {
		if (activeTimer == null) {
			return 0L;
		}
		if (activeTimer.paused) {
			return activeTimer.accumulatedMillis;
		}
		return activeTimer.accumulatedMillis + Math.max(0L, nowMillis - activeTimer.lastStartMillis);
	}

	private String formatDuration(long millis) {
		Duration duration = Duration.ofMillis(Math.max(0, millis));
		long hours = duration.toHours();
		long minutes = duration.toMinutes() % 60;
		long seconds = duration.getSeconds() % 60;
		return String.format("%02d:%02d:%02d", hours, minutes, seconds);
	}

	private String prompt(String message) {
		try {
			System.out.print(message);
			String line = reader.readLine();
			if (line == null) {
				return null;
			}
			return line.trim();
		} catch (IOException e) {
			System.out.println("Input error: " + e.getMessage());
			return null;
		}
	}

	private String promptRequired(String message) {
		while (true) {
			String value = prompt(message);
			if (value == null) {
				return null;
			}
			if (!value.isEmpty()) {
				return value;
			}
			System.out.println("Value is required.");
		}
	}

	private String promptWithDefault(String message, String current) {
		String value = prompt(message);
		if (value == null) {
			return null;
		}
		if (value.isEmpty()) {
			return current;
		}
		return value;
	}

	private Integer promptInt(String message, boolean allowBlank) {
		while (true) {
			String value = prompt(message);
			if (value == null) {
				return null;
			}
			if (value.isEmpty() && allowBlank) {
				return null;
			}
			try {
				return Integer.parseInt(value);
			} catch (NumberFormatException e) {
				System.out.println("Please enter a valid number.");
			}
		}
	}

	private Double promptDouble(String message, boolean allowBlank) {
		while (true) {
			String value = prompt(message);
			if (value == null) {
				return null;
			}
			if (value.isEmpty() && allowBlank) {
				return null;
			}
			try {
				return Double.parseDouble(value);
			} catch (NumberFormatException e) {
				System.out.println("Please enter a valid number.");
			}
		}
	}

	private Integer promptIntWithDefault(String message, int current) {
		while (true) {
			String value = prompt(message);
			if (value == null) {
				return null;
			}
			if (value.isEmpty()) {
				return current;
			}
			try {
				return Integer.parseInt(value);
			} catch (NumberFormatException e) {
				System.out.println("Please enter a valid number.");
			}
		}
	}

	private Double promptDoubleWithDefault(String message, double current) {
		while (true) {
			String value = prompt(message);
			if (value == null) {
				return null;
			}
			if (value.isEmpty()) {
				return current;
			}
			try {
				return Double.parseDouble(value);
			} catch (NumberFormatException e) {
				System.out.println("Please enter a valid number.");
			}
		}
	}

	private LocalDate promptDateWithDefault(String message, LocalDate current) {
		while (true) {
			String value = prompt(message);
			if (value == null) {
				return null;
			}
			if (value.isEmpty()) {
				return current;
			}
			try {
				return LocalDate.parse(value, DATE_FMT);
			} catch (DateTimeParseException e) {
				System.out.println("Use format YYYY-MM-DD.");
			}
		}
	}

	private TaskStatus promptStatusWithDefault(String message, TaskStatus current) {
		while (true) {
			String value = prompt(message);
			if (value == null) {
				return null;
			}
			if (value.isEmpty()) {
				return current;
			}
			try {
				return TaskStatus.valueOf(value.trim().toUpperCase());
			} catch (IllegalArgumentException e) {
				System.out.println("Use TODO or DONE.");
			}
		}
	}

	private LocalDate promptDate(String message) {
		while (true) {
			String value = prompt(message);
			if (value == null) {
				return null;
			}
			if (value.isEmpty()) {
				System.out.println("Date is required.");
				continue;
			}
			try {
				return LocalDate.parse(value, DATE_FMT);
			} catch (DateTimeParseException e) {
				System.out.println("Use format YYYY-MM-DD.");
			}
		}
	}

	private boolean isTaskInAssignment(int taskId, int assignmentId) {
		Assignment assignment = assignments.get(assignmentId);
		if (assignment == null) {
			return false;
		}
		for (Task task : assignment.tasks) {
			if (task.id == taskId) {
				return true;
			}
		}
		return false;
	}

	private String formatHours(double hours) {
		double rounded = Math.round(hours * 100.0) / 100.0;
		return Double.toString(rounded);
	}

	private void load() {
		Path path = Paths.get(DATA_FILE);
		if (!Files.exists(path)) {
			return;
		}
		try {
			List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
			List<Task> pendingTasks = new ArrayList<>();
			for (String line : lines) {
				if (line.trim().isEmpty()) {
					continue;
				}
				List<String> parts = splitEscaped(line);
				if (parts.isEmpty()) {
					continue;
				}
				String type = parts.get(0);
				if ("META".equals(type) && parts.size() >= 3) {
					nextAssignmentId = parseIntSafe(parts.get(1), nextAssignmentId);
					nextTaskId = parseIntSafe(parts.get(2), nextTaskId);
				} else if ("ASSIGNMENT".equals(type) && parts.size() >= 6) {
					int id = parseIntSafe(parts.get(1), 0);
					String title = parts.get(2);
					String subject = parts.get(3);
					LocalDate dueDate = LocalDate.parse(parts.get(4), DATE_FMT);
					int estMinutes = parseIntSafe(parts.get(5), 0);
					Assignment assignment = new Assignment(id, title, subject, dueDate, estMinutes);
					assignments.put(id, assignment);
				} else if ("TASK".equals(type) && parts.size() >= 7) {
					int id = parseIntSafe(parts.get(1), 0);
					int assignmentId = parseIntSafe(parts.get(2), 0);
					String title = parts.get(3);
					TaskStatus status = TaskStatus.valueOf(parts.get(4));
					int estMinutes = parseIntSafe(parts.get(5), 0);
					int spentMinutes = parseIntSafe(parts.get(6), 0);
					pendingTasks.add(new Task(id, assignmentId, title, status, estMinutes, spentMinutes));
				} else if ("SESSION".equals(type) && parts.size() >= 5) {
					int taskId = parseIntSafe(parts.get(1), 0);
					long startMillis = parseLongSafe(parts.get(2), 0L);
					long endMillis = parseLongSafe(parts.get(3), 0L);
					int duration = parseIntSafe(parts.get(4), 0);
					sessions.add(new StudySession(taskId, startMillis, endMillis, duration));
				}
			}
			for (Task task : pendingTasks) {
				Assignment assignment = assignments.get(task.assignmentId);
				if (assignment != null) {
					assignment.tasks.add(task);
				}
			}
			if (nextAssignmentId <= 0 || nextTaskId <= 0) {
				recalcIds();
			}
		} catch (IOException e) {
			System.out.println("Failed to load data: " + e.getMessage());
		} catch (Exception e) {
			System.out.println("Data file is corrupted: " + e.getMessage());
		}
	}

	private void save() {
		Path path = Paths.get(DATA_FILE);
		List<String> lines = new ArrayList<>();
		lines.add("VERSION|1");
		lines.add("META|" + nextAssignmentId + "|" + nextTaskId);
		for (Assignment assignment : assignments.values()) {
			lines.add(
				"ASSIGNMENT|" + assignment.id + "|" + escape(assignment.title) + "|" +
					escape(assignment.subject) + "|" + assignment.dueDate + "|" + assignment.estMinutes
			);
		}
		for (Assignment assignment : assignments.values()) {
			for (Task task : assignment.tasks) {
				lines.add(
					"TASK|" + task.id + "|" + task.assignmentId + "|" + escape(task.title) + "|" +
						task.status + "|" + task.estMinutes + "|" + task.spentMinutes
				);
			}
		}
		for (StudySession session : sessions) {
			lines.add(
				"SESSION|" + session.taskId + "|" + session.startMillis + "|" +
					session.endMillis + "|" + session.durationMinutes
			);
		}
		try {
			Files.write(path, lines, StandardCharsets.UTF_8,
				StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
		} catch (IOException e) {
			System.out.println("Failed to save data: " + e.getMessage());
		}
	}

	private void recalcIds() {
		int maxAssignment = 0;
		int maxTask = 0;
		for (Assignment assignment : assignments.values()) {
			maxAssignment = Math.max(maxAssignment, assignment.id);
			for (Task task : assignment.tasks) {
				maxTask = Math.max(maxTask, task.id);
			}
		}
		nextAssignmentId = maxAssignment + 1;
		nextTaskId = maxTask + 1;
	}

	private String escape(String value) {
		StringBuilder out = new StringBuilder();
		for (int i = 0; i < value.length(); i++) {
			char c = value.charAt(i);
			if (c == '\\') {
				out.append("\\\\");
			} else if (c == '|') {
				out.append("\\|");
			} else if (c == '\n') {
				out.append("\\n");
			} else if (c == '\r') {
				out.append("\\r");
			} else {
				out.append(c);
			}
		}
		return out.toString();
	}

	private List<String> splitEscaped(String line) {
		List<String> parts = new ArrayList<>();
		StringBuilder current = new StringBuilder();
		boolean escaping = false;
		for (int i = 0; i < line.length(); i++) {
			char c = line.charAt(i);
			if (escaping) {
				if (c == 'n') {
					current.append('\n');
				} else if (c == 'r') {
					current.append('\r');
				} else if (c == '|' || c == '\\') {
					current.append(c);
				} else {
					current.append(c);
				}
				escaping = false;
				continue;
			}
			if (c == '\\') {
				escaping = true;
				continue;
			}
			if (c == '|') {
				parts.add(current.toString());
				current.setLength(0);
				continue;
			}
			current.append(c);
		}
		if (escaping) {
			current.append('\\');
		}
		parts.add(current.toString());
		return parts;
	}

	private int parseIntSafe(String value, int fallback) {
		try {
			return Integer.parseInt(value);
		} catch (NumberFormatException e) {
			return fallback;
		}
	}

	private long parseLongSafe(String value, long fallback) {
		try {
			return Long.parseLong(value);
		} catch (NumberFormatException e) {
			return fallback;
		}
	}

	private enum TaskStatus {
		TODO,
		DONE
	}

	private static class Assignment {
		private final int id;
		private String title;
		private String subject;
		private LocalDate dueDate;
		private int estMinutes;
		private final List<Task> tasks = new ArrayList<>();

		private Assignment(int id, String title, String subject, LocalDate dueDate, int estMinutes) {
			this.id = id;
			this.title = title;
			this.subject = subject;
			this.dueDate = dueDate;
			this.estMinutes = estMinutes;
		}

		private int totalSpentMinutes() {
			int total = 0;
			for (Task task : tasks) {
				total += task.spentMinutes;
			}
			return total;
		}
	}

	private static class Task {
		private final int id;
		private final int assignmentId;
		private String title;
		private TaskStatus status;
		private int estMinutes;
		private int spentMinutes;

		private Task(int id, int assignmentId, String title, TaskStatus status, int estMinutes, int spentMinutes) {
			this.id = id;
			this.assignmentId = assignmentId;
			this.title = title;
			this.status = status;
			this.estMinutes = estMinutes;
			this.spentMinutes = spentMinutes;
		}
	}

	private static class StudySession {
		private final int taskId;
		private final long startMillis;
		private final long endMillis;
		private final int durationMinutes;

		private StudySession(int taskId, long startMillis, long endMillis, int durationMinutes) {
			this.taskId = taskId;
			this.startMillis = startMillis;
			this.endMillis = endMillis;
			this.durationMinutes = durationMinutes;
		}
	}

	private static class ActiveTimer {
		private final int taskId;
		private final long sessionStartMillis;
		private long lastStartMillis;
		private long accumulatedMillis;
		private boolean paused;

		private ActiveTimer(int taskId, long startMillis) {
			this.taskId = taskId;
			this.sessionStartMillis = startMillis;
			this.lastStartMillis = startMillis;
			this.accumulatedMillis = 0L;
			this.paused = false;
		}
	}
}

