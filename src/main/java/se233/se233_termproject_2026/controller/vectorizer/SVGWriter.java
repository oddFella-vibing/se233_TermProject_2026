package se233.se233_termproject_2026.controller.vectorizer;

import java.util.List;

public class SVGWriter {
    public static String generateFullSVG(List<ColorLayer> layers, int width, int height, boolean includeBackground, java.awt.Color bgColor) {
        StringBuilder svg = new StringBuilder();
        svg.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        svg.append(String.format("<svg width=\"%d\" height=\"%d\" viewBox=\"0 0 %d %d\" xmlns=\"http://www.w3.org/2000/svg\">\n",
                width, height, width, height));

        // 1. Optional background rectangle
        if (includeBackground && bgColor != null) {
            String bgHex = String.format("#%02x%02x%02x", bgColor.getRed(), bgColor.getGreen(), bgColor.getBlue());
            svg.append(String.format("  <rect width=\"100%%\" height=\"100%%\" fill=\"%s\" />\n", bgHex));
        }

        // 2. Append each traced vector layer
        for (ColorLayer layer : layers) {
            String hexColor = String.format("#%02x%02x%02x",
                    layer.getColor().getRed(), layer.getColor().getGreen(), layer.getColor().getBlue());

            // Clean path data from Potrace output
            String cleanPathData = PotraceCLIEngine.extractPathDataFromPotraceSvg(layer.getSvgPathData());

            if (!cleanPathData.isEmpty()) {
                svg.append(String.format("  <path fill=\"%s\" d=\"%s\" />\n", hexColor, cleanPathData));
            }
        }

        svg.append("</svg>");
        return svg.toString();
    }
}
