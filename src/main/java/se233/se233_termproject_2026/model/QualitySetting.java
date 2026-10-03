package se233.se233_termproject_2026.model;

import java.util.HashMap;
import java.util.Map;

public class QualitySetting {
    private Integer turdsize;
    private Double alphamax;
    private Double opttolerance;
    private String turnpolicy;

    public QualitySetting() {
        this("Medium");
    }

    public QualitySetting(String mode) {
        changeMode(mode);
    }

    public void changeMode(String mode) {
        switch (mode) {
            case "Low":
                this.turdsize = 10;
                this.alphamax = 1.3;
                this.opttolerance = 0.4;
                this.turnpolicy = "majority";
                break;
            case "High":
                this.turdsize = 2;
                this.alphamax = 0.55;
                this.opttolerance = 0.1;
                this.turnpolicy = "minority";
                break;
            case "Medium": default:
                this.turdsize = 5;
                this.alphamax = 1.0;
                this.opttolerance = 0.2;
                this.turnpolicy = "minority";
        }
    }

    public Integer getTurdsize() {
        return turdsize;
    }

    public Double getAlphamax() {
        return alphamax;
    }

    public Double getOpttolerance() {
        return opttolerance;
    }

    public String getTurnpolicy() {
        return turnpolicy;
    }
}
