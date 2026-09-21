package whitevoid.create.ui;

import whitevoid.create.editor.geometry.MeshSelectionMode;
import whitevoid.create.editor.transform.TransformMode;
import whitevoid.create.editor.viewport.ViewportContext;
import whitevoid.create.model.MeshGeometry;
import whitevoid.create.model.ModelNode;
import whitevoid.create.model.TransformMath;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

final class CreateViewportSelectionController {
    void selectComponentsInBox(ViewportContext viewport, ModelNode node,
                               double startX, double startY, double currentX, double currentY,
                               int width, int height, boolean altDown, boolean shiftDown) {
        if (node == null || viewport.transform().mode() != TransformMode.GEOMETRY) return;
        MeshGeometry mesh = node.ensureMeshGeometry();
        if (mesh == null) return;

        int left = (int) Math.round(Math.min(startX, currentX));
        int right = (int) Math.round(Math.max(startX, currentX));
        int top = (int) Math.round(Math.min(startY, currentY));
        int bottom = (int) Math.round(Math.max(startY, currentY));
        if (right - left < 3 && bottom - top < 3) return;

        ViewportProjector projector = new ViewportProjector(viewport.viewport().camera());
        int cx = width / 2;
        int cy = height / 2;
        var selection = viewport.meshComponentSelection();

        if (selection.mode() == MeshSelectionMode.VERTEX) {
            Set<Integer> hits = new LinkedHashSet<>();
            for (int i = 0; i < mesh.vertices().size(); i++) {
                var v = mesh.vertices().get(i);
                var w = TransformMath.applyHierarchy(new TransformMath.Point(v.x(), v.y(), v.z()), node);
                var p = projector.project(w.x(), w.y(), w.z(), cx, cy, 300);
                if (p != null && p.x() >= left && p.x() <= right && p.y() >= top && p.y() <= bottom) {
                    hits.add(i);
                }
            }
            if (altDown) {
                for (int i : hits) selection.removeVertex(node, i);
            } else if (shiftDown) {
                for (int i : hits) selection.addVertex(node, i);
            } else {
                selection.clear();
                for (int i : hits) selection.addVertex(node, i);
            }
        } else if (selection.mode() == MeshSelectionMode.EDGE) {
            List<int[]> hits = new java.util.ArrayList<>();
            for (int[] edge : whitevoid.create.model.ModelRenderer.meshEdges(mesh)) {
                var a = mesh.vertices().get(edge[0]);
                var b = mesh.vertices().get(edge[1]);
                var wa = TransformMath.applyHierarchy(new TransformMath.Point(a.x(), a.y(), a.z()), node);
                var wb = TransformMath.applyHierarchy(new TransformMath.Point(b.x(), b.y(), b.z()), node);
                var pa = projector.project(wa.x(), wa.y(), wa.z(), cx, cy, 300);
                var pb = projector.project(wb.x(), wb.y(), wb.z(), cx, cy, 300);
                if (pa != null && pb != null && pointInsideBox(pa.x(), pa.y(), left, top, right, bottom)) {
                    hits.add(new int[]{edge[0], edge[1]});
                }
            }
            if (altDown) {
                for (int[] edge : hits) selection.removeEdge(node, edge[0], edge[1]);
            } else {
                if (!shiftDown) selection.clear();
                for (int[] edge : hits) selection.addEdge(node, edge[0], edge[1]);
            }
        } else {
            Set<Integer> hits = new LinkedHashSet<>();
            for (int i = 0; i < mesh.faces().size(); i++) {
                int[] ids = mesh.faces().get(i).vertices();
                double sx = 0, sy = 0;
                int count = 0;
                for (int id : ids) {
                    var v = mesh.vertices().get(id);
                    var w = TransformMath.applyHierarchy(new TransformMath.Point(v.x(), v.y(), v.z()), node);
                    var p = projector.project(w.x(), w.y(), w.z(), cx, cy, 300);
                    if (p != null) {
                        sx += p.x();
                        sy += p.y();
                        count++;
                    }
                }
                if (count > 0 && pointInsideBox(sx / count, sy / count, left, top, right, bottom)) {
                    hits.add(i);
                }
            }
            if (altDown) {
                for (int i : hits) selection.removeFace(node, i);
            } else {
                if (!shiftDown) selection.clear();
                for (int i : hits) selection.addFace(node, i);
            }
        }
    }

    int hitTestMeshVertex(ModelNode node, MeshGeometry mesh, ViewportContext viewport,
                          double mouseX, double mouseY, int width, int height) {
        ViewportProjector projector = new ViewportProjector(viewport.viewport().camera());
        int cx = width / 2;
        int cy = height / 2;
        int best = -1;
        double bestDistance = 10.0;
        for (int i = 0; i < mesh.vertices().size(); i++) {
            var v = mesh.vertices().get(i);
            var world = TransformMath.applyHierarchy(new TransformMath.Point(v.x(), v.y(), v.z()), node);
            var p = projector.project(world.x(), world.y(), world.z(), cx, cy, 300);
            if (p == null) continue;
            double distance = Math.hypot(p.x() - mouseX, p.y() - mouseY);
            if (distance < bestDistance) {
                bestDistance = distance;
                best = i;
            }
        }
        return best;
    }

    private static boolean pointInsideBox(double x, double y, int left, int top, int right, int bottom) {
        return x >= left && x <= right && y >= top && y <= bottom;
    }
}
