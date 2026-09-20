package whitevoid.create.ui;

import net.minecraft.client.gui.DrawContext;
import whitevoid.create.editor.viewport.ViewportContext;

/**
 * First visual pass of CREATE. This deliberately uses Minecraft's DrawContext
 * while the 3D render pipeline is prepared; no raw OpenGL state is owned here.
 */
public final class ViewportRenderer {
    public void render(DrawContext context, int width, int height, ViewportContext viewport) {
        int left = 16;
        int top = 16;
        int right = width - 16;
        int bottom = height - 16;

        context.fill(left, top, right, bottom, 0xFF111216);

        if (viewport.viewport().gridVisible()) {
            drawGrid(context, left, top, right, bottom);
        }

        context.drawTextWithShadow(
                net.minecraft.client.MinecraftClient.getInstance().textRenderer,
                "CREATE • Viewport",
                left + 10,
                top + 10,
                0xFFE8E8E8
        );

        String camera = viewport.viewport().camera().mode().name();
        context.drawTextWithShadow(
                net.minecraft.client.MinecraftClient.getInstance().textRenderer,
                camera + "  |  Zoom " + String.format("%.2f", viewport.viewport().camera().distance()),
                left + 10,
                top + 25,
                0xFFAAAAAA
        );
    }

    private void drawGrid(DrawContext context, int left, int top, int right, int bottom) {
        int centerX = (left + right) / 2;
        int centerY = (top + bottom) / 2;
        int spacing = 24;
        int color = 0xFF24262C;

        for (int x = centerX; x < right; x += spacing) context.fill(x, top, x + 1, bottom, color);
        for (int x = centerX - spacing; x > left; x -= spacing) context.fill(x, top, x + 1, bottom, color);
        for (int y = centerY; y < bottom; y += spacing) context.fill(left, y, right, y + 1, color);
        for (int y = centerY - spacing; y > top; y -= spacing) context.fill(left, y, right, y + 1, color);

        context.fill(centerX, top, centerX + 2, bottom, 0xFF3B3E47);
        context.fill(left, centerY, right, centerY + 2, 0xFF3B3E47);
    }
}
