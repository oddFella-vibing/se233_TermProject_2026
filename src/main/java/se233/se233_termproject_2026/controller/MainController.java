package se233.se233_termproject_2026.controller;

import javafx.fxml.FXML;
import javafx.scene.Group;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import javafx.scene.transform.Scale;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import se233.se233_termproject_2026.controller.vectorizer.ColorLayer;
import se233.se233_termproject_2026.controller.vectorizer.ColorSegmenter;
import se233.se233_termproject_2026.controller.vectorizer.PotraceCLIEngine;
import se233.se233_termproject_2026.services.ProgressTaskService;
import se233.se233_termproject_2026.view_misc.PanZoomCanvas;
import javafx.concurrent.Task;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;
import se233.se233_termproject_2026.view_misc.VectorizedImageView;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.io.File;

public class MainController {
    private static final Logger logger = LogManager.getLogger();

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
    @FXML private Spinner<Integer> customColorSpinner;
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
    private VectorizedImageView VView;
    private int currentIndex = 0;

    @FXML
    public void initialize() {
        progressTaskService = new ProgressTaskService(
                extractProgressBox, extractProgressBar,
                analyzeProgressBox, analyzeProgressBar,
                vectorizeProgressBox, vectorizeProgressBar
        );
        // 1. Initialize Custom Color Spinner (Range: 2 to 5, Default: 2)
        SpinnerValueFactory<Integer> colorValueFactory =
                new SpinnerValueFactory.IntegerSpinnerValueFactory(2, 5, 2);
        customColorSpinner.setValueFactory(colorValueFactory);
        customColorSpinner.valueProperty().addListener((obs, oldVal, newVal) -> {
            triggerReprocessing();
        });
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
                customColorSpinner.setVisible(isCustom);
                customColorSpinner.setManaged(isCustom);
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
        // 1. ANALYZE TASK (Drives the analyze progress bar)
        String colorMode = getSelectedColorMode();
        Task<List<java.awt.Color>> analyzeTask = new Task<>() {
            @Override
            protected List<java.awt.Color> call() throws Exception {
                List<java.awt.Color> colors ;
                if ("Unlimited".equals(colorMode)) {
                    // Use original/unlimited palette extraction method . 255 currently
                    colors = ColorSegmenter.extractPalette(inputImage,255);
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
        Task<List<ColorLayer>> vectorTask = new Task<>() {
            @Override
            protected List<ColorLayer> call() throws Exception {
                // 1. Extract color palette

                List<ColorLayer> layers = new ArrayList<>();
                logger.debug("Extract color successful");

                // 2. Trace each layer via Potrace ProcessBuilder
                for (java.awt.Color c : colors) {
                    boolean[][] mask = ColorSegmenter.createBinaryMask(inputImage, c, colors);

                    // Returns raw string from Potrace (ProcessBuilder)
                    String rawPotraceOutput = PotraceCLIEngine.traceMaskToSvgPath(mask);
//                    logger.debug(rawPotraceOutput);

                    // Extract clean 'd' attribute string
//                    String cleanPathData = PotraceCLIEngine.extractPathDataFromPotraceSvg(rawPotraceOutput);

                    layers.add(new ColorLayer(c, rawPotraceOutput));
                }
                logger.debug("Mask layer extraction successful");
                return layers;
            }
        };

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
                        // Optional: store or pre-load if needed
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
                    processImageInFX(initialImg, vectorImage, getCustomColorCount());
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
    private void handlePrevImage() {
        if (currentIndex > 0) { currentIndex--; displayCurrentImage(); triggerReprocessing();}
    }

    @FXML
    private void handleNextImage() {
        if (currentIndex < loadedFiles.size() - 1) { currentIndex++; displayCurrentImage();
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
            colors = ColorSegmenter.extractPalette(img, 255); // Use full/unlimited palette
        } else {
            colors = ColorSegmenter.extractPalette(img, colorCount);
        }
        List<ColorLayer> layers = new ArrayList<>();

        for (java.awt.Color c : colors) {
            boolean[][] mask = ColorSegmenter.createBinaryMask(img, c, colors);
            String rawSvgPath = PotraceCLIEngine.traceMaskToSvgPath(mask);
            layers.add(new ColorLayer(c, rawSvgPath));
        }

        // 2. Construct the SVG string content
        StringBuilder svgContent = new StringBuilder();
        svgContent.append(String.format("<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"%d\" height=\"%d\">\n", img.getWidth(), img.getHeight()));
        for (ColorLayer layer : layers) {
            String hexColor = String.format("#%02x%02x%02x", layer.getColor().getRed(), layer.getColor().getGreen(), layer.getColor().getBlue());
            svgContent.append(String.format("  <path fill=\"%s\" d=\"%s\"/>\n", hexColor, layer.getSvgPathData()));
        }
        svgContent.append("</svg>");

        // 3. Write out the final .svg file into the target directory
        String fileName = file.getName();
        String baseName = fileName.contains(".") ? fileName.substring(0, fileName.lastIndexOf('.')) : fileName;
        File outputFile = new File(outputPath, baseName + "_vectorized.svg");
        java.nio.file.Files.writeString(outputFile.toPath(), svgContent.toString());
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
                                logger.error("Failed to export file in parallel: " + file.getName(), e);
                            }
                        });
                    } else {
                        logger.debug("Executing SEQUENTIALLY to: " + outputPath);
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
        return customColorSpinner.getValue();
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
            logger.error("Failed to re-process image on setting change: " + e.getMessage());
            e.printStackTrace();
        }
    }
}