package whitevoid.create.ui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import whitevoid.create.editor.viewport.ViewportContext;
import whitevoid.create.model.Model;
import whitevoid.create.model.ModelNode;
import whitevoid.create.model.TransformMath;
import whitevoid.create.editor.transform.TransformMode;
import whitevoid.create.editor.geometry.GeometryFace;
import whitevoid.create.ui.ComponentTransformGizmo.Operation;
import whitevoid.create.ui.ViewportProjector.Point;

public final class ViewportRenderer {
    private final CreateViewportModelRenderer modelRenderer = new CreateViewportModelRenderer();
    private final CreateViewportGridRenderer gridRenderer = new CreateViewportGridRenderer();
    private final CreateViewportSelectionOverlayRenderer selectionOverlayRenderer = new CreateViewportSelectionOverlayRenderer();

    public void render(DrawContext context, int width, int height, ViewportContext viewport, Model model,
                       ViewportGizmo.Axis hoveredAxis, GeometryFace hoveredFace, GeometryFace selectedFace,
                       int hoveredMeshFace, int hoveredMeshVertex, int hoveredMeshEdgeA, int hoveredMeshEdgeB,
                       ComponentTransformGizmo.Axis hoveredComponentAxis,
                       Operation componentOperation, ComponentTransformGizmo.PivotMode componentPivotMode,
                       boolean xrayMode) {
        int left = 16, top = 16, right = width - 16, bottom = height - 16;
        int centerX = (left + right) / 2, centerY = (top + bottom) / 2;
        context.fill(left, top, right, bottom, 0xFF111216);

        ViewportProjector projector = new ViewportProjector(viewport.viewport().camera());
        if (viewport.viewport().gridVisible()) {
            gridRenderer.renderGrid(context, projector, centerX, centerY, left, top, right, bottom);
        }
        gridRenderer.renderAxes(context, projector, centerX, centerY, left, top, right, bottom);

        modelRenderer.render(context, projector, model, viewport,
                centerX, centerY, left, top, right, bottom);

        ModelNode selected = viewport.selection().first(model);
        if (selected != null && viewport.transform().mode() != whitevoid.create.editor.transform.TransformMode.SELECT) {
            drawGizmo(context, projector, selected, viewport.transform().mode(), centerX, centerY, left, top, right, bottom,
                    hoveredAxis);
        }
        if (selected != null && viewport.transform().mode() == TransformMode.GEOMETRY) {
            selectionOverlayRenderer.render(context, projector, selected, viewport,
                    hoveredFace, selectedFace,
                    hoveredMeshFace, hoveredMeshVertex, hoveredMeshEdgeA, hoveredMeshEdgeB,
                    xrayMode, centerX, centerY, left, top, right, bottom);
            drawComponentGizmo(context, projector, selected, viewport,
                    centerX, centerY, left, top, right, bottom,
                    hoveredComponentAxis, componentOperation, componentPivotMode);
            if (selected.meshGeometry() == null) {
                drawGeometryHandles(context, projector, selected,
                        centerX, centerY, left, top, right, bottom, hoveredAxis);
            }
        }
        var textRenderer = MinecraftClient.getInstance().textRenderer;
        context.drawTextWithShadow(textRenderer, "CREATE • Model Viewport", left + 10, top + 10, 0xFFE8E8E8);
        context.drawTextWithShadow(textRenderer,
                viewport.viewport().camera().mode().name() + " | Zoom " +
                        String.format("%.2f", viewport.viewport().camera().distance()),
                left + 10, top + 25, 0xFFAAAAAA);
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
            TransformMath.Point p3=TransformMath.applyHierarchy(
                    new TransformMath.Point(dirs[i][0],dirs[i][1],dirs[i][2]),node);
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
        TransformMath.Point localPivot = new TransformMath.Point(0, 0, 0);
        int[][] planes = {{1, 2}, {0, 2}, {0, 1}};
        int[] colors = {0xFFE06B6B, 0xFF70C878, 0xFF6B8EDC};

        for (int axis = 0; axis < 3; axis++) {
            Point previous = null;
            for (int i = 0; i <= 64; i++) {
                double angle = Math.PI * 2.0 * i / 64.0;
                double[] offset = {0.0, 0.0, 0.0};
                offset[planes[axis][0]] = Math.cos(angle) * ComponentTransformGizmo.gizmoRadius(projector);
                offset[planes[axis][1]] = Math.sin(angle) * ComponentTransformGizmo.gizmoRadius(projector);

                TransformMath.Point local = new TransformMath.Point(
                        localPivot.x() + offset[0],
                        localPivot.y() + offset[1],
                        localPivot.z() + offset[2]);
                TransformMath.Point world = TransformMath.applyHierarchy(local, node);
                Point current = projector.project(world.x(), world.y(), world.z(), cx, cy, 300);
                if (current == null) {
                    previous = null;
                    continue;
                }
                if (previous != null) {
                    drawLine(context, previous, current, left, top, right, bottom, colors[axis]);
                }
                previous = current;
            }
        }
    }


