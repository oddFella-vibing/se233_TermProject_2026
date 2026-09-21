package se233.se233_termproject_2026.controller.vectorizer;

import java.io.*;

public class PotraceCLIEngine {

    public static String traceMaskToSvgPath(boolean[][] bitMatrix) throws IOException, InterruptedException {
        // 1. Convert boolean matrix to a temporary PBM (1-bit binary image) file
        File tempPbm = File.createTempFile("potrace_mask_", ".pbm");
        tempPbm.deleteOnExit();
        writePbmFile(bitMatrix, tempPbm);

        // 2. Call local Potrace CLI executable
        // Command: potrace -s -o - tempPbm.pbm
        ProcessBuilder pb = new ProcessBuilder(
                "./potrace-1.16.win64/potrace.exe",               // Path to executable (or system PATH)
                "-s",                    // Output format: SVG
                "--turdsize", "2",       // Suppress noise specks
                "-o", "-",               // Write output directly to STDOUT stream
                tempPbm.getAbsolutePath()
        );

        Process process = pb.start();

        // 3. Read SVG XML directly from process stdout
        String svgOutput;
        try (InputStream is = process.getInputStream()) {
            svgOutput = new String(is.readAllBytes());
        }

        process.waitFor();

        // 4. Extract the 'd' attribute string from <path d="..." />
        return extractPathDataFromPotraceSvg(svgOutput);
    }

    // Helper to write a lightweight 1-bit PBM image file
    private static void writePbmFile(boolean[][] matrix, File outputFile) throws IOException {
        int height = matrix.length;
        int width = matrix[0].length;

        try (BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(outputFile))) {
            // PBM Plain Text Header: P1 Width Height
            String header = "P1\n" + width + " " + height + "\n";
            out.write(header.getBytes());

            StringBuilder sb = new StringBuilder();
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    sb.append(matrix[y][x] ? "1 " : "0 ");
                }
                sb.append("\n");
            }
            out.write(sb.toString().getBytes());
        }
    }

    public static String extractPathDataFromPotraceSvg(String rawPotraceSvg) {
        if (rawPotraceSvg == null || rawPotraceSvg.isEmpty()) {
            return "";
        }

        StringBuilder combinedPathData = new StringBuilder();
        int searchIndex = 0;

        while ((searchIndex = rawPotraceSvg.indexOf("d=\"", searchIndex)) != -1) {
            int dStart = searchIndex + 3;
            int dEnd = rawPotraceSvg.indexOf("\"", dStart);
            if (dEnd != -1) {
                combinedPathData.append(rawPotraceSvg.substring(dStart, dEnd)).append(" ");
                searchIndex = dEnd;
            } else {
                break;
            }
        }

        return combinedPathData.toString().trim();
    }
}