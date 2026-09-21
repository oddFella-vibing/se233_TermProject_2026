package se233.se233_termproject_2026.controller.vectorizer;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.List;

public class ColorSegmenter {

    // 1. Reduce palette to K colors (Simplified example)
    public static List<Color> extractPalette(BufferedImage image, int colorCount) {
        // Implement K-Means or Median Cut clustering here to get top N colors
        // Returns a list of the dominant RGB colors in the image
        return List.of(Color.RED, Color.BLUE, Color.BLACK);
    }

    // 2. Create a 1-bit binary matrix for ONE specific target color
    public static boolean[][] createBinaryMask(BufferedImage image, Color targetColor, int tolerance) {
        int width = image.getWidth();
        int height = image.getHeight();
        boolean[][] bitMatrix = new boolean[height][width];

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                Color pixelColor = new Color(image.getRGB(x, y));
                // Check if pixel matches target color within tolerance
                bitMatrix[y][x] = isColorMatch(pixelColor, targetColor, tolerance);
            }
        }
        return bitMatrix;
    }

    private static boolean isColorMatch(Color c1, Color c2, int tol) {
        return Math.abs(c1.getRed() - c2.getRed()) <= tol &&
                Math.abs(c1.getGreen() - c2.getGreen()) <= tol &&
                Math.abs(c1.getBlue() - c2.getBlue()) <= tol;
    }
}