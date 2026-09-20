package whitevoid.create.model;

import java.util.function.BiConsumer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public final class ModelRenderer {
    private static final int[][] EDGES = {
            {0, 1}, {1, 2}, {2, 3}, {3, 0},
            {4, 5}, {5, 6}, {6, 7}, {7, 4},
            {0, 4}, {1, 5}, {2, 6}, {3, 7}
    };

    public void render(Model model, BiConsumer<ModelNode, TransformMath.Point[]> cubeConsumer) {
        for (ModelNode node : model.allNodes()) {
            CubeGeometry geometry = node.geometry();
            if (geometry == null) continue;

            double hx = geometry.width() / 2.0;
            double hy = geometry.height() / 2.0;
            double hz = geometry.depth() / 2.0;

            TransformMath.Point[] corners = {
                    corner(-hx, -hy, -hz, node), corner(hx, -hy, -hz, node),
                    corner(hx, hy, -hz, node), corner(-hx, hy, -hz, node),
                    corner(-hx, -hy, hz, node), corner(hx, -hy, hz, node),
                    corner(hx, hy, hz, node), corner(-hx, hy, hz, node)
            };

            cubeConsumer.accept(node, corners);
        }
    }

    private TransformMath.Point corner(double x, double y, double z, ModelNode node) {
        return TransformMath.applyHierarchy(new TransformMath.Point(x, y, z), node);
    }

    public void renderMesh(Model model, BiConsumer<ModelNode, MeshGeometry> meshConsumer) {
        for (ModelNode node : model.allNodes()) {
            MeshGeometry mesh = node.meshGeometry();
            if (mesh == null && node.geometry() != null) {
                mesh = node.ensureMeshGeometry();
            }
            if (mesh != null) meshConsumer.accept(node, mesh);
        }
    }

    public static int[][] meshEdges(MeshGeometry mesh) {
        Set<Long> unique = new HashSet<>();
        ArrayList<int[]> result = new ArrayList<>();
        for (MeshGeometry.Face face : mesh.faces()) {
            int[] indices = face.vertices();
            for (int i = 0; i < indices.length; i++) {
                int a = indices[i];
                int b = indices[(i + 1) % indices.length];
                int lo = Math.min(a, b);
                int hi = Math.max(a, b);
                long key = ((long) lo << 32) | (hi & 0xffffffffL);
                if (unique.add(key)) result.add(new int[]{a, b});
            }
        }
        return result.toArray(new int[0][]);
    }

    public static int[][] edges() {
        return EDGES;
    }
}
