package se233.se233_termproject_2026.controller;

import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import se233.se233_termproject_2026.controller.vectorizer.*;
import se233.se233_termproject_2026.services.ProgressTaskService;
import se233.se233_termproject_2026.view_misc.PanZoomCanvas;
import javafx.concurrent.Task;
import se233.se233_termproject_2026.view_misc.VectorizedImageView;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.io.File;
import java.util.concurrent.atomic.AtomicReference;

public class MainController {
    private static final Logger logger = LogManager.getLogger();
    private static final int MAX_COLOR_COUNT = 256;

    @FXML private ImageView originalImage;
    @FXML private Pane vectorImage;

    @FXML private Pane originalImagePane;
    @FXML private Pane vectorImagePane;

    @FXML private StackPane originalImgView;
    @FXML private StackPane vectorizedImgView;

    @FXML private ScrollPane originalImgContainer;
    @FXML private ScrollPane vectorizedImgContainer;

    @FXML private ToggleGroup detailToggleGroup;
    @FXML private ToggleGroup colorToggleGroup;

    // New UI elements
    @FXML private VBox colorSwatchBox;

    @FXML private ProgressBar exportProgressBar;
    @FXML private CheckBox removeBackgroundCheckBox;

    @FXML private Label imageCounterLabel;
    @FXML private Button prevButton;
    @FXML private Button nextButton;

    @FXML private VBox extractProgressBox ;
    @FXML private ProgressBar extractProgressBar;
    @FXML private VBox analyzeProgressBox;
    @FXML private ProgressBar analyzeProgressBar;
    @FXML private VBox vectorizeProgressBox;
    @FXML private ProgressBar vectorizeProgressBar;

    private ProgressTaskService progressTaskService;

    private List<File> loadedFiles = new ArrayList<>();
    private List<Color> _5ColorPalette;
    private int selectedCustomColorCount = 2;
    private VectorizedImageView VView;
    private int currentIndex = 0;

