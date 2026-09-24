package se233.se233_termproject_2026.controller.vectorizer;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.List;

public class ColorSegmenter {

    // 1. Reduce palette to K colors (Simplified example)
    public static List<Color> extractPalette(BufferedImage image, int colorCount) {
        // Implement K-Means or Median Cut clustering here to get top N colors
        // Returns a list of the dominant RGB colors in the image
        if (colorCount > 5) {
            // Fast built-in octree mapping for unlimited/large palettes
            return ColorQuantizer.extractPaletteUnlimited(image, colorCount);
        } else {
            // High-precision custom K-Means for 2 to 5 colors
            return ColorQuantizer.extractPaletteFew(image, colorCount, 10);
        }
    }

    // 2. Create a 1-bit binary matrix for ONE specific target color
    public static boolean[][] createBinaryMask(BufferedImage image, Color targetColor, List<Color> fullPalette) {
        int width = image.getWidth();
        int height = image.getHeight();
        boolean[][] mask = new boolean[height][width];

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgb = image.getRGB(x, y);
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;
                int[] pixel = new int[]{r, g, b};

                // Find closest palette color for this pixel
                Color closest = findClosestColor(pixel, fullPalette);

                // If this pixel maps to our target layer color, set mask bit to true
                mask[y][x] = closest.equals(targetColor);
            }
        }
        return mask;
    }

    private static Color findClosestColor(int[] pixel, List<Color> palette) {
        Color bestColor = palette.get(0);
        double minDistance = Double.MAX_VALUE;

        for (Color c : palette) {
            double dr = pixel[0] - c.getRed();
            double dg = pixel[1] - c.getGreen();
            double db = pixel[2] - c.getBlue();
            double dist = dr * dr + dg * dg + db * db;

            if (dist < minDistance) {
                minDistance = dist;
                bestColor = c;
            }
        }
        return bestColor;
    }
}