package se233.se233_termproject_2026.controller.vectorizer;

import java.awt.Color;

public class ColorLayer {
    private final Color color;           // The original AWT color (used for quantization & export)
    private final String svgPathData;    // The 'd' attribute string from Potrace ("M... C... Z")

    public ColorLayer(Color color, String svgPathData) {
        this.color = color;
        this.svgPathData = svgPathData;
    }

    public Color getColor() {
        return color;
    }

    public String getSvgPathData() {
        return svgPathData;
    }

    /**
     * Convenience helper to convert the AWT Color into a JavaFX Color
     * for direct rendering on the JavaFX Scene Graph.
     */
    public javafx.scene.paint.Color getFxColor() {
        if (color == null) return javafx.scene.paint.Color.BLACK;
        return javafx.scene.paint.Color.rgb(
                color.getRed(),
                color.getGreen(),
                color.getBlue(),
                color.getAlpha() / 255.0
        );
    }

    /**
     * Convenience helper to convert the AWT Color into an SVG-compatible Hex string (#RRGGBB).
     */
    public String getHexColor() {
        if (color == null) return "#000000";
        return String.format("#%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue());
    }
}