    @FXML
    public void initialize() {
        progressTaskService = new ProgressTaskService(
                extractProgressBox, extractProgressBar,
                analyzeProgressBox, analyzeProgressBar,
                vectorizeProgressBox, vectorizeProgressBar
        );


        Rectangle clipO = new Rectangle();
        clipO.widthProperty().bind(originalImgView.widthProperty());
        clipO.heightProperty().bind(originalImgView.heightProperty());
        originalImgView.setClip(clipO);

        Rectangle clipV = new Rectangle();
        clipV.widthProperty().bind(vectorizedImgView.widthProperty());
        clipV.heightProperty().bind(vectorizedImgView.heightProperty());
        vectorizedImgView.setClip(clipV);

        PanZoomCanvas Ocontroller = new PanZoomCanvas(originalImgView, originalImage);
        Ocontroller.enablePanAndZoom();

        PanZoomCanvas Vcontroller = new PanZoomCanvas(vectorizedImgView, vectorImage);
        Vcontroller.enablePanAndZoom();

        VView = new VectorizedImageView(vectorImage);

        //  Color Toggle Group (Handles spinner visibility AND re-processing)
        colorToggleGroup.selectedToggleProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                ToggleButton selected = (ToggleButton) newVal;
                boolean isCustom = "Custom".equals(selected.getText());
                try {
                    _5ColorPalette = ColorSegmenter.extractPalette(ImageIO.read(loadedFiles.get(currentIndex)), 5);
                    updateColorSwatches(_5ColorPalette);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                colorSwatchBox.setVisible(isCustom);
                colorSwatchBox.setManaged(isCustom);
                triggerReprocessing();
            }
        });

        //  Detail Toggle Group (Re-runs pipeline when detail level changes)
        detailToggleGroup.selectedToggleProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                triggerReprocessing();
            }
        });

        //  Remove Background CheckBox (Re-runs pipeline when background option changes)
        removeBackgroundCheckBox.selectedProperty().addListener((obs, oldVal, newVal) -> {
            triggerReprocessing();
        });
    }
    public void processImageInFX(BufferedImage inputImage, Pane canvas, int targetColorCount) {
        // 1. ANALYZE TASK (Drives the analysis progress bar)
        String colorMode = getSelectedColorMode();
        Task<List<java.awt.Color>> analyzeTask = new Task<>() {
            @Override
            protected List<java.awt.Color> call() throws Exception {
                List<java.awt.Color> colors ;
                if ("Unlimited".equals(colorMode)) {
                    // Use original/unlimited palette extraction method . 32 currently
                    colors = ColorSegmenter.extractPalette(inputImage,MAX_COLOR_COUNT);
//                    colors.stream().forEach(c -> logger.debug("r: {}, g: {}, b: {}", c.getRed(), c.getGreen(), c.getBlue()));
                } else {
                    colors = ColorSegmenter.extractPalette(inputImage, targetColorCount);
                }
                logger.debug("Extract color successful");
                return colors;
            }
        };
        analyzeTask.setOnSucceeded(event -> {
            List<java.awt.Color> colors = analyzeTask.getValue();
            startVectorizeTask(inputImage, canvas, colors);
            logger.debug("Analyzation task ended successfully");
        });

        analyzeTask.setOnFailed(event -> {
            Throwable exception = analyzeTask.getException();
            logger.error("Color analyzation error: " + exception.getMessage());
            exception.printStackTrace();
        });
        // Bind analyze task; when it succeeds, it triggers the vector task with the colors
        progressTaskService.bindTask(analyzeTask, analyzeProgressBox, analyzeProgressBar);

        Thread analyzeThread = new Thread(analyzeTask);
        analyzeThread.setDaemon(true);
        analyzeThread.start();
    }

    public void startVectorizeTask(BufferedImage inputImage, Pane canvas, List<java.awt.Color> colors) {

        VectorTask vectorTask = new VectorTask(inputImage, colors, isRemoveBackgroundEnabled());

        // 3. Render results back on UI Thread
        vectorTask.setOnSucceeded(event -> {
            List<ColorLayer> layers = vectorTask.getValue();

//            for(ColorLayer cl: layers) {
//                logger.debug(cl.getSvgPathData());
//                logger.debug(cl.getHexColor());
//            }

            VView.loadVectorLayers(layers, inputImage.getWidth(), inputImage.getHeight());

            logger.debug("Task ended successfully");
        });


        vectorTask.setOnFailed(event -> {
            Throwable exception = vectorTask.getException();
            logger.error("Vectorization error: " + exception.getMessage());
            System.err.println("Vectorization error: " + exception.getMessage());
            exception.printStackTrace();
        });
        progressTaskService.bindTask(vectorTask, vectorizeProgressBox, vectorizeProgressBar);

        // Run task on background thread to keep UI smooth and responsive
        Thread backgroundThread = new Thread(vectorTask);
        backgroundThread.setDaemon(true); // Ensures thread closes if application quits
        backgroundThread.start();
    }

    public void initFiles(List<File> files) throws IOException {
        if (files != null && !files.isEmpty()) {
            this.loadedFiles = files;
            this.currentIndex = 0;

            Task<Void> extractTask = new Task<>() {
                @Override
                protected Void call() throws Exception {
                    int total = files.size();
                    for (int i = 0; i < total; i++) {
                        BufferedImage img = ImageIO.read(files.get(i));
                        // Optional: store or preload if needed
                        updateProgress(i + 1, total); // Drives extractProgressBar!
                    }
                    updateProgress(total, total);
                    return null;
                }
            };
            extractTask.setOnSucceeded(e -> {
                displayCurrentImage();
                try {
                    BufferedImage initialImg = ImageIO.read(files.get(currentIndex));
                    processImageInFX(initialImg, vectorImage, MAX_COLOR_COUNT);
                } catch (IOException ex) {
                    logger.error("Failed to load initial image: " + ex.getMessage());
                }
                logger.debug("Extraction task ended successfully");
            });

            extractTask.setOnFailed(e -> {
                Throwable exception = extractTask.getException();
                logger.error("File extraction error: " + exception.getMessage());
                exception.printStackTrace();
            });

            progressTaskService.bindTask(extractTask, extractProgressBox, extractProgressBar);

            Thread backgroundThread = new Thread(extractTask);
            backgroundThread.setDaemon(true);
            backgroundThread.start();
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
    private void handlePrevImage() throws IOException {
        if (currentIndex > 0) {
            currentIndex--;
            _5ColorPalette = ColorSegmenter.extractPalette(ImageIO.read(loadedFiles.get(currentIndex)), 5);
            updateColorSwatches(_5ColorPalette);
            for(Color cp: _5ColorPalette) {
                System.out.println(cp.toString());
            }
            displayCurrentImage();
            triggerReprocessing();
        }
    }

    @FXML
    private void handleNextImage() throws IOException {
        if (currentIndex < loadedFiles.size() - 1) {
            currentIndex++;
            _5ColorPalette = ColorSegmenter.extractPalette(ImageIO.read(loadedFiles.get(currentIndex)), 5);
            updateColorSwatches(_5ColorPalette);
            for(Color cp: _5ColorPalette) {
                System.out.println(cp.toString());
            }
            displayCurrentImage();
            triggerReprocessing();
        }
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

    private void processAndSaveFile(File file, String outputPath, int colorCount) throws IOException, InterruptedException {
        BufferedImage img = ImageIO.read(file);

        // 1. Extract palette and trace layers
        String colorMode = getSelectedColorMode();
        List<java.awt.Color> colors;
        if ("Unlimited".equals(colorMode)) {
            colors = ColorSegmenter.extractPalette(img, MAX_COLOR_COUNT); // Useful/unlimited palette
        } else {
            colors = ColorSegmenter.extractPalette(img, colorCount);
        }

        VectorTask vectorTask = new VectorTask(img, colors, isRemoveBackgroundEnabled());
        AtomicReference<String> svgContent = new AtomicReference<>();

        // 3. Render results back on UI Thread
        vectorTask.setOnSucceeded(event -> {
            List<ColorLayer> layers = vectorTask.getValue();
            svgContent.set(SVGWriter.generateFullSVG(layers, img.getWidth(), img.getHeight(), isRemoveBackgroundEnabled(), new Color(255, 255, 255)));
            logger.debug("Task ended successfully");
        });

        vectorTask.setOnFailed(event -> {
            Throwable exception = vectorTask.getException();
            logger.error("Vectorization error: " + exception.getMessage());
            System.err.println("Vectorization error: " + exception.getMessage());
            exception.printStackTrace();
        });

        progressTaskService.bindTask(vectorTask, vectorizeProgressBox, vectorizeProgressBar);

        // Run task on background thread to keep UI smooth and responsive
        Thread backgroundThread = new Thread(vectorTask);
        backgroundThread.setDaemon(true); // Ensures thread closes if application quits
        backgroundThread.start();

        while(svgContent.get() == null){} // this solution is not so good...

        logger.debug(svgContent.get());

        // 3. Write out the final .svg file into the target directory
        String fileName = file.getName();
        String baseName = fileName.contains(".") ? fileName.substring(0, fileName.lastIndexOf('.')) : fileName;
        File outputFile = new File(outputPath, baseName + "_vectorized.svg");
        java.nio.file.Files.writeString(outputFile.toPath(), svgContent.get());
    }
    @FXML
    private void handleExportAll() {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Select Output Folder for Batch Export");
        File selectedDirectory = directoryChooser.showDialog(new Stage());

        if (selectedDirectory != null && !loadedFiles.isEmpty()) {
            String outputPath = selectedDirectory.getAbsolutePath();
            int colorCount = getCustomColorCount();

            // Constraint check for parallel processing execution (as defined in your stub)
            boolean isMediumDetail = "Medium".equals(getSelectedDetailLevel());
            boolean isCustomColors = "Custom".equals(getSelectedColorMode());
            boolean useParallel = (loadedFiles.size() > 1) && isMediumDetail && isCustomColors;

            // Make progress bar visible during processing
            exportProgressBar.setVisible(true);

            Task<Void> exportTask = new Task<>() {
                @Override
                protected Void call() throws Exception {
                    int total = loadedFiles.size();

                    if (useParallel) {
                        logger.debug("Executing PARALLEL export to: " + outputPath);
                        java.util.concurrent.atomic.AtomicInteger completedCount = new java.util.concurrent.atomic.AtomicInteger(0);

                        // Use parallel stream for multi-image batch processing
                        loadedFiles.parallelStream().forEach(file -> {
                            try {
                                processAndSaveFile(file, outputPath, colorCount);
                                int current = completedCount.incrementAndGet();
                                updateProgress(current, total);
                            } catch (Exception e) {
                                logger.error("Failed to export file in parallel: {}", file.getName(), e);
                            }
                        });
                    } else {
                        logger.debug("Executing SEQUENTIALLY to: {}", outputPath);
                        for (int i = 0; i < total; i++) {
                            processAndSaveFile(loadedFiles.get(i), outputPath, colorCount);
                            updateProgress(i + 1, total);
                        }
                    }

                    updateProgress(total, total);
                    return null;
                }
            };

            // Bind progress bar and handle completion/failure UI cleanup
            exportProgressBar.progressProperty().bind(exportTask.progressProperty());

            exportTask.setOnSucceeded(e -> {
                exportProgressBar.progressProperty().unbind();
                exportProgressBar.setVisible(false);
                logger.debug("Batch export completed successfully!");
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Export Successful");
                alert.setHeaderText(null);
                alert.setContentText("All images have been successfully vectorized and exported to the selected folder!");
                alert.showAndWait();
            });

            exportTask.setOnFailed(e -> {
                exportProgressBar.progressProperty().unbind();
                exportProgressBar.setVisible(false);
                logger.error("Batch export failed: " + exportTask.getException().getMessage());
                exportTask.getException().printStackTrace();
            });

            Thread exportThread = new Thread(exportTask);
            exportThread.setDaemon(true);
            exportThread.start();
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
        return selectedCustomColorCount;
    }

    public boolean isRemoveBackgroundEnabled() {
        return removeBackgroundCheckBox.isSelected();
    }

    public ProgressBar getExportProgressBar() {
        return exportProgressBar;
    }
    private void triggerReprocessing() {
        if (loadedFiles.isEmpty()) return;
        try {
            BufferedImage currentImg = ImageIO.read(loadedFiles.get(currentIndex));
            processImageInFX(currentImg, vectorImage, getCustomColorCount());
            logger.debug("Settings changed: Re-triggering image processing pipeline.");
        } catch (IOException e) {
            logger.error("Failed to re-process image on setting change: {}", e.getMessage());
            e.printStackTrace();
        }
    }
//    helper color pallette selector
private void updateColorSwatches(List<java.awt.Color> detectedColors) {
    colorSwatchBox.getChildren().clear();

    // Ensure selectedCustomColorCount defaults safely within 2 to 5 bounds
    if (selectedCustomColorCount < 2 || selectedCustomColorCount > 5) {
        selectedCustomColorCount = 2;
    }

    // Loop to create 4 rows: 2 colors, 3 colors, 4 colors, and 5 colors
    for (int k = 2; k <= 5; k++) {
        final int rowColorCount = k;

        // Create an HBox container for each row option
        HBox rowBox = new HBox(6);
        rowBox.setAlignment(Pos.CENTER_LEFT);

        boolean isRowSelected = (selectedCustomColorCount == rowColorCount);

        // Style the row to look like a selectable card (highlight active row with blue border)
        String rowStyle = "-fx-padding: 4px 6px; -fx-background-radius: 4px; -fx-cursor: hand;";
        if (isRowSelected) {
            rowStyle += "-fx-background-color: #e3f2fd; -fx-border-color: #2196F3; -fx-border-width: 1.5px; -fx-border-radius: 4px;";
        } else {
            rowStyle += "-fx-background-color: #fafafa; -fx-border-color: #e0e0e0; -fx-border-width: 1px; -fx-border-radius: 4px;";
        }
        rowBox.setStyle(rowStyle);



        // Populate the color blocks (swatches) for this specific row count
        int availableColors = (detectedColors != null) ? detectedColors.size() : 0;
        for (int i = 0; i < k; i++) {
            Pane swatch = new Pane();
            swatch.setPrefSize(20, 20); // Compact size to fit 4 rows neatly

            if (detectedColors != null && i < availableColors) {
                java.awt.Color awtColor = detectedColors.get(i);
                String hex = String.format("#%02x%02x%02x", awtColor.getRed(), awtColor.getGreen(), awtColor.getBlue());
                swatch.setStyle("-fx-background-color: " + hex + "; -fx-border-color: #cccccc; -fx-border-width: 1px; -fx-border-radius: 2px;");
            } else {
                // Fallback placeholder if extraction hasn't finished or lacks enough colors
                swatch.setStyle("-fx-background-color: #e0e0e0; -fx-border-color: #cccccc; -fx-border-width: 1px; -fx-border-radius: 2px;");
            }
            rowBox.getChildren().add(swatch);
        }

        // Clicking a row selects that color configuration and reprocesses the view
        rowBox.setOnMouseClicked(e -> {
            selectedCustomColorCount = rowColorCount;
            updateColorSwatches(detectedColors);
            triggerReprocessing();
        });

        colorSwatchBox.getChildren().add(rowBox);
    }
}
}