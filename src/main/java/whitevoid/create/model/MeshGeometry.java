package whitevoid.create.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public final class MeshGeometry {
    public record Vertex(double x, double y, double z) {
        public Vertex {
            requireFinite(x, "x");
            requireFinite(y, "y");
            requireFinite(z, "z");
        }
    }

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
        Objects.requireNonNull(vertices, "vertices");
        Objects.requireNonNull(faces, "faces");

        List<Vertex> vertexCopy = List.copyOf(vertices);
        List<Face> faceCopy = new ArrayList<>(faces.size());

        for (Face face : faces) {
            Objects.requireNonNull(face, "face");
            int[] indices = face.vertices();

            for (int index : indices) {
                if (index < 0 || index >= vertexCopy.size()) {
                    throw new IllegalArgumentException(
                            "Face references invalid vertex: " + index);
                }
            }

            faceCopy.add(face);
        }

        this.vertices = vertexCopy;
        this.faces = Collections.unmodifiableList(faceCopy);
    }

    public List<Vertex> vertices() { return vertices; }
    public List<Face> faces() { return faces; }

    /** Returns whether this mesh is exactly the canonical cube topology for the given dimensions. */
    public boolean matchesCube(CubeGeometry cube) {
        if (cube == null) return false;
        MeshGeometry expected = fromCube(cube);
        if (!vertices.equals(expected.vertices) || faces.size() != expected.faces.size()) return false;

        for (int i = 0; i < faces.size(); i++) {
            if (!java.util.Arrays.equals(faces.get(i).vertices(), expected.faces.get(i).vertices())) {
                return false;
            }
        }
        return true;
    }

    public static MeshGeometry fromCube(CubeGeometry cube) {
        Objects.requireNonNull(cube, "cube");

        double hx = cube.width() * 0.5;
        double hy = cube.height() * 0.5;
        double hz = cube.depth() * 0.5;

        List<Vertex> vertices = List.of(
                new Vertex(-hx, -hy, -hz),
                new Vertex( hx, -hy, -hz),
                new Vertex( hx,  hy, -hz),
                new Vertex(-hx,  hy, -hz),
                new Vertex(-hx, -hy,  hz),
                new Vertex( hx, -hy,  hz),
                new Vertex( hx,  hy,  hz),
                new Vertex(-hx,  hy,  hz)
        );

        List<Face> faces = List.of(
                new Face(1, 5, 6, 2),
                new Face(4, 0, 3, 7),
                new Face(3, 2, 6, 7),
                new Face(4, 5, 1, 0),
                new Face(5, 4, 7, 6),
                new Face(0, 1, 2, 3)
        );

        return new MeshGeometry(vertices, faces);
    }

    public MeshGeometry copy() {
        return new MeshGeometry(vertices, faces);
    }

    private static void requireFinite(double value, String name) {
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException(name + " must be finite");
        }
    }
}