    private void drawComponentGizmo(DrawContext context, ViewportProjector projector, ModelNode node,
                                        ViewportContext viewport, int cx, int cy,
                                        int left, int top, int right, int bottom,
                                        ComponentTransformGizmo.Axis hoveredAxis, Operation operation,
                                        ComponentTransformGizmo.PivotMode pivotMode) {
        var selection=viewport.meshComponentSelection();
        if(!selection.matches(node) || selection.size()==0) return;
        ComponentTransformGizmo gizmo=new ComponentTransformGizmo();
        TransformMath.Point localPivot=gizmo.localPivot(node,selection.mode(),selection.vertexIndices(),
                selection.edgeIndices(),selection.faceIndices(),pivotMode,selection);
        TransformMath.Point p3=TransformMath.applyHierarchy(localPivot,node);
        Point o=projector.project(p3.x(),p3.y(),p3.z(),cx,cy,300);
        if(o==null)return;
        double gizmoRadius = ComponentTransformGizmo.gizmoRadius(projector);
        double[][] dirs={{gizmoRadius,0,0},{0,gizmoRadius,0},{0,0,gizmoRadius}};
        int[] colors={0xFFE06B6B,0xFF70C878,0xFF6B8EDC};
        if(operation==Operation.ROTATE){
            drawComponentRotationRings(context,projector,localPivot,o,node,cx,cy,left,top,right,bottom,colors,hoveredAxis);
        } else {
            for(int i=0;i<3;i++){
                TransformMath.Point localEnd = new TransformMath.Point(
                        localPivot.x()+dirs[i][0],
                        localPivot.y()+dirs[i][1],
                        localPivot.z()+dirs[i][2]);
                TransformMath.Point worldEnd = TransformMath.applyHierarchy(localEnd, node);
                Point p=projector.project(worldEnd.x(),worldEnd.y(),worldEnd.z(),cx,cy,300);
                if(p==null)continue;
                int color=(hoveredAxis==new ComponentTransformGizmo.Axis[]{ComponentTransformGizmo.Axis.X,ComponentTransformGizmo.Axis.Y,ComponentTransformGizmo.Axis.Z}[i])?0xFFFFFFFF:colors[i];
                drawLine(context,o,p,left,top,right,bottom,color);
                int x=(int)Math.round(p.x()),y=(int)Math.round(p.y());
                if(operation==Operation.SCALE) drawHandle(context,p,color,left,top,right,bottom);
                else drawArrowHead(context,p,o,color,left,top,right,bottom);
            }
        }
        int ox=(int)Math.round(o.x()),oy=(int)Math.round(o.y());
        context.fill(ox-4,oy-4,ox+5,oy+5,0xFFFFFFFF);
    }

    private void drawComponentRotationRings(DrawContext context, ViewportProjector projector,
                                             TransformMath.Point localPivot, Point projectedPivot,
                                             ModelNode node, int cx, int cy,
                                             int left, int top, int right, int bottom,
                                             int[] colors, ComponentTransformGizmo.Axis hoveredAxis) {
        int[][] planes = {{1, 2}, {0, 2}, {0, 1}};
        ComponentTransformGizmo.Axis[] axes = {
                ComponentTransformGizmo.Axis.X,
                ComponentTransformGizmo.Axis.Y,
                ComponentTransformGizmo.Axis.Z
        };

        for (int axis = 0; axis < 3; axis++) {
            Point previous = null;
            int color = hoveredAxis == axes[axis] ? 0xFFFFFFFF : colors[axis];

            for (int i = 0; i <= 64; i++) {
                double angle = Math.PI * 2.0 * i / 64.0;
                double[] offset = {0.0, 0.0, 0.0};
                offset[planes[axis][0]] = Math.cos(angle) * 2.0;
                offset[planes[axis][1]] = Math.sin(angle) * 2.0;

                TransformMath.Point local = new TransformMath.Point(
                        localPivot.x() + offset[0],
                        localPivot.y() + offset[1],
                        localPivot.z() + offset[2]);
                TransformMath.Point world = TransformMath.applyHierarchy(local, node);
                Point current = projector.project(world.x(), world.y(), world.z(), cx, cy, 300);
                if (current == null) {
                    previous = null;
                    continue;
                }
                if (previous != null) {
                    drawLine(context, previous, current, left, top, right, bottom, color);
                }
                previous = current;
            }
        }
    }

    private void drawGeometryHandles(DrawContext context, ViewportProjector projector, ModelNode node,
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

        for (int i=0;i<3;i++) {
            for (int sign : new int[]{1,-1}) {
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
                drawHandle(context, p, color, left, top, right, bottom);
            }
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
