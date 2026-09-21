package whitevoid.create.ui;

import net.minecraft.client.gui.DrawContext;
import whitevoid.create.editor.geometry.GeometryFace;
import whitevoid.create.editor.geometry.MeshSelectionMode;
import whitevoid.create.editor.viewport.ViewportContext;
import whitevoid.create.model.MeshGeometry;
import whitevoid.create.model.ModelNode;
import whitevoid.create.model.ModelRenderer;
import whitevoid.create.model.TransformMath;
import whitevoid.create.ui.ViewportProjector.Point;

public final class CreateViewportSelectionOverlayRenderer {
    public void render(DrawContext context, ViewportProjector projector, ModelNode selected,
                       ViewportContext viewport, GeometryFace hoveredFace, GeometryFace selectedFace,
                       int hoveredMeshFace, int hoveredMeshVertex, int hoveredMeshEdgeA, int hoveredMeshEdgeB,
                       boolean xrayMode, int cx, int cy, int left, int top, int right, int bottom) {
        if (selected == null) return;

        if (viewport.meshComponentSelection().matches(selected)
                && viewport.meshComponentSelection().mode() == MeshSelectionMode.FACE) {
            for (int faceIndex : viewport.meshComponentSelection().faceIndices()) {
                drawMeshFaceHighlight(context, projector, selected, faceIndex,
                        cx, cy, left, top, right, bottom, true);
            }
        }

        if (hoveredMeshFace >= 0
                && !(viewport.meshComponentSelection().matches(selected)
                && viewport.meshComponentSelection().mode() == MeshSelectionMode.FACE
                && viewport.meshComponentSelection().containsFace(hoveredMeshFace))) {
            drawMeshFaceHighlight(context, projector, selected, hoveredMeshFace,
                    cx, cy, left, top, right, bottom, false);
        }

        if (hoveredMeshFace < 0) {
            GeometryFace faceToDraw = viewport.geometryFaceSelection().matches(selected)
                    ? selectedFace : hoveredFace;
            drawGeometryFaceHighlight(context, projector, selected, faceToDraw,
                    cx, cy, left, top, right, bottom);
        }

        if (xrayMode) {
            drawMeshComponentXRay(context, projector, selected, viewport,
                    cx, cy, left, top, right, bottom);
        }
        drawMeshComponentSelection(context, projector, selected, viewport,
                cx, cy, left, top, right, bottom);
        drawMeshComponentHover(context, projector, selected, viewport,
                cx, cy, left, top, right, bottom,
                hoveredMeshVertex, hoveredMeshEdgeA, hoveredMeshEdgeB);
    }

