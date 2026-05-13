package com.studentorganizer.ui;

import com.studentorganizer.model.Assignment;
import com.studentorganizer.model.Task;
import com.studentorganizer.service.OrganizerService;
import com.studentorganizer.service.ValidationException;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.util.Duration;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;

public class MainController {
    private final OrganizerService service;

    private final ListView<Assignment> assignmentsView = new ListView<>();
    private final TableView<Task> tasksTable = new TableView<>();

    private final Label titleValue = new Label("-");
    private final Label subjectValue = new Label("-");
    private final Label dueDateValue = new Label("-");
    private final Label estimatedValue = new Label("-");
    private final Label totalSpentValue = new Label("-");

    private final Label timerLabel = new Label("00:00:00");
    private final TextArea dailySummaryArea = new TextArea();
    private final TextArea weeklySummaryArea = new TextArea();
    private final DatePicker dailyDatePicker = new DatePicker(LocalDate.now());
    private final DatePicker weeklyDatePicker = new DatePicker(LocalDate.now());

    private final Timeline timerTimeline;

    public MainController(OrganizerService service) {
        this.service = service;
        this.timerTimeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> refreshTimer()));
        this.timerTimeline.setCycleCount(Timeline.INDEFINITE);
        this.timerTimeline.play();
    }

    public Parent build() {
        BorderPane root = new BorderPane();
        root.setPadding(new Insets(12));

        VBox leftPanel = new VBox(10,
                new Label("Assignments"),
                assignmentsView,
                new HBox(8, button("Add Assignment", this::addAssignmentDialog), button("Save", this::saveData))
        );
        VBox.setVgrow(assignmentsView, Priority.ALWAYS);
        leftPanel.setPrefWidth(300);

        VBox centerPanel = new VBox(10,
                buildAssignmentDetails(),
                new Label("Tasks"),
                buildTasksTable(),
                new HBox(8,
                        button("Add Task", this::addTaskDialog),
                        button("Start", this::startTimer),
                        button("Pause", this::pauseTimer),
                        button("Resume", this::resumeTimer),
                        button("Stop", this::stopTimer),
                        new Label("Timer:"),
                        timerLabel
                )
        );
        VBox.setVgrow(tasksTable, Priority.ALWAYS);

        TabPane summaryTabs = new TabPane(
                new Tab("Daily Summary", buildDailySummaryView()),
                new Tab("Weekly Report", buildWeeklySummaryView())
        );
        summaryTabs.getTabs().forEach(tab -> tab.setClosable(false));
        summaryTabs.setPrefHeight(220);

        root.setLeft(leftPanel);
        root.setCenter(centerPanel);
        root.setBottom(summaryTabs);

        assignmentsView.getItems().setAll(service.getAssignments());
        assignmentsView.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) -> refreshSelectedAssignment(newValue));

        refreshDailySummary();
        refreshWeeklySummary();

        return root;
    }

    private GridPane buildAssignmentDetails() {
        GridPane grid = new GridPane();
        grid.setHgap(8);
        grid.setVgap(6);
        grid.addRow(0, new Label("Title:"), titleValue);
        grid.addRow(1, new Label("Subject:"), subjectValue);
        grid.addRow(2, new Label("Due Date:"), dueDateValue);
        grid.addRow(3, new Label("Estimated Minutes:"), estimatedValue);
        grid.addRow(4, new Label("Total Spent:"), totalSpentValue);
        return grid;
    }

    private TableView<Task> buildTasksTable() {
        TableColumn<Task, String> titleCol = new TableColumn<>("Task");
        titleCol.setCellValueFactory(new PropertyValueFactory<>("title"));

        TableColumn<Task, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getStatus().name()));

        TableColumn<Task, Integer> estCol = new TableColumn<>("Estimated");
        estCol.setCellValueFactory(new PropertyValueFactory<>("estimatedMinutes"));

        TableColumn<Task, Integer> spentCol = new TableColumn<>("Spent");
        spentCol.setCellValueFactory(new PropertyValueFactory<>("spentMinutes"));

        tasksTable.getColumns().setAll(titleCol, statusCol, estCol, spentCol);
        tasksTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        return tasksTable;
    }

    private VBox buildDailySummaryView() {
        dailySummaryArea.setEditable(false);
        dailySummaryArea.setPrefRowCount(6);
        return new VBox(8,
                new HBox(8, new Label("Date:"), dailyDatePicker, button("Refresh", this::refreshDailySummary)),
                dailySummaryArea
        );
    }

    private VBox buildWeeklySummaryView() {
        weeklySummaryArea.setEditable(false);
        weeklySummaryArea.setPrefRowCount(6);
        return new VBox(8,
                new HBox(8,
                        new Label("Any date in week:"),
                        weeklyDatePicker,
                        button("Refresh", this::refreshWeeklySummary),
                        button("Export", this::exportWeekly)
                ),
                weeklySummaryArea
        );
    }

    private void refreshSelectedAssignment(Assignment assignment) {
        if (assignment == null) {
            titleValue.setText("-");
            subjectValue.setText("-");
            dueDateValue.setText("-");
            estimatedValue.setText("-");
            totalSpentValue.setText("-");
            tasksTable.getItems().clear();
            return;
        }

        titleValue.setText(assignment.getTitle());
        subjectValue.setText(assignment.getSubject());
        dueDateValue.setText(assignment.getDueDate().toString());
        estimatedValue.setText(Integer.toString(assignment.getEstimatedMinutes()));
        totalSpentValue.setText(assignment.getTotalSpentMinutes() + " min");
        tasksTable.getItems().setAll(assignment.getTasks());
    }

    private void addAssignmentDialog() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Add Assignment");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        TextField title = new TextField();
        TextField subject = new TextField();
        TextField dueDate = new TextField();
        dueDate.setPromptText("YYYY-MM-DD");
        TextField estimated = new TextField();

        GridPane form = new GridPane();
        form.setHgap(8);
        form.setVgap(8);
        form.addRow(0, new Label("Title"), title);
        form.addRow(1, new Label("Subject"), subject);
        form.addRow(2, new Label("Due Date"), dueDate);
        form.addRow(3, new Label("Estimated Minutes"), estimated);
        dialog.getDialogPane().setContent(form);

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            runSafely(() -> {
                Assignment assignment = service.createAssignment(title.getText(), subject.getText(), dueDate.getText(), estimated.getText());
                assignmentsView.getItems().add(assignment);
                assignmentsView.getSelectionModel().select(assignment);
                refreshDailySummary();
                refreshWeeklySummary();
            });
        }
    }

    private void addTaskDialog() {
        Assignment assignment = assignmentsView.getSelectionModel().getSelectedItem();
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Add Task");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        TextField title = new TextField();
        TextField estimated = new TextField();

        GridPane form = new GridPane();
        form.setHgap(8);
        form.setVgap(8);
        form.addRow(0, new Label("Task Title"), title);
        form.addRow(1, new Label("Estimated Minutes"), estimated);
        dialog.getDialogPane().setContent(form);

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            runSafely(() -> {
                service.createTask(assignment, title.getText(), estimated.getText());
                refreshSelectedAssignment(assignment);
            });
        }
    }

    private void startTimer() {
        runSafely(() -> service.startTimer(tasksTable.getSelectionModel().getSelectedItem()));
    }

    private void pauseTimer() {
        runSafely(service::pauseTimer);
    }

    private void resumeTimer() {
        runSafely(service::resumeTimer);
    }

    private void stopTimer() {
        runSafely(() -> {
            service.stopTimer();
            refreshSelectedAssignment(assignmentsView.getSelectionModel().getSelectedItem());
            refreshDailySummary();
            refreshWeeklySummary();
        });
    }

    private void refreshTimer() {
        timerLabel.setText(OrganizerService.formatDuration(service.getActiveElapsedSeconds()));
    }

    private void refreshDailySummary() {
        runSafely(() -> {
            Map<String, Integer> summary = service.buildDailySummary(dailyDatePicker.getValue());
            if (summary.isEmpty()) {
                dailySummaryArea.setText("No study sessions found.");
                return;
            }
            StringBuilder sb = new StringBuilder();
            summary.forEach((assignment, minutes) -> sb.append(assignment).append(": ").append(minutes).append(" min\n"));
            dailySummaryArea.setText(sb.toString());
        });
    }

    private void refreshWeeklySummary() {
        runSafely(() -> {
            Map<LocalDate, Integer> weekly = service.buildWeeklySummary(weeklyDatePicker.getValue());
            StringBuilder sb = new StringBuilder();
            weekly.forEach((date, minutes) -> sb.append(date).append(": ").append(minutes).append(" min\n"));
            weeklySummaryArea.setText(sb.toString());
        });
    }

    private void exportWeekly() {
        runSafely(() -> {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Export Weekly Report");
            chooser.setInitialFileName("weekly-report.txt");
            File file = chooser.showSaveDialog(assignmentsView.getScene().getWindow());
            if (file != null) {
                service.exportWeeklyReport(weeklyDatePicker.getValue(), file.toPath());
                showInfo("Export complete", "Weekly report exported to:\n" + file.getAbsolutePath());
            }
        });
    }

    private void saveData() {
        runSafely(() -> {
            service.save();
            showInfo("Saved", "Data saved successfully.");
        });
    }

    private Button button(String text, Runnable action) {
        Button btn = new Button(text);
        btn.setOnAction(e -> action.run());
        return btn;
    }

    private void runSafely(CheckedRunnable action) {
        try {
            action.run();
        } catch (ValidationException ex) {
            showError("Validation error", ex.getMessage());
        } catch (IOException ex) {
            showError("I/O error", ex.getMessage());
        } catch (Exception ex) {
            showError("Unexpected error", ex.getMessage());
        }
    }

    private void showInfo(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private void showError(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    @FunctionalInterface
    private interface CheckedRunnable {
        void run() throws Exception;
    }
}
