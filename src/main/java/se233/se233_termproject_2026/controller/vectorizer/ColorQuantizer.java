package se233.se233_termproject_2026.controller.vectorizer;

import java.awt.Graphics2D;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.awt.image.IndexColorModel;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class ColorQuantizer {

    public static List<Color> extractPaletteUnlimited(BufferedImage original, int maxColors) {
        // 1. Force Java's graphics engine to quantize the image down to 'maxColors'
        BufferedImage indexedImage = new BufferedImage(
                original.getWidth(),
                original.getHeight(),
                BufferedImage.TYPE_BYTE_INDEXED
        );

        Graphics2D g = indexedImage.createGraphics();
        g.drawImage(original, 0, 0, null);
        g.dispose();

        // 2. Extract the generated color palette directly from the ColorModel
        IndexColorModel colorModel = (IndexColorModel) indexedImage.getColorModel();
        int mapSize = colorModel.getMapSize();

        // Cap palette size at requested max
        int actualCount = Math.min(mapSize, maxColors);

        List<Color> palette = new ArrayList<>();
        byte[] reds = new byte[mapSize];
        byte[] greens = new byte[mapSize];
        byte[] blues = new byte[mapSize];

        colorModel.getReds(reds);
        colorModel.getGreens(greens);
        colorModel.getBlues(blues);

        for (int i = 0; i < actualCount; i++) {
            int r = reds[i] & 0xFF;
            int gr = greens[i] & 0xFF;
            int b = blues[i] & 0xFF;
            palette.add(new Color(r, gr, b));
        }

        return palette;
    }

    public static List<Color> extractPaletteFew(BufferedImage image, int k, int maxIterations) {
        int width = image.getWidth();
        int height = image.getHeight();

        // 1. Collect pixel RGB values (Sample down large images for performance)
        List<int[]> pixels = new ArrayList<>();
        int step = Math.max(1, (width * height) / 10000); // Sample ~10,000 pixels max for fast UI responsiveness

        for (int y = 0; y < height; y += (step > 1 ? 2 : 1)) {
            for (int x = 0; x < width; x += (step > 1 ? 2 : 1)) {
                int rgb = image.getRGB(x, y);
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;
                pixels.add(new int[]{r, g, b});
            }
        }

        if (pixels.isEmpty()) return List.of(Color.BLACK);

        // 2. Initialize K centroids randomly from sampled pixels
        Random rand = new Random(42); // Seeded for deterministic results
        double[][] centroids = new double[k][3];
        for (int i = 0; i < k; i++) {
            int[] p = pixels.get(rand.nextInt(pixels.size()));
            centroids[i][0] = p[0];
            centroids[i][1] = p[1];
            centroids[i][2] = p[2];
        }

        // 3. Iterative Clustering
        for (int iter = 0; iter < maxIterations; iter++) {
            double[][] newSum = new double[k][3];
            int[] counts = new int[k];

            // Assign pixels to nearest centroid
            for (int[] p : pixels) {
                int bestK = 0;
                double minDistance = Double.MAX_VALUE;

                for (int i = 0; i < k; i++) {
                    double dist = colorDistanceSq(p, centroids[i]);
                    if (dist < minDistance) {
                        minDistance = dist;
                        bestK = i;
                    }
                }

                newSum[bestK][0] += p[0];
                newSum[bestK][1] += p[1];
                newSum[bestK][2] += p[2];
                counts[bestK]++;
            }

            // Update centroids
            boolean shifted = false;
            for (int i = 0; i < k; i++) {
                if (counts[i] > 0) {
                    double newR = newSum[i][0] / counts[i];
                    double newG = newSum[i][1] / counts[i];
                    double newB = newSum[i][2] / counts[i];

                    if (Math.abs(newR - centroids[i][0]) > 0.5 ||
                            Math.abs(newG - centroids[i][1]) > 0.5 ||
                            Math.abs(newB - centroids[i][2]) > 0.5) {
                        shifted = true;
                    }

                    centroids[i][0] = newR;
                    centroids[i][1] = newG;
                    centroids[i][2] = newB;
                }
            }

            if (!shifted) break; // Convergence reached
        }

        // 4. Convert double centroids back to java.awt.Color objects
        List<Color> palette = new ArrayList<>();
        for (int i = 0; i < k; i++) {
            palette.add(new Color((int) centroids[i][0], (int) centroids[i][1], (int) centroids[i][2]));
        }

        return palette;
    }

    private static double colorDistanceSq(int[] p, double[] centroid) {
        double dr = p[0] - centroid[0];
        double dg = p[1] - centroid[1];
        double db = p[2] - centroid[2];
        return dr * dr + dg * dg + db * db;
    }
}