    private void drawMeshComponentXRay(DrawContext context, ViewportProjector projector,
                                       ModelNode node, ViewportContext viewport,
                                       int cx, int cy, int left, int top, int right, int bottom) {
        var selection = viewport.meshComponentSelection();
        if (!selection.matches(node)) return;
        var mesh = node.ensureMeshGeometry();
        if (mesh == null) return;

        int modeColor = 0x887F8799;
        if (selection.mode() == MeshSelectionMode.VERTEX) {
            for (int i = 0; i < mesh.vertices().size(); i++) {
                if (selection.containsVertex(i)) continue;
                var v = mesh.vertices().get(i);
                var w = TransformMath.applyHierarchy(new TransformMath.Point(v.x(), v.y(), v.z()), node);
                Point p = projector.project(w.x(), w.y(), w.z(), cx, cy, 300);
                if (p == null) continue;
                int x = (int) Math.round(p.x()), y = (int) Math.round(p.y());
                context.fill(x - 2, y - 2, x + 3, y + 3, modeColor);
            }
        } else if (selection.mode() == MeshSelectionMode.EDGE) {
            for (int[] edge : ModelRenderer.meshEdges(mesh)) {
                if (selection.containsEdge(edge[0], edge[1])) continue;
                var a = mesh.vertices().get(edge[0]);
                var b = mesh.vertices().get(edge[1]);
                var wa = TransformMath.applyHierarchy(new TransformMath.Point(a.x(), a.y(), a.z()), node);
                var wb = TransformMath.applyHierarchy(new TransformMath.Point(b.x(), b.y(), b.z()), node);
                Point pa = projector.project(wa.x(), wa.y(), wa.z(), cx, cy, 300);
                Point pb = projector.project(wb.x(), wb.y(), wb.z(), cx, cy, 300);
                if (pa != null && pb != null) drawLine(context, pa, pb, left, top, right, bottom, modeColor);
            }
        } else {
            for (int faceIndex = 0; faceIndex < mesh.faces().size(); faceIndex++) {
                if (selection.containsFace(faceIndex)) continue;
                int[] ids = mesh.faces().get(faceIndex).vertices();
                for (int i = 0; i < ids.length; i++) {
                    var a = mesh.vertices().get(ids[i]);
                    var b = mesh.vertices().get(ids[(i + 1) % ids.length]);
                    var wa = TransformMath.applyHierarchy(new TransformMath.Point(a.x(), a.y(), a.z()), node);
                    var wb = TransformMath.applyHierarchy(new TransformMath.Point(b.x(), b.y(), b.z()), node);
                    Point pa = projector.project(wa.x(), wa.y(), wa.z(), cx, cy, 300);
                    Point pb = projector.project(wb.x(), wb.y(), wb.z(), cx, cy, 300);
                    if (pa != null && pb != null) drawLine(context, pa, pb, left, top, right, bottom, modeColor);
                }
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

    private void drawMeshComponentHover(DrawContext context, ViewportProjector projector,
                                         ModelNode node, ViewportContext viewport,
                                         int cx, int cy, int left, int top, int right, int bottom,
                                         int hoveredVertex, int hoveredEdgeA, int hoveredEdgeB) {
        var selection = viewport.meshComponentSelection();
        if (!selection.matches(node)) return;
        var mesh = node.ensureMeshGeometry();
        if (mesh == null) return;

        if (selection.mode() == MeshSelectionMode.VERTEX && hoveredVertex >= 0
                && hoveredVertex < mesh.vertices().size()
                && !selection.containsVertex(hoveredVertex)) {
            var v = mesh.vertices().get(hoveredVertex);
            var w = TransformMath.applyHierarchy(new TransformMath.Point(v.x(), v.y(), v.z()), node);
            Point p = projector.project(w.x(), w.y(), w.z(), cx, cy, 300);
            if (p != null) {
                int x = (int)Math.round(p.x()), y = (int)Math.round(p.y());
                context.fill(x - 4, y - 4, x + 5, y + 5, 0xFFE7E9EF);
                context.fill(x - 2, y - 2, x + 3, y + 3, 0xFFFFFFFF);
            }
        } else if (selection.mode() == MeshSelectionMode.EDGE && hoveredEdgeA >= 0 && hoveredEdgeB >= 0
                && !selection.containsEdge(hoveredEdgeA, hoveredEdgeB)
                && hoveredEdgeA < mesh.vertices().size() && hoveredEdgeB < mesh.vertices().size()) {
            var a = mesh.vertices().get(hoveredEdgeA);
            var b = mesh.vertices().get(hoveredEdgeB);
            var wa = TransformMath.applyHierarchy(new TransformMath.Point(a.x(), a.y(), a.z()), node);
            var wb = TransformMath.applyHierarchy(new TransformMath.Point(b.x(), b.y(), b.z()), node);
            Point pa = projector.project(wa.x(), wa.y(), wa.z(), cx, cy, 300);
            Point pb = projector.project(wb.x(), wb.y(), wb.z(), cx, cy, 300);
            if (pa != null && pb != null) {
                drawLine(context, pa, pb, left, top, right, bottom, 0xFFFFFFFF);
                drawLine(context, new Point(pa.x()+1,pa.y(),pa.depth()),
                        new Point(pb.x()+1,pb.y(),pb.depth()), left,top,right,bottom,0xFFE7E9EF);
            }
        }

        if (selection.mode() == MeshSelectionMode.VERTEX) {
            for (int index : selection.vertexIndices()) {
                if (index < 0 || index >= mesh.vertices().size()) continue;
                var v = mesh.vertices().get(index);
                var w = TransformMath.applyHierarchy(new TransformMath.Point(v.x(), v.y(), v.z()), node);
                Point p = projector.project(w.x(), w.y(), w.z(), cx, cy, 300);
                if (p == null) continue;
                int x = (int)Math.round(p.x()), y = (int)Math.round(p.y());
                context.fill(x - 2, y - 2, x + 3, y + 3,
                        index == selection.activeVertex() ? 0xFFFFFFFF : 0xFFD6D9E2);
            }
        } else if (selection.mode() == MeshSelectionMode.EDGE) {
            for (int[] edge : selection.edgeIndices()) {
                if (edge[0] < 0 || edge[1] < 0 || edge[0] >= mesh.vertices().size() || edge[1] >= mesh.vertices().size()) continue;
                var a = mesh.vertices().get(edge[0]);
                var b = mesh.vertices().get(edge[1]);
                var wa = TransformMath.applyHierarchy(new TransformMath.Point(a.x(), a.y(), a.z()), node);
                var wb = TransformMath.applyHierarchy(new TransformMath.Point(b.x(), b.y(), b.z()), node);
                Point pa = projector.project(wa.x(), wa.y(), wa.z(), cx, cy, 300);
                Point pb = projector.project(wb.x(), wb.y(), wb.z(), cx, cy, 300);
                if (pa == null || pb == null) continue;
                boolean active = edge[0] == selection.activeEdgeA() && edge[1] == selection.activeEdgeB()
                        || edge[0] == selection.activeEdgeB() && edge[1] == selection.activeEdgeA();
                drawLine(context, pa, pb, left, top, right, bottom, active ? 0xFFFFFFFF : 0xFFD6D9E2);
            }
        }
    }

    private void drawMeshFaceHighlight(DrawContext context, ViewportProjector projector,
                                       ModelNode node, int faceIndex, int cx, int cy,
                                       int left, int top, int right, int bottom, boolean selected) {
        if (faceIndex < 0) return;
        var mesh = node.ensureMeshGeometry();
        if (mesh == null || faceIndex >= mesh.faces().size()) return;
        int[] indices = mesh.faces().get(faceIndex).vertices();
        Point[] points = new Point[indices.length];
        for (int i = 0; i < indices.length; i++) {
            var v = mesh.vertices().get(indices[i]);
            TransformMath.Point world = TransformMath.applyHierarchy(new TransformMath.Point(v.x(), v.y(), v.z()), node);
            points[i] = projector.project(world.x(), world.y(), world.z(), cx, cy, 300);
            if (points[i] == null) return;
        }
        for (int i = 0; i < points.length; i++) {
            Point a = points[i], b = points[(i + 1) % points.length];
            int primary = selected ? 0xFFFFFFFF : 0xFF8D96A8;
            int secondary = selected ? 0xFFE7E9EF : 0xFF596174;
            drawLine(context, a, b, left, top, right, bottom, primary);
            drawLine(context, new Point(a.x() + 1, a.y(), a.depth()),
                    new Point(b.x() + 1, b.y(), b.depth()), left, top, right, bottom, secondary);
        }
    }

    private void drawGeometryFaceHighlight(DrawContext context, ViewportProjector projector,
                                            ModelNode node, GeometryFace face,
                                            int cx, int cy, int left, int top, int right, int bottom) {
        if (face == GeometryFace.NONE || node.geometry() == null) return;
        double hx = node.geometry().width() * 0.5, hy = node.geometry().height() * 0.5, hz = node.geometry().depth() * 0.5;
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
        for (int i = 0; i < 4; i++) {
            TransformMath.Point world = TransformMath.applyHierarchy(local[i], node);
            points[i] = projector.project(world.x(), world.y(), world.z(), cx, cy, 300);
        }
        for (int i = 0; i < 4; i++) {
            Point a = points[i], b = points[(i + 1) % 4];
            if (a != null && b != null) {
                drawLine(context, a, b, left, top, right, bottom, 0xFFFFFFFF);
                drawLine(context, new Point(a.x()+1,a.y(),a.depth()),
                        new Point(b.x()+1,b.y(),b.depth()), left,top,right,bottom,0xFFE7E9EF);
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
