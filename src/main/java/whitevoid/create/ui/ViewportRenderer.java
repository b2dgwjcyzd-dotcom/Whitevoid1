package whitevoid.create.ui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import whitevoid.create.editor.viewport.ViewportContext;
import whitevoid.create.model.Model;
import whitevoid.create.model.ModelNode;
import whitevoid.create.model.ModelRenderer;
import whitevoid.create.model.TransformMath;
import whitevoid.create.ui.ViewportProjector.Point;

public final class ViewportRenderer {
    private final ModelRenderer modelRenderer = new ModelRenderer();

    public void render(DrawContext context, int width, int height, ViewportContext viewport, Model model) {
        int left = 16, top = 16, right = width - 16, bottom = height - 16;
        int centerX = (left + right) / 2, centerY = (top + bottom) / 2;
        context.fill(left, top, right, bottom, 0xFF111216);

        ViewportProjector projector = new ViewportProjector(viewport.viewport().camera());
        if (viewport.viewport().gridVisible()) {
            drawGrid(context, projector, centerX, centerY, left, top, right, bottom);
        }
        drawAxes(context, projector, centerX, centerY, left, top, right, bottom);

        modelRenderer.render(model, (node, corners) ->
                drawModelNode(context, projector, node, corners, viewport, centerX, centerY, left, top, right, bottom));

        var textRenderer = MinecraftClient.getInstance().textRenderer;
        context.drawTextWithShadow(textRenderer, "CREATE • Model Viewport", left + 10, top + 10, 0xFFE8E8E8);
        context.drawTextWithShadow(textRenderer,
                viewport.viewport().camera().mode().name() + " | Zoom " +
                        String.format("%.2f", viewport.viewport().camera().distance()),
                left + 10, top + 25, 0xFFAAAAAA);
    }

    private void drawModelNode(DrawContext context, ViewportProjector projector, ModelNode node,
                               TransformMath.Point[] corners, ViewportContext viewport,
                               int cx, int cy, int left, int top, int right, int bottom) {
        Point[] points = new Point[8];
        for (int i = 0; i < corners.length; i++) {
            TransformMath.Point p = corners[i];
            points[i] = projector.project(p.x(), p.y(), p.z(), cx, cy, 300.0);
        }

        int color = viewport.selection().selection().contains(node.id()) ? 0xFFFFFFFF : 0xFFBFC3CC;
        for (int[] edge : ModelRenderer.edges()) {
            Point a = points[edge[0]], b = points[edge[1]];
            if (a != null && b != null) drawLine(context, a, b, left, top, right, bottom, color);
        }
    }

    private void drawAxes(DrawContext context, ViewportProjector projector, int cx, int cy,
                          int left, int top, int right, int bottom) {
        Point origin = projector.project(0,0,0,cx,cy,300);
        Point x = projector.project(3,0,0,cx,cy,300);
        Point y = projector.project(0,3,0,cx,cy,300);
        Point z = projector.project(0,0,3,cx,cy,300);
        if (origin == null) return;
        if (x != null) drawLine(context,origin,x,left,top,right,bottom,0xFFB96B6B);
        if (y != null) drawLine(context,origin,y,left,top,right,bottom,0xFF78B978);
        if (z != null) drawLine(context,origin,z,left,top,right,bottom,0xFF6B88C8);
    }

    private void drawGrid(DrawContext context, ViewportProjector projector, int cx, int cy,
                          int left, int top, int right, int bottom) {
        for (int i=-8;i<=8;i++) {
            Point a=projector.project(i,0,-8,cx,cy,300), b=projector.project(i,0,8,cx,cy,300);
            if(a!=null&&b!=null) drawLine(context,a,b,left,top,right,bottom,0xFF24262C);
            a=projector.project(-8,0,i,cx,cy,300); b=projector.project(8,0,i,cx,cy,300);
            if(a!=null&&b!=null) drawLine(context,a,b,left,top,right,bottom,0xFF24262C);
        }
    }

    private void drawLine(DrawContext context, Point a, Point b, int left, int top,
                          int right, int bottom, int color) {
        int x0=(int)Math.round(a.x()), y0=(int)Math.round(a.y());
        int x1=(int)Math.round(b.x()), y1=(int)Math.round(b.y());
        int dx=Math.abs(x1-x0), dy=Math.abs(y1-y0);
        int sx=x0<x1?1:-1, sy=y0<y1?1:-1, err=dx-dy;
        while(true) {
            if(x0>=left&&x0<right&&y0>=top&&y0<bottom) context.fill(x0,y0,x0+1,y0+1,color);
            if(x0==x1&&y0==y1) break;
            int e2=2*err;
            if(e2>-dy){err-=dy;x0+=sx;}
            if(e2<dx){err+=dx;y0+=sy;}
        }
    }
}
