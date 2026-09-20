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

    public static MeshGeometry extrudeEdge(MeshGeometry mesh, int a, int b, double amount) {
        validateEdge(mesh, a, b);
        if (amount == 0.0) return mesh.copy();

        List<Integer> adjacent = adjacentFaces(mesh, a, b);
        if (adjacent.isEmpty()) throw new IllegalArgumentException("Edge is not connected to a face");

        double nx = 0.0, ny = 0.0, nz = 0.0;
        for (int faceIndex : adjacent) {
            double[] n = faceNormal(mesh, mesh.faces().get(faceIndex));
            nx += n[0]; ny += n[1]; nz += n[2];
        }
        double len = Math.sqrt(nx * nx + ny * ny + nz * nz);
        if (len < 1e-9) throw new IllegalArgumentException("Cannot extrude degenerate edge");
        nx /= len; ny /= len; nz /= len;

        List<MeshGeometry.Vertex> vertices = new ArrayList<>(mesh.vertices());
        int na = vertices.size();
        var va = mesh.vertices().get(a);
        vertices.add(new MeshGeometry.Vertex(va.x() + nx * amount, va.y() + ny * amount, va.z() + nz * amount));
        int nb = vertices.size();
        var vb = mesh.vertices().get(b);
        vertices.add(new MeshGeometry.Vertex(vb.x() + nx * amount, vb.y() + ny * amount, vb.z() + nz * amount));

        List<MeshGeometry.Face> faces = new ArrayList<>(mesh.faces());
        faces.add(new MeshGeometry.Face(a, b, nb, na));
        return new MeshGeometry(vertices, faces);
    }

    public static MeshGeometry bevelEdge(MeshGeometry mesh, int a, int b, double amount) {
        validateEdge(mesh, a, b);
        if (amount <= 0.0) return mesh.copy();

        List<Integer> adjacent = adjacentFaces(mesh, a, b);
        if (adjacent.size() != 2) {
            throw new IllegalArgumentException("Bevel currently requires exactly two adjacent faces");
        }

        double edgeLength = distance(mesh.vertices().get(a), mesh.vertices().get(b));
        double offset = Math.min(amount, edgeLength * 0.49);

        List<MeshGeometry.Vertex> vertices = new ArrayList<>(mesh.vertices());
        int[] newA = new int[2];
        int[] newB = new int[2];

        for (int i = 0; i < 2; i++) {
            var face = mesh.faces().get(adjacent.get(i));
            double[] ca = centroid(mesh, face);
            var va = mesh.vertices().get(a);
            var vb = mesh.vertices().get(b);

            newA[i] = vertices.size();
            vertices.add(toward(va, ca, offset));
            newB[i] = vertices.size();
            vertices.add(toward(vb, ca, offset));
        }

        List<MeshGeometry.Face> faces = new ArrayList<>();
        for (int i = 0; i < mesh.faces().size(); i++) {
            if (i == adjacent.get(0) || i == adjacent.get(1)) continue;
            faces.add(mesh.faces().get(i));
        }

        for (int side = 0; side < 2; side++) {
            int faceIndex = adjacent.get(side);
            int[] original = mesh.faces().get(faceIndex).vertices();
            int[] replaced = original.clone();
            for (int i = 0; i < replaced.length; i++) {
                int current = replaced[i];
                int next = replaced[(i + 1) % replaced.length];
                if (current == a && next == b) {
                    replaced[i] = newA[side];
                    replaced[(i + 1) % replaced.length] = newB[side];
                } else if (current == b && next == a) {
                    replaced[i] = newB[side];
                    replaced[(i + 1) % replaced.length] = newA[side];
                }
            }
            faces.add(new MeshGeometry.Face(replaced));
        }

        faces.add(new MeshGeometry.Face(newA[0], newB[0], newB[1], newA[1]));
        return new MeshGeometry(vertices, faces);
    }

    private static void validateEdge(MeshGeometry mesh, int a, int b) {
        if (mesh == null) throw new IllegalArgumentException("Mesh cannot be null");
        if (a < 0 || b < 0 || a >= mesh.vertices().size() || b >= mesh.vertices().size() || a == b) {
            throw new IllegalArgumentException("Invalid edge");
        }
    }

    private static List<Integer> adjacentFaces(MeshGeometry mesh, int a, int b) {
        List<Integer> result = new ArrayList<>();
        for (int i = 0; i < mesh.faces().size(); i++) {
            int[] indices = mesh.faces().get(i).vertices();
            for (int j = 0; j < indices.length; j++) {
                int next = indices[(j + 1) % indices.length];
                if ((indices[j] == a && next == b) || (indices[j] == b && next == a)) {
                    result.add(i);
                    break;
                }
            }
        }
        return result;
    }

    private static double[] faceNormal(MeshGeometry mesh, MeshGeometry.Face face) {
        int[] ids = face.vertices();
        var a = mesh.vertices().get(ids[0]);
        var b = mesh.vertices().get(ids[1]);
        var c = mesh.vertices().get(ids[2]);
        double ux = b.x() - a.x(), uy = b.y() - a.y(), uz = b.z() - a.z();
        double vx = c.x() - a.x(), vy = c.y() - a.y(), vz = c.z() - a.z();
        double nx = uy * vz - uz * vy, ny = uz * vx - ux * vz, nz = ux * vy - uy * vx;
        double len = Math.sqrt(nx * nx + ny * ny + nz * nz);
        return len < 1e-9 ? new double[]{0, 0, 0} : new double[]{nx / len, ny / len, nz / len};
    }

    private static double[] centroid(MeshGeometry mesh, MeshGeometry.Face face) {
        int[] ids = face.vertices();
        double x = 0, y = 0, z = 0;
        for (int id : ids) {
            var v = mesh.vertices().get(id);
            x += v.x(); y += v.y(); z += v.z();
        }
        return new double[]{x / ids.length, y / ids.length, z / ids.length};
    }

    private static MeshGeometry.Vertex toward(MeshGeometry.Vertex v, double[] target, double distance) {
        double dx = target[0] - v.x(), dy = target[1] - v.y(), dz = target[2] - v.z();
        double len = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (len < 1e-9) return v;
        double factor = distance / len;
        return new MeshGeometry.Vertex(v.x() + dx * factor, v.y() + dy * factor, v.z() + dz * factor);
    }

    private static double distance(MeshGeometry.Vertex a, MeshGeometry.Vertex b) {
        double dx = a.x() - b.x(), dy = a.y() - b.y(), dz = a.z() - b.z();
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
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

    /** Extrudes a selected face region as one connected operation. */
    public static MeshGeometry extrudeFaces(MeshGeometry mesh, java.util.Set<Integer> selectedFaces, double amount) {
        if (mesh == null) throw new IllegalArgumentException("Mesh cannot be null");
        if (selectedFaces == null || selectedFaces.isEmpty() || amount == 0.0) return mesh.copy();

        java.util.Set<Integer> valid = new java.util.LinkedHashSet<>();
        for (int face : selectedFaces) {
            if (face >= 0 && face < mesh.faces().size()) valid.add(face);
        }
        if (valid.isEmpty()) return mesh.copy();

        java.util.Map<Integer, double[]> normals = new java.util.LinkedHashMap<>();
        java.util.Set<Integer> affected = new java.util.LinkedHashSet<>();
        for (int faceIndex : valid) {
            MeshGeometry.Face face = mesh.faces().get(faceIndex);
            double[] n = faceNormal(mesh, face);
            for (int id : face.vertices()) {
                affected.add(id);
                double[] sum = normals.computeIfAbsent(id, ignored -> new double[3]);
                sum[0] += n[0]; sum[1] += n[1]; sum[2] += n[2];
            }
        }

        List<MeshGeometry.Vertex> vertices = new ArrayList<>(mesh.vertices());
        java.util.Map<Integer, Integer> duplicate = new java.util.LinkedHashMap<>();
        for (int id : affected) {
            double[] n = normals.get(id);
            double len = Math.sqrt(n[0] * n[0] + n[1] * n[1] + n[2] * n[2]);
            if (len < 1e-9) { n[0] = 0; n[1] = 1; n[2] = 0; }
            else { n[0] /= len; n[1] /= len; n[2] /= len; }
            MeshGeometry.Vertex v = mesh.vertices().get(id);
            int copy = vertices.size();
            vertices.add(new MeshGeometry.Vertex(v.x() + n[0] * amount,
                    v.y() + n[1] * amount, v.z() + n[2] * amount));
            duplicate.put(id, copy);
        }

        List<MeshGeometry.Face> faces = new ArrayList<>();
        for (int i = 0; i < mesh.faces().size(); i++) {
            if (!valid.contains(i)) faces.add(mesh.faces().get(i));
        }

        for (int faceIndex : valid) {
            int[] original = mesh.faces().get(faceIndex).vertices();
            int[] moved = new int[original.length];
            for (int i = 0; i < original.length; i++) moved[i] = duplicate.get(original[i]);
            reverse(moved);
            faces.add(new MeshGeometry.Face(moved));
        }

        for (int faceIndex : valid) {
            int[] original = mesh.faces().get(faceIndex).vertices();
            for (int i = 0; i < original.length; i++) {
                int a = original[i];
                int b = original[(i + 1) % original.length];
                if (!isSelectedEdge(mesh, a, b, valid)) {
                    faces.add(new MeshGeometry.Face(a, b, duplicate.get(b), duplicate.get(a)));
                }
            }
        }
        return new MeshGeometry(vertices, faces);
    }

    private static boolean isSelectedEdge(MeshGeometry mesh, int a, int b,
                                           java.util.Set<Integer> selectedFaces) {
        int count = 0;
        for (int face : selectedFaces) {
            int[] ids = mesh.faces().get(face).vertices();
            for (int i = 0; i < ids.length; i++) {
                if (sameEdge(ids[i], ids[(i + 1) % ids.length], a, b)) {
                    count++;
                    break;
                }
            }
        }
        return count >= 2;
    }

    private static boolean sameEdge(int a, int b, int c, int d) {
        return (a == c && b == d) || (a == d && b == c);
    }

    private static void reverse(int[] values) {
        for (int i = 0, j = values.length - 1; i < j; i++, j--) {
            int temp = values[i]; values[i] = values[j]; values[j] = temp;
        }
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
