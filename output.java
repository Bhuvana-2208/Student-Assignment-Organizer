
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class output {
	private static final int WIDTH = 78;

	public static void main(String[] args) throws IOException {
		BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
		System.out.print("Enter your problem statement sentence: ");
		String problem = reader.readLine();
		if (problem == null || problem.trim().isEmpty()) {
			problem = "I want to reduce the time spent on assignments and use that time to learn programming.";
		} else {
			problem = problem.trim();
		}

		printTopBorder();
		printLine(center("MY CORE JAVA PROJECT: WHAT I BUILT THIS WEEK", WIDTH));
		printMiddleBorder();
		printSectionHeader("PROJECT OVERVIEW");
		printKeyValue("Name", "Student Assignment Organizer - Track your study time and stay focused!");
		printKeyValue("Problem", problem);
		printKeyValue("Solution", "Plan assignments, split into tasks, track real-time study, export weekly report.");
		printKeyValue("Tech", "Core Java, OOP, Collections, File I/O, java.time");
		printKeyValue("Classes", "ProblemStatement, Assignment, Task, StudySession, ActiveTimer, TaskStatus");
		printMiddleBorder();
		printSectionHeader("APP PREVIEW (Console)");
		printLine("===== STUDENT ASSIGNMENT ORGANIZER =====");
		printLine("Active timer: none");
		printLine("1) Add assignment");
		printLine("2) List assignments");
		printLine("3) Edit assignment");
		printLine("4) Delete assignment");
		printLine("5) Add task to assignment");
		printLine("6) List tasks for assignment");
		printLine("7) Edit task");
		printLine("8) Delete task");
		printLine("9) Start timer for task");
		printLine("10) Pause timer");
		printLine("11) Resume timer");
		printLine("12) Stop active timer");
		printLine("13) Mark task done");
		printLine("14) Show daily summary");
		printLine("15) Export weekly report");
		printLine("16) Save and exit");
		printLine("Choose an option: 14");
		printLine("---- TODAY SUMMARY ----");
		printLine("DSA Assignment : 120m");
		printLine("Total today   : 120m");
		printMiddleBorder();
		printSectionHeader("OOP CONCEPTS USED");
		printBullet("Encapsulation with private fields and controlled updates");
		printBullet("Aggregation: Assignment holds a list of Task objects");
		printBullet("Composition: StudySession tied to a Task lifecycle");
		printBullet("Abstraction: menu hides storage + timer details");
		printMiddleBorder();
		printSectionHeader("WHAT I LEARNED");
		printBullet("Planning classes first saves a lot of time");
		printBullet("Input validation is harder than the core logic");
		printBullet("Timers and file saving need careful edge handling");
		printMiddleBorder();
		printSectionHeader("FEEDBACK RECEIVED");
		printBullet("This makes assignments less overwhelming.");
		printBullet("Weekly report helps me track progress.");
		printMiddleBorder();
		printSectionHeader("LINKS");
		printKeyValue("Source Code", "(https://github.com/Bhuvana-2208/Student-Assignment-Organizer)");
		printBottomBorder();
	}

	private static void printTopBorder() {
		System.out.println("┌" + repeat('─', WIDTH) + "┐");
	}

	private static void printMiddleBorder() {
		System.out.println("├" + repeat('─', WIDTH) + "┤");
	}

	private static void printBottomBorder() {
		System.out.println("└" + repeat('─', WIDTH) + "┘");
	}

	private static void printSectionHeader(String title) {
		printLine(title);
	}

	private static void printLine(String content) {
		String safe = content == null ? "" : content;
		if (safe.length() > WIDTH) {
			safe = safe.substring(0, WIDTH);
		}
		System.out.println("│" + padRight(safe, WIDTH) + "│");
	}

	private static void printKeyValue(String label, String value) {
		String prefix = label + " : ";
		int available = Math.max(1, WIDTH - prefix.length());
		List<String> lines = wrapText(value, available);
		if (lines.isEmpty()) {
			printLine(prefix);
			return;
		}
		printLine(prefix + lines.get(0));
		for (int i = 1; i < lines.size(); i++) {
			printLine(repeat(' ', prefix.length()) + lines.get(i));
		}
	}

	private static void printBullet(String text) {
		String prefix = "- ";
		int available = Math.max(1, WIDTH - prefix.length());
		List<String> lines = wrapText(text, available);
		if (lines.isEmpty()) {
			printLine(prefix);
			return;
		}
		printLine(prefix + lines.get(0));
		for (int i = 1; i < lines.size(); i++) {
			printLine(repeat(' ', prefix.length()) + lines.get(i));
		}
	}

	private static List<String> wrapText(String text, int maxWidth) {
		List<String> result = new ArrayList<>();
		if (text == null) {
			return result;
		}
		String[] words = text.trim().split("\\s+");
		StringBuilder line = new StringBuilder();
		for (String word : words) {
			if (line.length() == 0) {
				line.append(word);
			} else if (line.length() + 1 + word.length() <= maxWidth) {
				line.append(' ').append(word);
			} else {
				result.add(line.toString());
				line.setLength(0);
				line.append(word);
			}
		}
		if (line.length() > 0) {
			result.add(line.toString());
		}
		return result;
	}

	private static String padRight(String text, int width) {
		if (text.length() >= width) {
			return text;
		}
		return text + repeat(' ', width - text.length());
	}

	private static String center(String text, int width) {
		if (text.length() >= width) {
			return text.substring(0, width);
		}
		int left = (width - text.length()) / 2;
		int right = width - text.length() - left;
		return repeat(' ', left) + text + repeat(' ', right);
	}

	private static String repeat(char ch, int count) {
		if (count <= 0) {
			return "";
		}
		StringBuilder builder = new StringBuilder(count);
		for (int i = 0; i < count; i++) {
			builder.append(ch);
		}
		return builder.toString();
	}
}

