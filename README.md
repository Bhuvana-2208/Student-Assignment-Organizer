# Student Assignment Organizer (Desktop App)

This project now **replaces the original console app with a JavaFX desktop app** for production-ready usage.

## New Folder Structure

```text
Student-Assignment-Organizer/
├── pom.xml
├── src/
│   ├── main/
│   │   └── java/com/studentorganizer/
│   │       ├── model/
│   │       │   ├── Assignment.java
│   │       │   ├── Task.java
│   │       │   ├── StudySession.java
│   │       │   ├── ActiveTimer.java
│   │       │   └── TaskStatus.java
│   │       ├── service/
│   │       │   ├── OrganizerService.java
│   │       │   └── ValidationException.java
│   │       ├── storage/
│   │       │   └── OrganizerStorage.java
│   │       └── ui/
│   │           ├── DesktopApp.java
│   │           └── MainController.java
│   └── test/
│       └── java/com/studentorganizer/storage/
│           └── OrganizerStorageTest.java
└── README.md
```

## Features

- Assignment list + details (title, subject, due date, estimated minutes, total spent)
- Task list for selected assignment (status, estimated, spent)
- Timer controls (start/pause/resume/stop) tied to selected task
- Daily summary view
- Weekly report view + export to file
- Data persistence to disk in a pipe-delimited format at:
  - `~/.student-assignment-organizer/data.txt`
- Input validation with user-friendly error dialogs

## Build and Run

### Prerequisites

- Java 17+
- Maven 3.9+

### Commands

```bash
mvn clean test
mvn javafx:run
```

## Notes

- Architecture follows MVC-style separation:
  - **model**: domain data classes
  - **service**: business logic and validation
  - **storage**: file persistence and loading
  - **ui**: JavaFX desktop presentation layer

## UI Preview

A generated screenshot of the JavaFX app is available at:
- `docs/ui-screenshot.png`
