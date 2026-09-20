package whitevoid.create.ui;

import whitevoid.create.editor.viewport.ViewportCamera;

public final class ViewportProjector {
    private final ViewportCamera camera;

    public ViewportProjector(ViewportCamera camera) {
        this.camera = camera;
    }

    public Point project(double x, double y, double z, int centerX, int centerY, double focalLength) {
        double yaw = Math.toRadians(camera.yaw());
        double pitch = Math.toRadians(camera.pitch());

        double cy = Math.cos(yaw);
        double sy = Math.sin(yaw);
        double x1 = x * cy - z * sy;
        double z1 = x * sy + z * cy;

        double cp = Math.cos(pitch);
        double sp = Math.sin(pitch);
        double y2 = y * cp - z1 * sp;
        double z2 = y * sp + z1 * cp;

        double depth = z2 + camera.distance();
        if (depth <= 0.05) {
            return null;
        }

        double scale = focalLength / depth;
        double screenX = centerX + camera.panX() * 18.0 + x1 * scale;
        double screenY = centerY - camera.panY() * 18.0 - y2 * scale;
        return new Point(screenX, screenY, depth);
    }

    public double cameraYaw() { return camera.yaw(); }
    public double cameraPitch() { return camera.pitch(); }
    public double cameraDistance() { return camera.distance(); }
    public double cameraPanX() { return camera.panX() * 18.0; }
    public double cameraPanY() { return camera.panY() * 18.0; }
    public double cameraPitch() { return camera.pitch(); }

    public record Point(double x, double y, double depth) {}
}
