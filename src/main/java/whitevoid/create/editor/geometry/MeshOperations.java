package whitevoid.create.editor.geometry;

import java.util.ArrayList;
import java.util.List;
import whitevoid.create.model.MeshGeometry;

public final class MeshOperations {
    private MeshOperations() {}

    public static MeshGeometry insetFace(MeshGeometry mesh, int faceIndex, double amount) {
        if (mesh == null) throw new IllegalArgumentException("Mesh cannot be null");
        if (faceIndex < 0 || faceIndex >= mesh.faces().size()) {
            throw new IllegalArgumentException("Invalid face index: " + faceIndex);
        }
        if (amount <= 0.0) return mesh.copy();

        MeshGeometry.Face selected = mesh.faces().get(faceIndex);
        int[] indices = selected.vertices();
        if (indices.length < 3) {
            throw new IllegalArgumentException("Inset requires a polygon face");
        }

        List<MeshGeometry.Vertex> vertices = new ArrayList<>(mesh.vertices());
        List<MeshGeometry.Face> faces = new ArrayList<>();
        for (int i = 0; i < mesh.faces().size(); i++) {
            if (i != faceIndex) faces.add(mesh.faces().get(i));
        }

        int[] inner = new int[indices.length];
        double cx = 0.0, cy = 0.0, cz = 0.0;
        for (int index : indices) {
            var v = vertices.get(index);
            cx += v.x(); cy += v.y(); cz += v.z();
        }
        cx /= indices.length; cy /= indices.length; cz /= indices.length;

        double factor = Math.max(0.0, Math.min(0.99, amount));
        for (int i = 0; i < indices.length; i++) {
            var v = vertices.get(indices[i]);
            inner[i] = vertices.size();
            vertices.add(new MeshGeometry.Vertex(
                    v.x() + (cx - v.x()) * factor,
                    v.y() + (cy - v.y()) * factor,
                    v.z() + (cz - v.z()) * factor));
        }

        for (int i = 0; i < indices.length; i++) {
            int next = (i + 1) % indices.length;
            faces.add(new MeshGeometry.Face(
                    indices[i], indices[next], inner[next], inner[i]));
        }

        faces.add(new MeshGeometry.Face(inner));
        return new MeshGeometry(vertices, faces);
    }
}
