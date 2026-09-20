package whitevoid.create.model;

import java.util.function.BiConsumer;

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

    public static int[][] edges() {
        return EDGES;
    }
}
