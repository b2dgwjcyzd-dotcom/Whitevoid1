package whitevoid.create.editor.geometry;

import java.util.ArrayList;
import java.util.List;
import whitevoid.create.model.MeshGeometry;

/**
 * Shared geometric queries used by CREATE mesh operations.
 */
final class MeshOperationGeometry {
    private MeshOperationGeometry() {}

    static long edgeKey(int a, int b) {
        int lo = Math.min(a, b);
        int hi = Math.max(a, b);
        return ((long) lo << 32) | (hi & 0xffffffffL);
    }

    static void validateEdge(MeshGeometry mesh, int a, int b) {
        if (mesh == null) throw new IllegalArgumentException("Mesh cannot be null");
        if (a < 0 || b < 0 || a >= mesh.vertices().size() || b >= mesh.vertices().size() || a == b) {
            throw new IllegalArgumentException("Invalid edge");
        }
    }

    static List<Integer> adjacentFaces(MeshGeometry mesh, int a, int b) {
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

    static double[] faceNormal(MeshGeometry mesh, MeshGeometry.Face face) {
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

    static double[] centroid(MeshGeometry mesh, MeshGeometry.Face face) {
        int[] ids = face.vertices();
        double x = 0, y = 0, z = 0;
        for (int id : ids) {
            var v = mesh.vertices().get(id);
            x += v.x(); y += v.y(); z += v.z();
        }
        return new double[]{x / ids.length, y / ids.length, z / ids.length};
    }

    static MeshGeometry.Vertex toward(MeshGeometry.Vertex v, double[] target, double distance) {
        double dx = target[0] - v.x(), dy = target[1] - v.y(), dz = target[2] - v.z();
        double len = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (len < 1e-9) return v;
        double factor = distance / len;
        return new MeshGeometry.Vertex(v.x() + dx * factor, v.y() + dy * factor, v.z() + dz * factor);
    }

    static double distance(MeshGeometry.Vertex a, MeshGeometry.Vertex b) {
        double dx = a.x() - b.x(), dy = a.y() - b.y(), dz = a.z() - b.z();
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    static boolean sameEdge(int a, int b, int c, int d) {
        return (a == c && b == d) || (a == d && b == c);
    }

    static void reverse(int[] values) {
        for (int i = 0, j = values.length - 1; i < j; i++, j--) {
            int temp = values[i]; values[i] = values[j]; values[j] = temp;
        }
    }

    static boolean isSelectedEdge(MeshGeometry mesh, int a, int b, java.util.Set<Integer> selectedFaces) {
        List<Integer> adjacent = adjacentFaces(mesh, a, b);
        if (adjacent.size() < 2) return false;

        int selectedCount = 0;
        for (int face : adjacent) {
            if (selectedFaces.contains(face)) selectedCount++;
        }

        // An edge is internal to the selected region only when every face
        // incident to it is selected. On non-manifold topology, treating
        // "two selected faces" as internal would hide the boundary against
        // an unselected third face.
        return selectedCount == adjacent.size();
    }
}
