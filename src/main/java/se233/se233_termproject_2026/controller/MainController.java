package se233.se233_termproject_2026.controller;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Rectangle;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import se233.se233_termproject_2026.controller.vectorizer.ColorLayer;
import se233.se233_termproject_2026.controller.vectorizer.ColorSegmenter;
import se233.se233_termproject_2026.controller.vectorizer.PotraceCLIEngine;
import se233.se233_termproject_2026.view_misc.PanZoomCanvas;
import javafx.concurrent.Task;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.io.File;

public class MainController {

    @FXML private ImageView originalImage;
    @FXML private ImageView vectorImage;

    @FXML private Pane originalImagePane;
    @FXML private Pane vectorImagePane;

    @FXML private StackPane originalImgView;
    @FXML private StackPane vectorizedImgView;

    @FXML private ScrollPane originalImgContainer;
    @FXML private ScrollPane vectorizedImgContainer;

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

        Rectangle clipO = new Rectangle();
        clipO.widthProperty().bind(originalImgView.widthProperty());
        clipO.heightProperty().bind(originalImgView.heightProperty());
        originalImgView.setClip(clipO);

        Rectangle clipV = new Rectangle();
        clipV.widthProperty().bind(vectorizedImgView.widthProperty());
        clipV.heightProperty().bind(vectorizedImgView.heightProperty());
        vectorizedImgView.setClip(clipV);

        PanZoomCanvas Ocontroller = new PanZoomCanvas();
        Ocontroller.enablePanAndZoom(originalImgView, originalImage);

        PanZoomCanvas Vcontroller = new PanZoomCanvas();
        Vcontroller.enablePanAndZoom(vectorizedImgView, vectorImage);

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

    public void processImageInFX(BufferedImage inputImage, Pane canvas, int targetColorCount) {
        Task<List<ColorLayer>> vectorTask = new Task<>() {
            @Override
            protected List<ColorLayer> call() throws Exception {
                // 1. Extract color palette
                List<java.awt.Color> colors = ColorSegmenter.extractPalette(inputImage, targetColorCount);
                List<ColorLayer> layers = new ArrayList<>();

                // 2. Trace each layer via Potrace ProcessBuilder
                for (java.awt.Color c : colors) {
                    boolean[][] mask = ColorSegmenter.createBinaryMask(inputImage, c, 10);

                    // Returns raw string from Potrace (ProcessBuilder)
                    String rawPotraceOutput = PotraceCLIEngine.traceMaskToSvgPath(mask);

                    // Extract clean 'd' attribute string
                    String cleanPathData = PotraceCLIEngine.extractPathDataFromPotraceSvg(rawPotraceOutput);

                    layers.add(new ColorLayer(c, cleanPathData));
                }
                return layers;
            }
        };

        // 3. Render results back on UI Thread
        vectorTask.setOnSucceeded(event -> {
            List<ColorLayer> layers = vectorTask.getValue();
            canvas.getChildren().clear();

            for (ColorLayer layer : layers) {
                if (layer.getSvgPathData() == null || layer.getSvgPathData().isEmpty()) {
                    continue;
                }

                SVGPath fxPath = new SVGPath();
                // JavaFX accepts raw "M... C... Z" path strings directly
                fxPath.setContent(layer.getSvgPathData());

                // Convert AWT Color to JavaFX Color
                Color fxColor = Color.rgb(
                        layer.getColor().getRed(),
                        layer.getColor().getGreen(),
                        layer.getColor().getBlue()
                );

                fxPath.setFill(fxColor);
                fxPath.setStroke(null); // Remove default black outline border

                canvas.getChildren().add(fxPath);
            }
        });

        vectorTask.setOnFailed(event -> {
            Throwable exception = vectorTask.getException();
            System.err.println("Vectorization error: " + exception.getMessage());
            exception.printStackTrace();
        });

        // Run task on background thread to keep UI smooth and responsive
        Thread backgroundThread = new Thread(vectorTask);
        backgroundThread.setDaemon(true); // Ensures thread closes if application quits
        backgroundThread.start();
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
        originalImage.setImage(image);

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
        originalImage.setScaleX(originalImage.getScaleX() * 1.15);
        originalImage.setScaleY(originalImage.getScaleY() * 1.15);
        vectorImage.setScaleX(vectorImage.getScaleX() * 1.15);
        vectorImage.setScaleY(vectorImage.getScaleY() * 1.15);
    }

    @FXML
    private void handleZoomOut() {
        originalImage.setScaleX(Math.max(0.5, originalImage.getScaleX() / 1.15));
        originalImage.setScaleY(Math.max(0.5, originalImage.getScaleY() / 1.15));
        vectorImage.setScaleX(Math.max(0.5, vectorImage.getScaleX() / 1.15));
        vectorImage.setScaleY(Math.max(0.5, vectorImage.getScaleY() / 1.15));
    }

    @FXML
    private void handleZoomToFit() {
        originalImage.setScaleX(1.0);
        originalImage.setScaleY(1.0);
        vectorImage.setScaleX(1.0);
        vectorImage.setScaleY(1.0);
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