package whitevoid.create.editor.geometry;

import java.util.ArrayList;
import java.util.List;
import whitevoid.create.model.MeshGeometry;

/**
 * Vertex-focused mesh modeling operations for CREATE.
 *
 * <p>This class intentionally owns only vertex operations. Edge and face
 * operations live in their dedicated operation classes.</p>
 */
public final class MeshVertexOperations {
    private MeshVertexOperations() {}

    public static MeshGeometry moveVertex(MeshGeometry mesh, int vertexIndex,
                                           double dx, double dy, double dz) {
        if (mesh == null) throw new IllegalArgumentException("Mesh cannot be null");
        if (vertexIndex < 0 || vertexIndex >= mesh.vertices().size()) {
            throw new IllegalArgumentException("Invalid vertex index: " + vertexIndex);
        }

        List<MeshGeometry.Vertex> vertices = new ArrayList<>(mesh.vertices());
        MeshGeometry.Vertex vertex = vertices.get(vertexIndex);
        vertices.set(vertexIndex, new MeshGeometry.Vertex(
                vertex.x() + dx,
                vertex.y() + dy,
                vertex.z() + dz));
        return new MeshGeometry(vertices, mesh.faces());
    }
}
