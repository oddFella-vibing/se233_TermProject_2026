package se233.se233_termproject_2026.controller.vectorizer;

import java.awt.Graphics2D;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.awt.image.IndexColorModel;
import java.util.ArrayList;
import java.util.List;

public class ColorQuantizer {

    public static List<Color> extractPaletteBuiltIn(BufferedImage original, int maxColors) {
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
}