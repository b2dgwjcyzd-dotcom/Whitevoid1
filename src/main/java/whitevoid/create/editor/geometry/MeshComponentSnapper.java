package whitevoid.create.editor.geometry;

import java.util.LinkedHashSet;
import java.util.Set;
import whitevoid.create.model.MeshGeometry;

public final class MeshComponentSnapper {
    private MeshComponentSnapper() {}

    public static MeshGeometry snap(MeshGeometry mesh, Set<Integer> ids, double increment) {
        if (increment <= 0.0) return mesh;
        java.util.ArrayList<MeshGeometry.Vertex> vertices = new java.util.ArrayList<>(mesh.vertices());
        for (int id : ids) {
            if (id < 0 || id >= vertices.size()) continue;
            var v = vertices.get(id);
            vertices.set(id, new MeshGeometry.Vertex(
                    snapValue(v.x(), increment),
                    snapValue(v.y(), increment),
                    snapValue(v.z(), increment)
            ));
        }
        return new MeshGeometry(vertices, mesh.faces());
    }

    private static double snapValue(double value, double increment) {
        return Math.rint(value / increment) * increment;
    }
}
