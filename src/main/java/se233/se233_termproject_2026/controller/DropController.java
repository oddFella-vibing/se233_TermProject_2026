package se233.se233_termproject_2026.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class DropController {
    private static final Logger logger = LogManager.getLogger(DropController.class);

    @FXML
    private AnchorPane dropPane;

    @FXML
    public void initialize() {
        // 1. Allow dragging items over the pane
        dropPane.setOnDragOver(event -> {
            if (event.getGestureSource() != dropPane && event.getDragboard().hasFiles()) {
                event.acceptTransferModes(TransferMode.COPY);
            }
            event.consume();
        });

        // 2. Handle when files are dropped
        dropPane.setOnDragDropped(event -> {
            boolean success = false;
            if (event.getDragboard().hasFiles()) {
                List<File> rawFiles = event.getDragboard().getFiles();

                // Process and flatten (handles PNG, JPG, and extracts ZIP archives)
                List<File> processedImages = processDroppedFiles(rawFiles);

                if (!processedImages.isEmpty()) {
                    transitionToMainView(processedImages);
                    success = true;
                } else {
                    logger.info("No valid PNG, JPG, or ZIP files found in drop.");
                }
            }
            event.setDropCompleted(success);
            event.consume();
        });
    }

    /**
     * Filters valid image files and extracts image contents if a ZIP file is dropped.
     */
    private List<File> processDroppedFiles(List<File> rawFiles) {
        List<File> validImages = new ArrayList<>();

        for (File file : rawFiles) {
            String name = file.getName().toLowerCase();
            if (name.endsWith(".png") || name.endsWith(".jpg") || name.endsWith(".jpeg")) {
                validImages.add(file);
            } else if (name.endsWith(".zip")) {
                validImages.addAll(extractImagesFromZip(file));
            }
        }
        return validImages;
    }

    private List<File> extractImagesFromZip(File zipFile) {
        List<File> extractedImages = new ArrayList<>();
        try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(zipFile.toPath()))) {
            Path tempDir = Files.createTempDirectory("vector_magic_unpacked");
            ZipEntry entry;

            while ((entry = zis.getNextEntry()) != null) {
                String entryName = entry.getName();

                //  Skip directories, Mac metadata folders, and hidden system files (like .DS_Store)
                if (!entry.isDirectory() &&
                        !entryName.contains("__MACOSX") &&
                        !new File(entryName).getName().startsWith(".")) {

                    String name = entryName.toLowerCase();
                    if (name.endsWith(".png") || name.endsWith(".jpg") || name.endsWith(".jpeg")) {
                        Path targetPath = tempDir.resolve(new File(entryName).getName());
                        Files.copy(zis, targetPath);
                        extractedImages.add(targetPath.toFile());
                    }
                }
                zis.closeEntry();
            }
        } catch (IOException e) {
            logger.debug("Invalid zip file input");
        }
        return extractedImages;
    }

    /**
     * Loads the main FXML workspace and hands off the image list to the MainController.
     */
    private void transitionToMainView(List<File> images) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/se233/se233_termproject_2026/main-view.fxml"));
            Parent root = loader.load();

            // Get MainController instance and pass the files
            MainController mainController = loader.getController();
            mainController.initFiles(images);

            // Swap the scene on the current stage window
            Stage stage = (Stage) dropPane.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Vector Magic - Workspace");
            stage.centerOnScreen();

        } catch (IOException e) {
            logger.debug(e.getMessage());
            logger.debug("Invalid file passed to Main");
        }
    }
}
