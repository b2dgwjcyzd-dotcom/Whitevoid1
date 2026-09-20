package whitevoid.create.ui;

import whitevoid.create.editor.viewport.ViewportCamera;

/**
 * Converts CREATE screen mouse gestures into camera operations.
 *
 * Controls:
 * - Middle mouse drag: orbit
 * - Shift + middle mouse drag: pan
 * - Mouse wheel: zoom
 */
public final class CreateViewportInput {
    private static final double ORBIT_SENSITIVITY = 0.35;
    private static final double PAN_SENSITIVITY = 0.02;

    private boolean middleDragging;
    private double lastMouseX;
    private double lastMouseY;

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 2) {
            return false;
        }

        middleDragging = true;
        lastMouseX = mouseX;
        lastMouseY = mouseY;
        return true;
    }

    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button != 2) {
            return false;
        }

        middleDragging = false;
        return true;
    }

    public boolean mouseDragged(double mouseX, double mouseY, int button, boolean shiftDown) {
        if (!middleDragging || button != 2) {
            return false;
        }

        double deltaX = mouseX - lastMouseX;
        double deltaY = mouseY - lastMouseY;
        lastMouseX = mouseX;
        lastMouseY = mouseY;

        ViewportCamera camera = camera();
        if (shiftDown) {
            camera.pan(-deltaX * PAN_SENSITIVITY, deltaY * PAN_SENSITIVITY);
        } else {
            camera.orbit(-deltaX * ORBIT_SENSITIVITY, -deltaY * ORBIT_SENSITIVITY);
        }

        return true;
    }

    public boolean mouseScrolled(double verticalAmount) {
        if (verticalAmount == 0.0) {
            return false;
        }

        camera().zoom(verticalAmount);
        return true;
    }

    public void cancelDrag() {
        middleDragging = false;
    }

    private ViewportCamera camera() {
        throw new IllegalStateException("CreateViewportInput must be bound to a camera before use");
    }
}
