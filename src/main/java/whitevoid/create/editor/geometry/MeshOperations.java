package whitevoid.create.editor.geometry;

import java.util.ArrayList;
import java.util.List;
import whitevoid.create.model.MeshGeometry;

public final class MeshOperations {
    private MeshOperations() {}

    public static MeshGeometry moveVertex(MeshGeometry mesh, int vertexIndex,
                                             double dx, double dy, double dz) {
        if (mesh == null) throw new IllegalArgumentException("Mesh cannot be null");
        if (vertexIndex < 0 || vertexIndex >= mesh.vertices().size()) {
            throw new IllegalArgumentException("Invalid vertex index: " + vertexIndex);
        }

        List<MeshGeometry.Vertex> vertices = new ArrayList<>(mesh.vertices());
        var v = vertices.get(vertexIndex);
        vertices.set(vertexIndex, new MeshGeometry.Vertex(
                v.x() + dx, v.y() + dy, v.z() + dz));
        return new MeshGeometry(vertices, mesh.faces());
    }

    public static MeshGeometry extrudeFace(MeshGeometry mesh, int faceIndex, double amount) {
        if (mesh == null) throw new IllegalArgumentException("Mesh cannot be null");
        if (faceIndex < 0 || faceIndex >= mesh.faces().size()) {
            throw new IllegalArgumentException("Invalid face index: " + faceIndex);
        }
        if (amount == 0.0) return mesh.copy();

        int[] indices = mesh.faces().get(faceIndex).vertices();
        if (indices.length < 3) {
            throw new IllegalArgumentException("Extrude requires a polygon face");
        }

        var a = mesh.vertices().get(indices[0]);
        var b = mesh.vertices().get(indices[1]);
        var d = mesh.vertices().get(indices[2]);

        double ux = b.x() - a.x();
        double uy = b.y() - a.y();
        double uz = b.z() - a.z();
        double vx = d.x() - a.x();
        double vy = d.y() - a.y();
        double vz = d.z() - a.z();

        double nx = uy * vz - uz * vy;
        double ny = uz * vx - ux * vz;
        double nz = ux * vy - uy * vx;
        double length = Math.sqrt(nx * nx + ny * ny + nz * nz);
        if (length < 1e-9) {
            throw new IllegalArgumentException("Cannot extrude a degenerate face");
        }
        nx /= length;
        ny /= length;
        nz /= length;

        List<MeshGeometry.Vertex> vertices = new ArrayList<>(mesh.vertices());
        int[] extruded = new int[indices.length];

        for (int i = 0; i < indices.length; i++) {
            var vtx = vertices.get(indices[i]);
            extruded[i] = vertices.size();
            vertices.add(new MeshGeometry.Vertex(
                    vtx.x() + nx * amount,
                    vtx.y() + ny * amount,
                    vtx.z() + nz * amount));
        }

        List<MeshGeometry.Face> faces = new ArrayList<>();
        for (int i = 0; i < mesh.faces().size(); i++) {
            if (i != faceIndex) faces.add(mesh.faces().get(i));
        }

        for (int i = 0; i < indices.length; i++) {
            int next = (i + 1) % indices.length;
            faces.add(new MeshGeometry.Face(
                    indices[i], indices[next], extruded[next], extruded[i]));
        }

        int[] cap = new int[extruded.length];
        for (int i = 0; i < extruded.length; i++) {
            cap[i] = extruded[extruded.length - 1 - i];
        }
        faces.add(new MeshGeometry.Face(cap));

        return new MeshGeometry(vertices, faces);
    }

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
