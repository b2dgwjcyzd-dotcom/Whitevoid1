package whitevoid.create.editor.viewport;

public final class ViewportCamera {
    private double yaw;
    private double pitch;
    private double distance = 8.0;
    private double panX;
    private double panY;
    private ViewportMode mode = ViewportMode.PERSPECTIVE;

    public void orbit(double deltaYaw, double deltaPitch) {
        yaw += deltaYaw;
        pitch = Math.max(-89.0, Math.min(89.0, pitch + deltaPitch));
    }

    public void zoom(double amount) {
        distance = Math.max(0.25, Math.min(256.0, distance * Math.pow(0.9, amount)));
    }

    public void pan(double x, double y) {
        panX += x;
        panY += y;
    }

    public void reset() {
        yaw = 0;
        pitch = 0;
        distance = 8.0;
        panX = 0;
        panY = 0;
        mode = ViewportMode.PERSPECTIVE;
    }

    public void toggleMode() {
        mode = mode == ViewportMode.PERSPECTIVE
                ? ViewportMode.ORTHOGRAPHIC
                : ViewportMode.PERSPECTIVE;
    }

    public double yaw() { return yaw; }
    public double pitch() { return pitch; }
    public double distance() { return distance; }
    public double panX() { return panX; }
    public double panY() { return panY; }
    public ViewportMode mode() { return mode; }
    public void setMode(ViewportMode mode) { this.mode = mode; }
}
