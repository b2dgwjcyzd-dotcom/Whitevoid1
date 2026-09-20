package whitevoid.create.ui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import whitevoid.create.editor.viewport.ViewportContext;
import whitevoid.create.model.Model;
import whitevoid.create.model.ModelNode;
import whitevoid.create.model.MeshGeometry;
import whitevoid.create.model.ModelRenderer;
import whitevoid.create.model.TransformMath;
import whitevoid.create.editor.transform.TransformMode;
import whitevoid.create.editor.geometry.GeometryFace;
import whitevoid.create.editor.geometry.MeshSelectionMode;
import whitevoid.create.ui.ComponentTransformGizmo.Operation;
import whitevoid.create.ui.ViewportProjector.Point;

public final class ViewportRenderer {
    private final ModelRenderer modelRenderer = new ModelRenderer();

    public void render(DrawContext context, int width, int height, ViewportContext viewport, Model model,
                       ViewportGizmo.Axis hoveredAxis, GeometryFace hoveredFace, GeometryFace selectedFace,
                       int hoveredMeshFace, ComponentTransformGizmo.Axis hoveredComponentAxis,
                       Operation componentOperation, ComponentTransformGizmo.PivotMode componentPivotMode) {
        int left = 16, top = 16, right = width - 16, bottom = height - 16;
        int centerX = (left + right) / 2, centerY = (top + bottom) / 2;
        context.fill(left, top, right, bottom, 0xFF111216);

        ViewportProjector projector = new ViewportProjector(viewport.viewport().camera());
        if (viewport.viewport().gridVisible()) {
            drawGrid(context, projector, centerX, centerY, left, top, right, bottom);
        }
        drawAxes(context, projector, centerX, centerY, left, top, right, bottom);

        modelRenderer.renderMesh(model, (node, mesh) ->
                drawModelMesh(context, projector, node, mesh, viewport,
                        centerX, centerY, left, top, right, bottom));

        ModelNode selected = viewport.selection().first(model);
        if (selected != null && viewport.transform().mode() != whitevoid.create.editor.transform.TransformMode.SELECT) {
            drawGizmo(context, projector, selected, viewport.transform().mode(), centerX, centerY, left, top, right, bottom,
                    hoveredAxis);
        }
        if (selected != null && viewport.transform().mode() == TransformMode.GEOMETRY) {
            if (viewport.meshComponentSelection().matches(selected)
                    && viewport.meshComponentSelection().mode() == MeshSelectionMode.FACE) {
                for (int faceIndex : viewport.meshComponentSelection().faceIndices()) {
                    drawMeshFaceHighlight(context, projector, selected,
                            faceIndex, centerX, centerY, left, top, right, bottom, true);
                }
            }
            if (hoveredMeshFace >= 0
                    && !(viewport.meshComponentSelection().matches(selected)
                    && viewport.meshComponentSelection().mode() == MeshSelectionMode.FACE
                    && viewport.meshComponentSelection().containsFace(hoveredMeshFace))) {
                drawMeshFaceHighlight(context, projector, selected, hoveredMeshFace,
                        centerX, centerY, left, top, right, bottom, false);
            }
            if (hoveredMeshFace < 0) {
                GeometryFace faceToDraw = viewport.geometryFaceSelection().matches(selected)
                        ? selectedFace : hoveredFace;
                drawGeometryFaceHighlight(context, projector, selected, faceToDraw,
                        centerX, centerY, left, top, right, bottom);
            }
            drawMeshComponentSelection(context, projector, selected, viewport,
                    centerX, centerY, left, top, right, bottom);
            drawComponentGizmo(context, projector, selected, viewport,
                    centerX, centerY, left, top, right, bottom, hoveredComponentAxis, componentOperation, componentPivotMode);
            if (selected.meshGeometry() == null) {
                drawGeometryHandles(context, projector, selected, centerX, centerY, left, top, right, bottom, hoveredAxis);
            }
        }

        var textRenderer = MinecraftClient.getInstance().textRenderer;
        context.drawTextWithShadow(textRenderer, "CREATE • Model Viewport", left + 10, top + 10, 0xFFE8E8E8);
        context.drawTextWithShadow(textRenderer,
                viewport.viewport().camera().mode().name() + " | Zoom " +
                        String.format("%.2f", viewport.viewport().camera().distance()),
                left + 10, top + 25, 0xFFAAAAAA);
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
                if (components.mode() == whitevoid.create.editor.geometry.MeshSelectionMode.VERTEX) {
                    for (int index : components.vertexIndices()) {
                        if (index < 0 || index >= points.length || points[index] == null) continue;
                        Point point = points[index];
                        int x=(int)Math.round(point.x()), y=(int)Math.round(point.y());
                        int marker = index == components.activeVertex() ? 0xFFFFFFFF : 0xFFD6D9E2;
                        context.fill(x-3,y-3,x+4,y+4,marker);
                        if (index == components.activeVertex()) {
                            context.fill(x-5,y-1,x+6,y+1,0xFFFFFFFF);
                            context.fill(x-1,y-5,x+1,y+6,0xFFFFFFFF);
                        }
                    }
                } else if (components.mode() == whitevoid.create.editor.geometry.MeshSelectionMode.EDGE) {
                    for (int[] edge : components.edgeIndices()) {
                        if (edge[0] < 0 || edge[1] < 0 || edge[0] >= points.length || edge[1] >= points.length) continue;
                        Point a=points[edge[0]], b=points[edge[1]];
                        if(a==null||b==null) continue;
                        boolean active = edge[0] == components.activeEdgeA() && edge[1] == components.activeEdgeB()
                                || edge[0] == components.activeEdgeB() && edge[1] == components.activeEdgeA();
                        int edgeColor = active ? 0xFFFFFFFF : 0xFFD6D9E2;
                        drawLine(context,a,b,left,top,right,bottom,edgeColor);
                        drawLine(context,new Point(a.x()+1,a.y(),a.depth()),new Point(b.x()+1,b.y(),b.depth()),
                                left,top,right,bottom,active ? 0xFFFFFFFF : 0xFFE7E9EF);
                    }
                }
            } else if (selected) {
                for (Point point : points) {
                    if (point == null) continue;
                    int x=(int)Math.round(point.x()), y=(int)Math.round(point.y());
                    context.fill(x-2,y-2,x+3,y+3,0xFFFFFFFF);
                }
            }
        } else if (selected) {
            for (Point point : points) {
                if (point == null) continue;
                int x = (int) Math.round(point.x());
                int y = (int) Math.round(point.y());
                context.fill(x - 2, y - 2, x + 3, y + 3, 0xFFFFFFFF);
            }
        }
    }

    private void drawModelNode(DrawContext context, ViewportProjector projector, ModelNode node,
                               TransformMath.Point[] corners, ViewportContext viewport,
                               int cx, int cy, int left, int top, int right, int bottom) {
        Point[] points = new Point[8];
        for (int i = 0; i < corners.length; i++) {
            TransformMath.Point p = corners[i];
            points[i] = projector.project(p.x(), p.y(), p.z(), cx, cy, 300.0);
        }

        boolean selected = viewport.selection().selection().contains(node.id());
        int color = selected ? 0xFFFFFFFF : 0xFFBFC3CC;
        for (int[] edge : ModelRenderer.edges()) {
            Point a = points[edge[0]], b = points[edge[1]];
            if (a == null || b == null) continue;
            drawLine(context, a, b, left, top, right, bottom, color);
            if (selected) {
                drawLine(context, new Point(a.x()+1,a.y(),a.depth()),
                        new Point(b.x()+1,b.y(),b.depth()), left,top,right,bottom,0xFFE7E9EF);
                drawLine(context, new Point(a.x()-1,a.y(),a.depth()),
                        new Point(b.x()-1,b.y(),b.depth()), left,top,right,bottom,0xFFE7E9EF);
            }
        }

        if (selected) {
            for (Point point : points) {
                if (point == null) continue;
                int x=(int)Math.round(point.x()), y=(int)Math.round(point.y());
                context.fill(x-2,y-2,x+3,y+3,0xFFFFFFFF);
            }
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

    private void drawMeshComponentSelection(DrawContext context, ViewportProjector projector,
                                                ModelNode node, ViewportContext viewport,
                                                int cx, int cy, int left, int top, int right, int bottom) {
        var selection = viewport.meshComponentSelection();
        if (!selection.matches(node)) return;
        var mesh = node.ensureMeshGeometry();
        if (mesh == null) return;

        if (selection.mode() == MeshSelectionMode.VERTEX) {
            int index = selection.indexA();
            if (index >= 0 && index < mesh.vertices().size()) {
                var v = mesh.vertices().get(index);
                var w = TransformMath.applyHierarchy(new TransformMath.Point(v.x(), v.y(), v.z()), node);
                var p = projector.project(w.x(), w.y(), w.z(), cx, cy, 300);
                if (p != null) {
                    int x = (int)Math.round(p.x()), y = (int)Math.round(p.y());
                    context.fill(x - 4, y - 4, x + 5, y + 5, 0xFFFFFFFF);
                }
            }
        } else if (selection.mode() == MeshSelectionMode.EDGE) {
            int a = selection.indexA(), b = selection.indexB();
            if (a >= 0 && b >= 0 && a < mesh.vertices().size() && b < mesh.vertices().size()) {
                var va = mesh.vertices().get(a);
                var vb = mesh.vertices().get(b);
                var wa = TransformMath.applyHierarchy(new TransformMath.Point(va.x(), va.y(), va.z()), node);
                var wb = TransformMath.applyHierarchy(new TransformMath.Point(vb.x(), vb.y(), vb.z()), node);
                var pa = projector.project(wa.x(), wa.y(), wa.z(), cx, cy, 300);
                var pb = projector.project(wb.x(), wb.y(), wb.z(), cx, cy, 300);
                if (pa != null && pb != null) {
                    drawLine(context, pa, pb, left, top, right, bottom, 0xFFFFFFFF);
                    drawLine(context, new Point(pa.x()+1,pa.y(),pa.depth()),
                            new Point(pb.x()+1,pb.y(),pb.depth()), left,top,right,bottom,0xFFE7E9EF);
                }
            }
        }
    }

    private void drawMeshFaceHighlight(DrawContext context, ViewportProjector projector,
                                          ModelNode node, int faceIndex,
                                          int cx, int cy, int left, int top, int right, int bottom) {
        drawMeshFaceHighlight(context, projector, node, faceIndex, cx, cy, left, top, right, bottom, true);
    }

    private void drawMeshFaceHighlight(DrawContext context, ViewportProjector projector,
                                          ModelNode node, int faceIndex,
                                          int cx, int cy, int left, int top, int right, int bottom,
                                          boolean selected) {
        if (faceIndex < 0) return;
        var mesh = node.ensureMeshGeometry();
        if (mesh == null || faceIndex >= mesh.faces().size()) return;

        int[] indices = mesh.faces().get(faceIndex).vertices();
        Point[] points = new Point[indices.length];
        for (int i = 0; i < indices.length; i++) {
            var v = mesh.vertices().get(indices[i]);
            TransformMath.Point world = TransformMath.applyHierarchy(
                    new TransformMath.Point(v.x(), v.y(), v.z()), node);
            points[i] = projector.project(world.x(), world.y(), world.z(), cx, cy, 300);
            if (points[i] == null) return;
        }

        for (int i = 0; i < points.length; i++) {
            Point a = points[i], b = points[(i + 1) % points.length];
            int primary = selected ? 0xFFFFFFFF : 0xFF8D96A8;
            int secondary = selected ? 0xFFE7E9EF : 0xFF596174;
            drawLine(context, a, b, left, top, right, bottom, primary);
            drawLine(context, new Point(a.x() + 1, a.y(), a.depth()),
                    new Point(b.x() + 1, b.y(), b.depth()),
                    left, top, right, bottom, secondary);
        }
    }

    private void drawGeometryFaceHighlight(DrawContext context, ViewportProjector projector,
                                             ModelNode node, GeometryFace face,
                                             int cx, int cy, int left, int top, int right, int bottom) {
        if (face == GeometryFace.NONE || node.geometry() == null) return;

        double hx = node.geometry().width() * 0.5;
        double hy = node.geometry().height() * 0.5;
        double hz = node.geometry().depth() * 0.5;
        TransformMath.Point[] local = switch (face) {
            case POS_X -> new TransformMath.Point[]{
                    new TransformMath.Point(hx,-hy,-hz), new TransformMath.Point(hx,hy,-hz),
                    new TransformMath.Point(hx,hy,hz), new TransformMath.Point(hx,-hy,hz)};
            case NEG_X -> new TransformMath.Point[]{
                    new TransformMath.Point(-hx,-hy,hz), new TransformMath.Point(-hx,hy,hz),
                    new TransformMath.Point(-hx,hy,-hz), new TransformMath.Point(-hx,-hy,-hz)};
            case POS_Y -> new TransformMath.Point[]{
                    new TransformMath.Point(-hx,hy,-hz), new TransformMath.Point(-hx,hy,hz),
                    new TransformMath.Point(hx,hy,hz), new TransformMath.Point(hx,hy,-hz)};
            case NEG_Y -> new TransformMath.Point[]{
                    new TransformMath.Point(-hx,-hy,hz), new TransformMath.Point(-hx,-hy,-hz),
                    new TransformMath.Point(hx,-hy,-hz), new TransformMath.Point(hx,-hy,hz)};
            case POS_Z -> new TransformMath.Point[]{
                    new TransformMath.Point(-hx,-hy,hz), new TransformMath.Point(hx,-hy,hz),
                    new TransformMath.Point(hx,hy,hz), new TransformMath.Point(-hx,hy,hz)};
            case NEG_Z -> new TransformMath.Point[]{
                    new TransformMath.Point(hx,-hy,-hz), new TransformMath.Point(-hx,-hy,-hz),
                    new TransformMath.Point(-hx,hy,-hz), new TransformMath.Point(hx,hy,-hz)};
            case NONE -> new TransformMath.Point[0];
        };

        Point[] points = new Point[4];
        for (int i=0;i<4;i++) {
            TransformMath.Point world = TransformMath.applyHierarchy(local[i], node);
            points[i] = projector.project(world.x(), world.y(), world.z(), cx, cy, 300);
        }
        for (int i=0;i<4;i++) {
            Point a = points[i], b = points[(i+1)%4];
            if (a != null && b != null) {
                drawLine(context, a, b, left, top, right, bottom, 0xFFFFFFFF);
                drawLine(context, new Point(a.x()+1,a.y(),a.depth()),
                        new Point(b.x()+1,b.y(),b.depth()), left,top,right,bottom,0xFFE7E9EF);
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
