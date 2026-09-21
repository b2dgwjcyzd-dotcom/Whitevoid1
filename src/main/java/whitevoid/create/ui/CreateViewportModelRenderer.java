package whitevoid.create.ui;

import net.minecraft.client.gui.DrawContext;
import whitevoid.create.editor.viewport.ViewportContext;
import whitevoid.create.editor.geometry.MeshSelectionMode;
import whitevoid.create.model.MeshGeometry;
import whitevoid.create.model.Model;
import whitevoid.create.model.ModelNode;
import whitevoid.create.model.ModelRenderer;
import whitevoid.create.model.TransformMath;
import whitevoid.create.ui.ViewportProjector.Point;

public final class CreateViewportModelRenderer {
    private final ModelRenderer modelRenderer = new ModelRenderer();

    public void render(DrawContext context, ViewportProjector projector, Model model,
                       ViewportContext viewport, int cx, int cy,
                       int left, int top, int right, int bottom) {
        modelRenderer.renderMesh(model, (node, mesh) ->
                drawModelMesh(context, projector, node, mesh, viewport,
                        cx, cy, left, top, right, bottom));
    }

    private void drawModelMesh(DrawContext context, ViewportProjector projector, ModelNode node,
                               MeshGeometry mesh, ViewportContext viewport,
                               int cx, int cy, int left, int top, int right, int bottom) {
        Point[] points = new Point[mesh.vertices().size()];
        for (int i = 0; i < points.length; i++) {
            MeshGeometry.Vertex v = mesh.vertices().get(i);
            TransformMath.Point world = TransformMath.applyHierarchy(
                    new TransformMath.Point(v.x(), v.y(), v.z()), node);
            points[i] = projector.project(world.x(), world.y(), world.z(), cx, cy, 300.0);
        }

        boolean selected = viewport.selection().selection().contains(node.id());
        int color = selected ? 0xFFFFFFFF : 0xFFBFC3CC;

        for (int[] edge : ModelRenderer.meshEdges(mesh)) {
            Point a = points[edge[0]], b = points[edge[1]];
            if (a == null || b == null) continue;
            drawLine(context, a, b, left, top, right, bottom, color);
        }

        if (selected && viewport.transform().mode() == whitevoid.create.editor.transform.TransformMode.GEOMETRY) {
            var components = viewport.meshComponentSelection();
            if (components.matches(node)) {
                if (components.mode() == MeshSelectionMode.VERTEX) {
                    for (int index : components.vertexIndices()) {
                        if (index < 0 || index >= points.length || points[index] == null) continue;
                        Point point = points[index];
                        int x = (int) Math.round(point.x()), y = (int) Math.round(point.y());
                        int marker = index == components.activeVertex() ? 0xFFFFFFFF : 0xFFD6D9E2;
                        context.fill(x - 3, y - 3, x + 4, y + 4, marker);
                        if (index == components.activeVertex()) {
                            context.fill(x - 5, y - 1, x + 6, y + 1, 0xFFFFFFFF);
                            context.fill(x - 1, y - 5, x + 1, y + 6, 0xFFFFFFFF);
                        }
                    }
                } else if (components.mode() == MeshSelectionMode.EDGE) {
                    for (int[] edge : components.edgeIndices()) {
                        if (edge[0] < 0 || edge[1] < 0 || edge[0] >= points.length || edge[1] >= points.length) continue;
                        Point a = points[edge[0]], b = points[edge[1]];
                        if (a == null || b == null) continue;
                        boolean active = edge[0] == components.activeEdgeA() && edge[1] == components.activeEdgeB()
                                || edge[0] == components.activeEdgeB() && edge[1] == components.activeEdgeA();
                        int edgeColor = active ? 0xFFFFFFFF : 0xFFD6D9E2;
                        drawLine(context, a, b, left, top, right, bottom, edgeColor);
                        drawLine(context, new Point(a.x() + 1, a.y(), a.depth()),
                                new Point(b.x() + 1, b.y(), b.depth()),
                                left, top, right, bottom, active ? 0xFFFFFFFF : 0xFFE7E9EF);
                    }
                }
            } else {
                for (Point point : points) {
                    if (point == null) continue;
                    int x = (int) Math.round(point.x()), y = (int) Math.round(point.y());
                    context.fill(x - 2, y - 2, x + 3, y + 3, 0xFFFFFFFF);
                }
            }
        } else if (selected) {
            for (Point point : points) {
                if (point == null) continue;
                int x = (int) Math.round(point.x()), y = (int) Math.round(point.y());
                context.fill(x - 2, y - 2, x + 3, y + 3, 0xFFFFFFFF);
            }
        }
    }

    private void drawLine(DrawContext context, Point a, Point b,
                          int left, int top, int right, int bottom, int color) {
        int x0 = (int) Math.round(a.x()), y0 = (int) Math.round(a.y());
        int x1 = (int) Math.round(b.x()), y1 = (int) Math.round(b.y());
        int dx = Math.abs(x1 - x0), dy = Math.abs(y1 - y0);
        int sx = x0 < x1 ? 1 : -1, sy = y0 < y1 ? 1 : -1, err = dx - dy;
        while (true) {
            if (x0 >= left && x0 < right && y0 >= top && y0 < bottom) {
                context.fill(x0, y0, x0 + 1, y0 + 1, color);
            }
            if (x0 == x1 && y0 == y1) break;
            int e2 = 2 * err;
            if (e2 > -dy) { err -= dy; x0 += sx; }
            if (e2 < dx) { err += dx; y0 += sy; }
        }
    }
}
