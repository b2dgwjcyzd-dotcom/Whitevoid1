package whitevoid.create.ui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import whitevoid.create.editor.viewport.ViewportContext;
import whitevoid.create.model.Model;
import whitevoid.create.model.ModelNode;
import whitevoid.create.model.ModelRenderer;
import whitevoid.create.model.TransformMath;
import whitevoid.create.editor.transform.TransformMode;
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

        ModelNode selected = viewport.selection().first(model);
        if (selected != null && viewport.transform().mode() != whitevoid.create.editor.transform.TransformMode.SELECT) {
            drawGizmo(context, projector, selected, viewport.transform().mode(), centerX, centerY, left, top, right, bottom,
                    ViewportGizmo.Axis.NONE);
        }

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

    private void drawGizmo(DrawContext context, ViewportProjector projector, ModelNode node,
                           TransformMode mode,
                           int cx, int cy, int left, int top, int right, int bottom,
                           ViewportGizmo.Axis hoveredAxis) {
        TransformMath.Point o3 = TransformMath.applyHierarchy(new TransformMath.Point(0,0,0), node);
        Point o = projector.project(o3.x(),o3.y(),o3.z(),cx,cy,300);
        if(o==null) return;
        double[][] dirs={{2.2,0,0},{0,2.2,0},{0,0,2.2}};
        int[] colors={0xFFE06B6B,0xFF70C878,0xFF6B8EDC};
        if(hoveredAxis!=ViewportGizmo.Axis.NONE) {
            int hi=hoveredAxis==ViewportGizmo.Axis.X?0:hoveredAxis==ViewportGizmo.Axis.Y?1:2;
            colors[hi]=0xFFFFFFFF;
        }
        for(int i=0;i<3;i++){
            TransformMath.Point p3=TransformMath.applyHierarchy(new TransformMath.Point(dirs[i][0],dirs[i][1],dirs[i][2]),node);
            Point p=projector.project(p3.x(),p3.y(),p3.z(),cx,cy,300);
            if(p==null) continue;
            drawLine(context,o,p,left,top,right,bottom,colors[i]);
            if(mode==whitevoid.create.editor.transform.TransformMode.MOVE) {
                drawArrowHead(context,p,o,colors[i],left,top,right,bottom);
            } else if(mode==whitevoid.create.editor.transform.TransformMode.SCALE) {
                drawHandle(context,p,colors[i],left,top,right,bottom);
            }
        }
        if(mode==whitevoid.create.editor.transform.TransformMode.ROTATE) {
            drawRotationRings(context,projector,node,cx,cy,left,top,right,bottom);
        }
        context.fill((int)o.x()-4,(int)o.y()-4,(int)o.x()+5,(int)o.y()+5,0xFFFFFFFF);
    }



    private void drawArrowHead(DrawContext context, Point tip, Point origin, int color,
                               int left, int top, int right, int bottom) {
        double dx=tip.x()-origin.x(), dy=tip.y()-origin.y();
        double len=Math.hypot(dx,dy);
        if(len<1) return;
        dx/=len; dy/=len;
        double px=-dy, py=dx;
        Point a=new Point(tip.x()-dx*10+px*4,tip.y()-dy*10+py*4,tip.depth());
        Point b=new Point(tip.x()-dx*10-px*4,tip.y()-dy*10-py*4,tip.depth());
        drawLine(context,tip,a,left,top,right,bottom,color);
        drawLine(context,tip,b,left,top,right,bottom,color);
    }

    private void drawHandle(DrawContext context, Point p, int color,
                            int left, int top, int right, int bottom) {
        int x=(int)Math.round(p.x()), y=(int)Math.round(p.y());
        context.fill(x-4,y-4,x+5,y+5,0xFF111216);
        context.fill(x-3,y-3,x+4,y+4,color);
    }

    private void drawRotationRings(DrawContext context, ViewportProjector projector, ModelNode node,
                                   int cx, int cy, int left, int top, int right, int bottom) {
        TransformMath.Point origin3=TransformMath.applyHierarchy(new TransformMath.Point(0,0,0),node);
        Point origin=projector.project(origin3.x(),origin3.y(),origin3.z(),cx,cy,300);
        if(origin==null) return;

        double[][] basis={{2.0,0,0},{0,2.0,0},{0,0,2.0}};
        int[] colors={0xFFE06B6B,0xFF70C878,0xFF6B8EDC};
        for(int axis=0;axis<3;axis++){
            TransformMath.Point a3=TransformMath.applyHierarchy(
                    new TransformMath.Point(basis[axis][0],basis[axis][1],basis[axis][2]),node);
            Point a=projector.project(a3.x(),a3.y(),a3.z(),cx,cy,300);
            if(a==null) continue;
            double radius=Math.hypot(a.x()-origin.x(),a.y()-origin.y());
            if(radius<8) continue;
            double prevX=origin.x()+radius, prevY=origin.y();
            for(int i=1;i<=48;i++){
                double angle=(Math.PI*2*i)/48.0;
                double x=origin.x()+Math.cos(angle)*radius;
                double y=origin.y()+Math.sin(angle)*radius;
                drawLine(context,new Point(prevX,prevY,0),new Point(x,y,0),
                        left,top,right,bottom,colors[axis]);
                prevX=x; prevY=y;
            }
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
