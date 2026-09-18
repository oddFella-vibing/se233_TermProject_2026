package se233.se233_termproject_2026.controller;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class MainController {

    @FXML private ImageView originalImageView;
    @FXML private ImageView vectorImageView;

    @FXML private ToggleGroup detailToggleGroup;
    @FXML private ToggleGroup colorToggleGroup;

    // New UI elements
    @FXML private Spinner<Integer> customColorSpinner;
    @FXML private ProgressBar exportProgressBar;
    @FXML private CheckBox removeBackgroundCheckBox;

    @FXML private Label imageCounterLabel;
    @FXML private Button prevButton;
    @FXML private Button nextButton;

    private List<File> loadedFiles = new ArrayList<>();
    private int currentIndex = 0;

    @FXML
    public void initialize() {
        // 1. Initialize Custom Color Spinner (Range: 2 to 5, Default: 2)
        SpinnerValueFactory<Integer> colorValueFactory =
                new SpinnerValueFactory.IntegerSpinnerValueFactory(2, 5, 2);
        customColorSpinner.setValueFactory(colorValueFactory);

        // 2. Hide/Show spinner dynamically based on whether "Custom" is active
        colorToggleGroup.selectedToggleProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                ToggleButton selected = (ToggleButton) newVal;
                boolean isCustom = "Custom".equals(selected.getText());
                customColorSpinner.setVisible(isCustom);
                customColorSpinner.setManaged(isCustom);
            }
        });
    }

    public void initFiles(List<File> files) {
        if (files != null && !files.isEmpty()) {
            this.loadedFiles = files;
            this.currentIndex = 0;
            displayCurrentImage();
        }
    }

    private void displayCurrentImage() {
        if (loadedFiles.isEmpty()) return;

        File currentFile = loadedFiles.get(currentIndex);
        Image image = new Image(currentFile.toURI().toString());
        originalImageView.setImage(image);

        imageCounterLabel.setText("Image " + (currentIndex + 1) + " of " + loadedFiles.size());
        prevButton.setDisable(currentIndex == 0);
        nextButton.setDisable(currentIndex == loadedFiles.size() - 1);
    }

    @FXML
    private void handlePrevImage() {
        if (currentIndex > 0) { currentIndex--; displayCurrentImage(); }
    }

    @FXML
    private void handleNextImage() {
        if (currentIndex < loadedFiles.size() - 1) { currentIndex++; displayCurrentImage(); }
    }

    @FXML
    private void handleZoomIn() {
        originalImageView.setScaleX(originalImageView.getScaleX() * 1.15);
        originalImageView.setScaleY(originalImageView.getScaleY() * 1.15);
        vectorImageView.setScaleX(vectorImageView.getScaleX() * 1.15);
        vectorImageView.setScaleY(vectorImageView.getScaleY() * 1.15);
    }

    @FXML
    private void handleZoomOut() {
        originalImageView.setScaleX(Math.max(0.5, originalImageView.getScaleX() / 1.15));
        originalImageView.setScaleY(Math.max(0.5, originalImageView.getScaleY() / 1.15));
        vectorImageView.setScaleX(Math.max(0.5, vectorImageView.getScaleX() / 1.15));
        vectorImageView.setScaleY(Math.max(0.5, vectorImageView.getScaleY() / 1.15));
    }

    @FXML
    private void handleZoomToFit() {
        originalImageView.setScaleX(1.0);
        originalImageView.setScaleY(1.0);
        vectorImageView.setScaleX(1.0);
        vectorImageView.setScaleY(1.0);
    }

    @FXML
    private void handleExportAll() {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Select Output Folder for Batch Export");
        File selectedDirectory = directoryChooser.showDialog(new Stage());

        if (selectedDirectory != null) {
            String outputPath = selectedDirectory.getAbsolutePath();

            // Constraint check for parallel processing execution
            boolean isMediumDetail = "Medium".equals(getSelectedDetailLevel());
            boolean isCustomColors = "Custom".equals(getSelectedColorMode());
            boolean useParallel = (loadedFiles.size() > 1) && isMediumDetail && isCustomColors;

            // Make progress bar visible during processing
            exportProgressBar.setVisible(true);

            if (useParallel) {
                System.out.println("Executing PARALLEL export to: " + outputPath);
                System.out.println("Parameters -> Colors: " + getCustomColorCount() + ", Transparent BG: " + isRemoveBackgroundEnabled());
                // TODO : Hand off loadedFiles, outputPath, and settings to your background task here.
            } else {
                System.out.println("Executing SEQUENTIALLY to: " + outputPath);
                // TODO : Hand off to sequential runner.
            }
        }
    }


    // =========================================================================
    // INTEGRATION API: Clean accessors for processing & background tasks
    // =========================================================================

    public List<File> getLoadedFiles() {
        return loadedFiles;
    }

    public String getSelectedDetailLevel() {
        ToggleButton btn = (ToggleButton) detailToggleGroup.getSelectedToggle();
        return (btn != null) ? btn.getText() : "Medium";
    }

    public String getSelectedColorMode() {
        ToggleButton btn = (ToggleButton) colorToggleGroup.getSelectedToggle();
        return (btn != null) ? btn.getText() : "Custom";
    }

    public int getCustomColorCount() {
        return customColorSpinner.getValue();
    }

    public boolean isRemoveBackgroundEnabled() {
        return removeBackgroundCheckBox.isSelected();
    }

    public ProgressBar getExportProgressBar() {
        return exportProgressBar;
    }
}