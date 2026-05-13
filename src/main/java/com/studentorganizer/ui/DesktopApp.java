package com.studentorganizer.ui;

import com.studentorganizer.service.OrganizerService;
import com.studentorganizer.storage.OrganizerStorage;
import javafx.animation.PauseTransition;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Stage;
import javafx.util.Duration;

import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class DesktopApp extends Application {
    private OrganizerService service;

    @Override
    public void start(Stage stage) {
        Path dataPath = Paths.get(System.getProperty("user.home"), ".student-assignment-organizer", "data.txt");
        service = new OrganizerService(new OrganizerStorage(dataPath));

        try {
            service.load();
        } catch (IOException ex) {
            showStartupError("Unable to load data. Starting with empty state.\n" + ex.getMessage());
        }

        MainController controller = new MainController(service);
        Parent root = controller.build();

        stage.setTitle("Student Assignment Organizer");
        stage.setScene(new Scene(root, 1100, 700));
        stage.show();

        stage.setOnCloseRequest(event -> {
            try {
                service.save();
            } catch (IOException ex) {
                showStartupError("Failed to save data on exit:\n" + ex.getMessage());
            }
        });

        maybeCaptureScreenshot(root);
    }

    private void maybeCaptureScreenshot(Parent root) {
        List<String> args = getParameters().getRaw();
        String screenshotArg = args.stream().filter(s -> s.startsWith("--screenshot=")).findFirst().orElse(null);
        if (screenshotArg == null) {
            return;
        }
        String filePath = screenshotArg.substring("--screenshot=".length());
        PauseTransition pause = new PauseTransition(Duration.seconds(1));
        pause.setOnFinished(e -> {
            try {
                File output = Paths.get(filePath).toFile();
                File parent = output.getParentFile();
                if (parent != null) {
                    parent.mkdirs();
                }
                ImageIO.write(SwingFXUtils.fromFXImage(root.snapshot(null, null), null), "png", output);
            } catch (IOException ioException) {
                showStartupError("Failed to capture screenshot:\n" + ioException.getMessage());
            } finally {
                Platform.exit();
            }
        });
        pause.play();
    }

    private void showStartupError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Student Assignment Organizer");
        alert.setHeaderText("Error");
        alert.setContentText(message);
        alert.showAndWait();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
