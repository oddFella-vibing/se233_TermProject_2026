package se233.se233_termproject_2026.controller.vectorizer;

import javafx.concurrent.Task;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class VectorTask extends Task<List<ColorLayer>> {
    private static final Logger logger = LogManager.getLogger(VectorTask.class);
    private BufferedImage inputImage;
    private List<java.awt.Color> colors;
    private boolean removeBackground;

    public VectorTask(BufferedImage inputImage, List<java.awt.Color> colors, boolean removeBackground) {
        this.inputImage = inputImage;
        this.colors = colors;
        this.removeBackground = removeBackground;
    }

    private boolean removeBackgroundByFloodFill(boolean[][] mask, int width, int height, double minCoverage) {
        if (mask == null || width <= 0 || height <= 0) return false;

        boolean[][] visited = new boolean[height][width];
        Queue<Integer[]> queue = new ArrayDeque<>();

        // 1. Seed the 4 image corners
        int[][] corners = {
                {0, 0},
                {width - 1, 0},
                {0, height - 1},
                {width - 1, height - 1}
        };

        for (int[] corner : corners) {
            int cx = corner[0];
            int cy = corner[1];
            if (mask[cy][cx] && !visited[cy][cx]) {
                visited[cy][cx] = true;
                queue.add(new Integer[]{cx, cy});
            }
        }

        // If none of the corners belong to this color mask, it's not the background layer
        if (queue.isEmpty()) {
            return false;
        }

        // 2. BFS Flood-Fill to count and mark connected outer background pixels
        int floodedPixelCount = 0;
        int[] dx = {1, -1, 0, 0};
        int[] dy = {0, 0, 1, -1};

        // We use a secondary queue or list to track flooded points so we can clear them if threshold is met
        Queue<Integer[]> floodedPoints = new ArrayDeque<>();

        while (!queue.isEmpty()) {
            Integer[] current = (Integer[]) queue.poll();
            int x = current[0];
            int y = current[1];

            floodedPoints.add(current);
            floodedPixelCount++;

            for (int i = 0; i < 4; i++) {
                int nx = x + dx[i];
                int ny = y + dy[i];

                if (nx >= 0 && nx < width && ny >= 0 && ny < height) {
                    if (mask[ny][nx] && !visited[ny][nx]) {
                        visited[ny][nx] = true;
                        queue.add(new Integer[]{nx, ny});
                    }
                }
            }
        }

        // 3. Check if the flooded contiguous area is large enough to be considered the background
        double totalPixels = width * height;
        double coverageRatio = floodedPixelCount / totalPixels;

        if (coverageRatio >= minCoverage) {
            // Clear only the flooded background pixels from the mask
            for (Integer[] point : floodedPoints) {
                mask[point[1]][point[0]] = false;
            }
            return true; // Background was detected and carved out
        }

        return false;
    }

    private int countMaskPixels(boolean[][] mask) {
        int count = 0;
        for (boolean[] row : mask) {
            for (boolean val : row) {
                if (val) count++;
            }
        }
        return count;
    }

    @Override
    protected List<ColorLayer> call() throws Exception {
        // 1. Trace each layer via Potrace ProcessBuilder
        int totalPixels = inputImage.getWidth() * inputImage.getHeight();
        int minPixelThreshold = (int) (totalPixels * 0.0005); // 0.1% threshold -> 0.05% for 255 color
        ExecutorService threadPool = Executors.newFixedThreadPool(
                Runtime.getRuntime().availableProcessors()
        );
        // 2. Dispatch each color layer as a parallel task
        List<CompletableFuture<ColorLayer>> futures = new ArrayList<>();
        for (java.awt.Color c : colors) {

            CompletableFuture<ColorLayer> task = CompletableFuture.supplyAsync(() -> {
                boolean[][] mask = ColorSegmenter.createBinaryMask(inputImage, c, colors);

                if(removeBackground) {
                    // Flood fills from corners; if it covers >= 15% of total canvas, it clears those background pixels
                    boolean wasBgRemoved = removeBackgroundByFloodFill(mask, inputImage.getWidth(), inputImage.getHeight(), 0.15);

                    // If the entire mask was just the background, countMaskPixels will be 0
                    if (wasBgRemoved && countMaskPixels(mask) == 0) {
                        return null; // Skip Potrace completely for empty background layers!
                    }
                }

                int pixelCount = countMaskPixels(mask);

                // Skip layers that barely exist in the image
                if (pixelCount < minPixelThreshold) {
                    return null;
                }

                // Returns raw string from Potrace (ProcessBuilder)
                String rawPotraceOutput = null;
                try {
                    rawPotraceOutput = PotraceCLIEngine.traceMaskToSvgPath(mask);
                } catch (IOException e) {
                    e.printStackTrace();
                    return null;
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    return null;
                }
//                    logger.debug(rawPotraceOutput);

                // C. Trace remaining mask geometry (interior details stay intact!)
                if (rawPotraceOutput == null || rawPotraceOutput.trim().isEmpty()) {
                    return null;
                }

                return new ColorLayer(c, rawPotraceOutput);
            }, threadPool);
            futures.add(task);
        }

        logger.debug("Mask layer extraction successful");
        List<ColorLayer> layers = futures.stream()
                .map(CompletableFuture::join)
                .filter(colorLayer -> colorLayer != null)
                .toList();
        return layers;
    }
}
