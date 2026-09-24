package se233.se233_termproject_2026.view_misc;

import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.StackPane;

import java.util.ArrayList;
import java.util.List;

public class PanZoomCanvas {

    private double dragStartX;
    private double dragStartY;

    private static List<Node> gviewports = new ArrayList<>();
    private static List<Node> gtargets = new ArrayList<>();

    private Node thisViewport;
    private Node thisTarget;

    public PanZoomCanvas(Node viewport, Node target) {
        this.thisViewport = viewport;
        this.thisTarget = target;
        gviewports.add(viewport);
        gtargets.add(target);
    }

    public void enablePanAndZoom() {

        // --- 1. ZOOM LOGIC (Mouse Wheel) ---
        this.thisViewport.setOnScroll(event -> {
            for(Node target: gtargets) {
                // Determine zoom direction
                double zoomFactor = (event.getDeltaY() > 0) ? 1.15 : 0.85;

                // Optional: Set min/max zoom limits (e.g., 50% to 2000%)
                double currentScale = target.getScaleX();
                double newScale = currentScale * zoomFactor;

                if (newScale >= 0.1 && newScale <= 20.0) {
                    target.setScaleX(newScale);
                    target.setScaleY(newScale);
                }

                event.consume(); // Prevent parent containers from scrolling
            }
        });

        // --- 2. PAN LOGIC (Click & Drag) ---
        // Capture initial click position
        this.thisViewport.setOnMousePressed(event -> {
            for(Node target: gtargets) {
                if (event.getButton() == MouseButton.PRIMARY || event.getButton() == MouseButton.MIDDLE) {
                    dragStartX = event.getX() - target.getTranslateX();
                    dragStartY = event.getY() - target.getTranslateY();
                }
            }
        });

        // Update translation as mouse moves
        this.thisViewport.setOnMouseDragged(event -> {
            for(int i = 0; i<gtargets.size(); i++) {
                if (event.getButton() == MouseButton.PRIMARY || event.getButton() == MouseButton.MIDDLE) {
                    if(event.getX() >= 0 && event.getX() <= gviewports.get(i).getLayoutBounds().getWidth()) {
                        if(gtargets.get(i).getTranslateX() < -(gviewports.get(i).getLayoutBounds().getWidth()/2)) gtargets.get(i).setTranslateX(gtargets.get(i).getTranslateX() + 1);
                        else if(gtargets.get(i).getTranslateX() > gviewports.get(i).getLayoutBounds().getWidth()/2) gtargets.get(i).setTranslateX(gtargets.get(i).getTranslateX() - 1);
                        else gtargets.get(i).setTranslateX(event.getX() - dragStartX);
                    }
                    if(event.getY() >= 0 && event.getY() <= gviewports.get(i).getLayoutBounds().getHeight()) {
                        if(gtargets.get(i).getTranslateY() < -(gviewports.get(i).getLayoutBounds().getHeight()/2)) gtargets.get(i).setTranslateY(gtargets.get(i).getTranslateY() + 1);
                        else if(gtargets.get(i).getTranslateY() > gviewports.get(i).getLayoutBounds().getHeight()/2) gtargets.get(i).setTranslateY(gtargets.get(i).getTranslateY() - 1);
                        else gtargets.get(i).setTranslateY(event.getY() - dragStartY);
                    }
                }
            }
        });
    }
}