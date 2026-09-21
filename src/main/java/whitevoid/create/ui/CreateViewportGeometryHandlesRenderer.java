package whitevoid.create.ui;

import net.minecraft.client.gui.DrawContext;
import whitevoid.create.model.ModelNode;
import whitevoid.create.model.TransformMath;
import whitevoid.create.ui.ViewportProjector.Point;

public final class CreateViewportGeometryHandlesRenderer {
    public void render(DrawContext context, ViewportProjector projector, ModelNode node,
                       int cx, int cy, int left, int top, int right, int bottom,
                       ViewportGizmo.Axis hoveredAxis) {
        if (node.geometry() == null) return;

        double[] half = {
                node.geometry().width() * 0.5,
                node.geometry().height() * 0.5,
                node.geometry().depth() * 0.5
        };
        double[][] dirs = {{1,0,0},{0,1,0},{0,0,1}};
        int[] colors = {0xFFE06B6B, 0xFF70C878, 0xFF6B8EDC};
        ViewportGizmo.Axis[] positive = {
                ViewportGizmo.Axis.X, ViewportGizmo.Axis.Y, ViewportGizmo.Axis.Z
        };
        ViewportGizmo.Axis[] negative = {
                ViewportGizmo.Axis.NEG_X, ViewportGizmo.Axis.NEG_Y, ViewportGizmo.Axis.NEG_Z
        };

        TransformMath.Point center3 = TransformMath.applyHierarchy(
                new TransformMath.Point(0,0,0), node);
        Point center = projector.project(center3.x(), center3.y(), center3.z(), cx, cy, 300);
        if (center == null) return;

        for (int i = 0; i < 3; i++) {
            for (int sign : new int[]{1, -1}) {
                TransformMath.Point local = new TransformMath.Point(
                        dirs[i][0] * half[i] * sign,
                        dirs[i][1] * half[i] * sign,
                        dirs[i][2] * half[i] * sign);
                TransformMath.Point world = TransformMath.applyHierarchy(local, node);
                Point p = projector.project(world.x(), world.y(), world.z(), cx, cy, 300);
                if (p == null) continue;

                ViewportGizmo.Axis axis = sign > 0 ? positive[i] : negative[i];
                int color = hoveredAxis == axis ? 0xFFFFFFFF : colors[i];
                drawLine(context, center, p, left, top, right, bottom, color);
                drawHandle(context, p, color);
            }
        }
    }

    private void drawHandle(DrawContext context, Point p, int color) {
        int x = (int)Math.round(p.x()), y = (int)Math.round(p.y());
        context.fill(x - 4, y - 4, x + 5, y + 5, 0xFF111216);
        context.fill(x - 3, y - 3, x + 4, y + 4, color);
    }

    private void drawLine(DrawContext context, Point a, Point b,
                          int left, int top, int right, int bottom, int color) {
        int x0 = (int)Math.round(a.x()), y0 = (int)Math.round(a.y());
        int x1 = (int)Math.round(b.x()), y1 = (int)Math.round(b.y());
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
