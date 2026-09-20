package whitevoid.create.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Editable polygon mesh used by CREATE geometry tools.
 *
 * The first implementation stores indexed vertices and polygon faces.
 * Faces reference vertex indices in counter-clockwise order.
 */
public final class MeshGeometry {
    public record Vertex(double x, double y, double z) {}
    public record Face(int... vertices) {
        public Face {
            if (vertices == null || vertices.length < 3) {
                throw new IllegalArgumentException("A face needs at least three vertices");
            }
            vertices = vertices.clone();
        }

        @Override
        public int[] vertices() {
            return vertices.clone();
        }
    }

    private final List<Vertex> vertices;
    private final List<Face> faces;

    public MeshGeometry(List<Vertex> vertices, List<Face> faces) {
        if (vertices == null || faces == null) throw new NullPointerException();
        this.vertices = List.copyOf(vertices);
        List<Face> copy = new ArrayList<>(faces.size());
        for (Face face : faces) {
            for (int index : face.vertices()) {
                if (index < 0 || index >= this.vertices.size()) {
                    throw new IllegalArgumentException("Face references invalid vertex: " + index);
                }
            }
            copy.add(face);
        }
        this.faces = Collections.unmodifiableList(copy);
    }

    public List<Vertex> vertices() { return vertices; }
    public List<Face> faces() { return faces; }

    public static MeshGeometry fromCube(CubeGeometry cube) {
        double hx = cube.width() * 0.5;
        double hy = cube.height() * 0.5;
        double hz = cube.depth() * 0.5;

        List<Vertex> vertices = List.of(
                new Vertex(-hx, -hy, -hz), // 0
                new Vertex( hx, -hy, -hz), // 1
                new Vertex( hx,  hy, -hz), // 2
                new Vertex(-hx,  hy, -hz), // 3
                new Vertex(-hx, -hy,  hz), // 4
                new Vertex( hx, -hy,  hz), // 5
                new Vertex( hx,  hy,  hz), // 6
                new Vertex(-hx,  hy,  hz)  // 7
        );

        List<Face> faces = List.of(
                new Face(1, 5, 6, 2), // +X
                new Face(4, 0, 3, 7), // -X
                new Face(3, 2, 6, 7), // +Y
                new Face(4, 5, 1, 0), // -Y
                new Face(5, 4, 7, 6), // +Z
                new Face(0, 1, 2, 3)  // -Z
        );

        return new MeshGeometry(vertices, faces);
    }

    public MeshGeometry copy() {
        return new MeshGeometry(vertices, faces);
    }
}
