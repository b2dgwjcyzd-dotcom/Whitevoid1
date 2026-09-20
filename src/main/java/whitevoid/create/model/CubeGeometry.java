package whitevoid.create.model;

public record CubeGeometry(double width, double height, double depth) {
    public CubeGeometry {
        if (width <= 0 || height <= 0 || depth <= 0) {
            throw new IllegalArgumentException("Cube dimensions must be positive");
        }
    }
}
