package se233.se233_termproject_2026.controller.vectorizer;

import java.util.List;

public class SVGWriter {
    public static String generateFullSVG(List<ColorLayer> layers, int width, int height, boolean includeBackground, java.awt.Color bgColor) {
        StringBuilder svg = new StringBuilder();
        svg.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        svg.append(String.format("<svg width=\"%d\" height=\"%d\" viewBox=\"0 0 %d %d\" xmlns=\"http://www.w3.org/2000/svg\">\n",
                width, height, width, height));

        // 2. Transform group: scale(0.1, -0.1) shrinks Potrace's 10x units and flips Y right-side up
        int potraceMaxY = height * 10;
        svg.append(String.format("  <g transform=\"scale(0.1, -0.1) translate(0, -%d)\">\n", potraceMaxY));

        // 2. Append each traced vector layer
        for (ColorLayer layer : layers) {
            String hexColor = String.format("#%02x%02x%02x",
                    layer.getColor().getRed(), layer.getColor().getGreen(), layer.getColor().getBlue());

            if (!layer.getSvgPathData().isEmpty()) {
                svg.append(String.format("  <path fill=\"%s\" d=\"%s\" />\n", hexColor, layer.getSvgPathData()));
            }
        }

        svg.append("</g>\n");
        svg.append("</svg>");
        return svg.toString();
    }
}
