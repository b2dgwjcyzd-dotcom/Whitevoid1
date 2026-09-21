package whitevoid.create.ui;

import net.minecraft.client.gui.DrawContext;
import whitevoid.create.ui.ViewportProjector.Point;

/**
 * Renders the modeling grid and world axes for the CREATE viewport.
 *
 * <p>Grid spacing grows with camera distance so the workspace keeps a
 * readable number of lines instead of becoming a dense wall of pixels.</p>
 */
public final class CreateViewportGridRenderer {
    public void renderGrid(DrawContext context, ViewportProjector projector,
                           int centerX, int centerY, int left, int top, int right, int bottom) {
        int step = gridStep(projector.cameraDistance());
        int extent = step * 8;

        for (int i = -extent; i <= extent; i += step) {
            Point a = projector.project(i, 0, -extent, centerX, centerY, 300);
            Point b = projector.project(i, 0, extent, centerX, centerY, 300);
            if (a != null && b != null) {
                drawLine(context, a, b, left, top, right, bottom, 0xFF24262C);
            }

            a = projector.project(-extent, 0, i, centerX, centerY, 300);
            b = projector.project(extent, 0, i, centerX, centerY, 300);
            if (a != null && b != null) {
                drawLine(context, a, b, left, top, right, bottom, 0xFF24262C);
            }
        }
    }

    private int gridStep(double distance) {
        if (distance >= 128.0) return 16;
        if (distance >= 64.0) return 8;
        if (distance >= 32.0) return 4;
        if (distance >= 16.0) return 2;
        return 1;
    }

    public void renderAxes(DrawContext context, ViewportProjector projector,
                           int centerX, int centerY, int left, int top, int right, int bottom) {
        Point origin = projector.project(0, 0, 0, centerX, centerY, 300);
        Point x = projector.project(3, 0, 0, centerX, centerY, 300);
        Point y = projector.project(0, 3, 0, centerX, centerY, 300);
        Point z = projector.project(0, 0, 3, centerX, centerY, 300);
        if (origin == null) return;

        if (x != null) drawLine(context, origin, x, left, top, right, bottom, 0xFFB96B6B);
        if (y != null) drawLine(context, origin, y, left, top, right, bottom, 0xFF78B978);
        if (z != null) drawLine(context, origin, z, left, top, right, bottom, 0xFF6B88C8);
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
            if (e2 > -dy) {
                err -= dy;
                x0 += sx;
            }
            if (e2 < dx) {
                err += dx;
                y0 += sy;
            }
        }
    }
}
