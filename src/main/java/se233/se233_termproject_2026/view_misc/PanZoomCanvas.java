package se233.se233_termproject_2026.view_misc;

import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.StackPane;

public class PanZoomCanvas {

    private double dragStartX;
    private double dragStartY;

    public void enablePanAndZoom(Node viewport, Node target) {

        // --- 1. ZOOM LOGIC (Mouse Wheel) ---
        viewport.setOnScroll(event -> {
            // Determine zoom direction
            double zoomFactor = (event.getDeltaY() > 0) ? 1.15 : 0.85;

            // Optional: Set min/max zoom limits (e.g., 50% to 2000%)
            double currentScale = target.getScaleX();
            double newScale = currentScale * zoomFactor;

            if (newScale >= 0.5 && newScale <= 20.0) {
                target.setScaleX(newScale);
                target.setScaleY(newScale);
            }

            event.consume(); // Prevent parent containers from scrolling
        });

        // --- 2. PAN LOGIC (Click & Drag) ---
        // Capture initial click position
        viewport.setOnMousePressed(event -> {
            if (event.getButton() == MouseButton.PRIMARY || event.getButton() == MouseButton.MIDDLE) {
                dragStartX = event.getX() - target.getTranslateX();
                dragStartY = event.getY() - target.getTranslateY();
            }
        });

        // Update translation as mouse moves
        viewport.setOnMouseDragged(event -> {
            if (event.getButton() == MouseButton.PRIMARY || event.getButton() == MouseButton.MIDDLE) {
                if(event.getX() >= 0 && event.getX() <= viewport.getLayoutBounds().getWidth()) {
                    if(target.getTranslateX() < -(viewport.getLayoutBounds().getWidth()/2)) target.setTranslateX(target.getTranslateX() + 1);
                    else if(target.getTranslateX() > viewport.getLayoutBounds().getWidth()/2) target.setTranslateX(target.getTranslateX() - 1);
                    else target.setTranslateX(event.getX() - dragStartX);
                }
                if(event.getY() >= 0 && event.getY() <= viewport.getLayoutBounds().getHeight()) {
                    if(target.getTranslateY() < -(viewport.getLayoutBounds().getHeight()/2)) target.setTranslateY(target.getTranslateY() + 1);
                    else if(target.getTranslateY() > viewport.getLayoutBounds().getHeight()/2) target.setTranslateY(target.getTranslateY() - 1);
                    else target.setTranslateY(event.getY() - dragStartY);
                }
            }
        });
    }
}