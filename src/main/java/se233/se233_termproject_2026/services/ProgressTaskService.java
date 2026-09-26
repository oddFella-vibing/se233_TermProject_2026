package se233.se233_termproject_2026.services;

import javafx.concurrent.Task;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.VBox;

public class ProgressTaskService {

    private final VBox extractProgressBox;
    private final ProgressBar extractProgressBar;
    private final VBox analyzeProgressBox;
    private final ProgressBar analyzeProgressBar;
    private final VBox vectorizeProgressBox;
    private final ProgressBar vectorizeProgressBar;

    public ProgressTaskService(VBox extractProgressBox, ProgressBar extractProgressBar,
                               VBox analyzeProgressBox, ProgressBar analyzeProgressBar,
                               VBox vectorizeProgressBox, ProgressBar vectorizeProgressBar) {
        this.extractProgressBox = extractProgressBox;
        this.extractProgressBar = extractProgressBar;
        this.analyzeProgressBox = analyzeProgressBox;
        this.analyzeProgressBar = analyzeProgressBar;
        this.vectorizeProgressBox = vectorizeProgressBox;
        this.vectorizeProgressBar = vectorizeProgressBar;
    }

    public void bindTask(Task<?> task, VBox progressBox, ProgressBar progressBar) {
        if (progressBox != null && progressBar != null) {
            progressBox.setVisible(true);
            progressBox.setManaged(true);
            progressBar.progressProperty().bind(task.progressProperty());

            // 1. Capture whatever setOnSucceeded your teammate already wrote on the task!
            EventHandler<WorkerStateEvent> existingOnSucceeded = task.getOnSucceeded();
            EventHandler<WorkerStateEvent> existingOnFailed = task.getOnFailed();

            // 2. Set the service's cleanup logic, but safely invoke your teammate's code too
            task.setOnSucceeded(e -> {
                progressBar.progressProperty().unbind();
                progressBox.setVisible(false);
                progressBox.setManaged(false);

                // Automatically run your teammate's original success logic!
                if (existingOnSucceeded != null) {
                    existingOnSucceeded.handle(e);
                }
            });

            task.setOnFailed(e -> {
                progressBar.progressProperty().unbind();
                progressBox.setVisible(false);
                progressBox.setManaged(false);

                // Automatically run your teammate's original failure logic, or print exception
                if (existingOnFailed != null) {
                    existingOnFailed.handle(e);
                } else if (task.getException() != null) {
                    task.getException().printStackTrace();
                }
            });
        }
    }

    // Getters for boxes/bars if needed for individual stage binding
    public VBox getVectorizeProgressBox() { return vectorizeProgressBox; }
    public ProgressBar getVectorizeProgressBar() { return vectorizeProgressBar; }
}