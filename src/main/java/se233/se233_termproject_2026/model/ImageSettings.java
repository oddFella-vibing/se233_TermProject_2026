package se233.se233_termproject_2026.model;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

public class ImageSettings {
    private String colorMode = "Custom";
    private int customColorCount = 2;
    private String detailLevel = "Medium";
    private boolean removeBackground = false;
    private List<Color> cachedPalette = new ArrayList<>();
    private String preset = null;
    // Getters and Setters
    public String getColorMode() { return colorMode; }
    public void setColorMode(String colorMode) { this.colorMode = colorMode; }

    public int getCustomColorCount() { return customColorCount; }
    public void setCustomColorCount(int customColorCount) { this.customColorCount = customColorCount; }

    public String getDetailLevel() { return detailLevel; }
    public void setDetailLevel(String detailLevel) { this.detailLevel = detailLevel; }

    public boolean isRemoveBackground() { return removeBackground; }
    public void setRemoveBackground(boolean removeBackground) { this.removeBackground = removeBackground; }

    public List<Color> getCachedPalette() { return cachedPalette; }
    public void setCachedPalette(List<Color> cachedPalette) { this.cachedPalette = cachedPalette; }
    public String getPreset() { return preset; }
    public void setPreset(String preset) { this.preset = preset; }
}
