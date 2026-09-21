package whitevoid.create.model;

public final class Transform {
    private double x;
    private double y;
    private double z;
    private double rotationX;
    private double rotationY;
    private double rotationZ;
    private double scaleX = 1.0;
    private double scaleY = 1.0;
    private double scaleZ = 1.0;

    public double x() { return x; }
    public double y() { return y; }
    public double z() { return z; }
    public double rotationX() { return rotationX; }
    public double rotationY() { return rotationY; }
    public double rotationZ() { return rotationZ; }
    public double scaleX() { return scaleX; }
    public double scaleY() { return scaleY; }
    public double scaleZ() { return scaleZ; }

    public void position(double x, double y, double z) {
        requireFinite(x, "x");
        requireFinite(y, "y");
        requireFinite(z, "z");
        this.x = x; this.y = y; this.z = z;
    }

    public void rotation(double x, double y, double z) {
        requireFinite(x, "rotationX");
        requireFinite(y, "rotationY");
        requireFinite(z, "rotationZ");
        rotationX = x; rotationY = y; rotationZ = z;
    }

    public void scale(double x, double y, double z) {
        requireFinite(x, "scaleX");
        requireFinite(y, "scaleY");
        requireFinite(z, "scaleZ");
        if (x == 0.0 || y == 0.0 || z == 0.0) {
            throw new IllegalArgumentException("Scale components must be non-zero");
        }
        scaleX = x; scaleY = y; scaleZ = z;
    }

    private static void requireFinite(double value, String name) {
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException(name + " must be finite");
        }
    }
}
