package se233.se233_termproject_2026.view_misc;

import javafx.geometry.Bounds;
import javafx.scene.Group;
import javafx.scene.layout.Pane;
import javafx.scene.shape.SVGPath;
import javafx.scene.transform.Scale;
import javafx.scene.transform.Translate;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import se233.se233_termproject_2026.controller.vectorizer.ColorLayer;

import java.awt.*;
import java.util.List;

public class VectorizedImageView {
    private static final Logger logger = LogManager.getLogger();
    private final Pane viewportPane;
    private final Group vectorGroup;
    private double originalImageWidth;
    private double originalImageHeight;

    public VectorizedImageView(Pane viewportPane) {
        this.viewportPane = viewportPane;
        this.vectorGroup = new Group();

        // 1. Add group to pane
        this.viewportPane.getChildren().add(vectorGroup);

        viewportPane.widthProperty().addListener((obs, oldV, newV) -> fitToViewport());
        viewportPane.heightProperty().addListener((obs, oldV, newV) -> fitToViewport());
    }

    public void loadVectorLayers(List<ColorLayer> layers, double imgWidth, double imgHeight) {
        this.originalImageWidth = imgWidth;
        this.originalImageHeight = imgHeight;

        vectorGroup.getChildren().clear();

        for (ColorLayer layer : layers) {
            if (layer.getSvgPathData() == null || layer.getSvgPathData().isEmpty()) continue;

            SVGPath fxPath = new SVGPath();
            fxPath.setContent(layer.getSvgPathData());
            fxPath.setFill(layer.getFxColor());
            fxPath.setStroke(null);

            vectorGroup.getChildren().add(fxPath);
        }

        fitToViewport();
    }

    public void fitToViewport() {
        double pWidth = viewportPane.getWidth();
        double pHeight = viewportPane.getHeight();

        if (pWidth <= 0 || pHeight <= 0 || originalImageWidth <= 0 || originalImageHeight <= 0) {
            return;
        }

        // Clear previous transforms first so getBoundsInLocal() reads raw path geometry
        vectorGroup.getTransforms().clear();
        vectorGroup.setTranslateX(0);
        vectorGroup.setTranslateY(0);

        // 1. Get raw SVG path dimensions
        Bounds rawBounds = vectorGroup.getBoundsInLocal();
        double unscaledWidth = rawBounds.getWidth();   // ~7206.0
        double unscaledHeight = rawBounds.getHeight(); // ~14402.0

        if (unscaledWidth <= 0 || unscaledHeight <= 0) {
            return;
        }

        // 2. Calculate uniform scale factor based on true unscaled path size
        double scaleX = pWidth / unscaledWidth;
        double scaleY = pHeight / unscaledHeight;
        double fitScale = Math.min(scaleX, scaleY);

        // 3. Flip Y and Scale simultaneously around the center pivot point
        double pivotX = rawBounds.getMinX() + (unscaledWidth / 2.0);
        double pivotY = rawBounds.getMinY() + (unscaledHeight / 2.0);

        // Scaling with negative Y around pivotY flips the image right-side up without drifting
        Scale flipAndScale = new Scale(fitScale, -fitScale, pivotX, pivotY);
        vectorGroup.getTransforms().add(flipAndScale);

        // 4. Center the Group inside the Viewport Pane
        // Read the actual transformed bounding box in parent coordinates
        Bounds transformedBounds = vectorGroup.getBoundsInParent();
        double renderedWidth = transformedBounds.getWidth();
        double renderedHeight = transformedBounds.getHeight();

        // Calculate centering shift based on transformed bounds
        double offsetX = (pWidth - renderedWidth) / 2.0 - transformedBounds.getMinX();
        double offsetY = (pHeight - renderedHeight) / 2.0 - transformedBounds.getMinY();

        vectorGroup.setTranslateX(offsetX);
        vectorGroup.setTranslateY(offsetY);

        logger.debug("Raw Paths: {}x{}", unscaledWidth, unscaledHeight);
        logger.debug("Rendered Bounds: {}", transformedBounds);
    }
}
