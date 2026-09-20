package whitevoid.create.ui;

import whitevoid.create.editor.transform.TransformMode;
import whitevoid.create.model.ModelNode;
import whitevoid.create.model.ModelRenderer;
import whitevoid.create.model.TransformMath;
import whitevoid.create.editor.geometry.GeometryFace;

public final class ViewportGizmo {
    public Axis hoveredAxis(ModelNode node, TransformMode mode, ViewportProjector projector, double mouseX, double mouseY, int cx, int cy) {
        return hit(node, mode, projector, mouseX, mouseY, cx, cy);
    }
    public enum Axis { NONE, X, Y, Z, NEG_X, NEG_Y, NEG_Z }

    public Axis hit(ModelNode node, TransformMode mode, ViewportProjector projector,
                    double mouseX, double mouseY, int cx, int cy) {
        if (node == null || mode == TransformMode.SELECT) return Axis.NONE;
        TransformMath.Point origin3 = TransformMath.applyHierarchy(new TransformMath.Point(0,0,0), node);
        ViewportProjector.Point o = projector.project(origin3.x(),origin3.y(),origin3.z(),cx,cy,300);
        if (o == null) return Axis.NONE;
        double best=14;
        Axis result=Axis.NONE;
        double[][] dirs={{2.2,0,0},{0,2.2,0},{0,0,2.2}};
        Axis[] axes={Axis.X,Axis.Y,Axis.Z};
        for(int i=0;i<3;i++){
            TransformMath.Point p3=TransformMath.applyHierarchy(new TransformMath.Point(dirs[i][0],dirs[i][1],dirs[i][2]),node);
            ViewportProjector.Point p=projector.project(p3.x(),p3.y(),p3.z(),cx,cy,300);
            if(p==null) continue;
            double d=distanceToSegment(mouseX,mouseY,o.x(),o.y(),p.x(),p.y());
            if(d<best){best=d;result=axes[i];}
        }
        return result;
    }

    public Axis geometryHit(ModelNode node, ViewportProjector projector,
                             double mouseX, double mouseY, int cx, int cy) {
        if (node == null || node.geometry() == null) return Axis.NONE;

        TransformMath.Point center3 = TransformMath.applyHierarchy(
                new TransformMath.Point(0, 0, 0), node);

        double[] half = {
                node.geometry().width() * 0.5,
                node.geometry().height() * 0.5,
                node.geometry().depth() * 0.5
        };
        double[][] dirs = {{1,0,0},{0,1,0},{0,0,1}};
        Axis[] positive = {Axis.X, Axis.Y, Axis.Z};
        Axis[] negative = {Axis.NEG_X, Axis.NEG_Y, Axis.NEG_Z};

        double best = 13.0;
        Axis result = Axis.NONE;

        for (int i = 0; i < 3; i++) {
            for (int sign : new int[]{1, -1}) {
                TransformMath.Point local = new TransformMath.Point(
                        dirs[i][0] * half[i] * sign,
                        dirs[i][1] * half[i] * sign,
                        dirs[i][2] * half[i] * sign);
                TransformMath.Point world = TransformMath.applyHierarchy(local, node);
                ViewportProjector.Point p = projector.project(
                        world.x(), world.y(), world.z(), cx, cy, 300);
                if (p == null) continue;

                ViewportProjector.Point center = projector.project(
                        center3.x(), center3.y(), center3.z(), cx, cy, 300);
                if (center == null) continue;

                double d = distanceToSegment(mouseX, mouseY,
                        center.x(), center.y(), p.x(), p.y());
                if (d < best) {
                    best = d;
                    result = sign > 0 ? positive[i] : negative[i];
                }
            }
        }
        return result;
    }

    public GeometryFace faceHit(ModelNode node, ViewportProjector projector,
                                   double mouseX, double mouseY, int cx, int cy) {
        if (node == null) return GeometryFace.NONE;
        var mesh = node.ensureMeshGeometry();
        if (mesh == null) return GeometryFace.NONE;

        GeometryFace bestFace = GeometryFace.NONE;
        double bestDepth = Double.POSITIVE_INFINITY;
        double bestDistance = 8.0;

        for (int faceIndex = 0; faceIndex < mesh.faces().size(); faceIndex++) {
            int[] indices = mesh.faces().get(faceIndex).vertices();
            if (indices.length < 3) continue;

            ViewportProjector.Point[] projected = new ViewportProjector.Point[indices.length];
            boolean valid = true;
            for (int i = 0; i < indices.length; i++) {
                var vertex = mesh.vertices().get(indices[i]);
                TransformMath.Point world = TransformMath.applyHierarchy(
                        new TransformMath.Point(vertex.x(), vertex.y(), vertex.z()), node);
                projected[i] = projector.project(
                        world.x(), world.y(), world.z(), cx, cy, 300);
                if (projected[i] == null) {
                    valid = false;
                    break;
                }
            }
            if (!valid) continue;

            double distance = pointToPolygon(mouseX, mouseY, projected);
            if (distance <= bestDistance) {
                double depth = 0.0;
                for (var point : projected) depth += point.depth();
                depth /= projected.length;

                if (depth < bestDepth) {
                    bestDepth = depth;
                    bestFace = switch (faceIndex) {
                        case 0 -> GeometryFace.POS_X;
                        case 1 -> GeometryFace.NEG_X;
                        case 2 -> GeometryFace.POS_Y;
                        case 3 -> GeometryFace.NEG_Y;
                        case 4 -> GeometryFace.POS_Z;
                        case 5 -> GeometryFace.NEG_Z;
                        default -> GeometryFace.NONE;
                    };
                }
            }
        }
        return bestFace;
    }

    public int meshVertexHit(ModelNode node, ViewportProjector projector,
                                double mouseX, double mouseY, int cx, int cy) {
        var hits = meshVertexHits(node, projector, mouseX, mouseY, cx, cy);
        return hits.isEmpty() ? -1 : hits.get(0);
    }

    /** Returns all vertex hits ordered front-to-back by camera depth. */
    public java.util.List<Integer> meshVertexHits(ModelNode node, ViewportProjector projector,
                                                   double mouseX, double mouseY, int cx, int cy) {
        if (node == null) return java.util.List.of();
        var mesh = node.ensureMeshGeometry();
        if (mesh == null) return java.util.List.of();
        record Hit(int index, double depth, double distance) {}
        var hits = new java.util.ArrayList<Hit>();
        for (int i = 0; i < mesh.vertices().size(); i++) {
            var v = mesh.vertices().get(i);
            var world = TransformMath.applyHierarchy(new TransformMath.Point(v.x(), v.y(), v.z()), node);
            var p = projector.project(world.x(), world.y(), world.z(), cx, cy, 300);
            if (p == null) continue;
            double distance = Math.hypot(mouseX - p.x(), mouseY - p.y());
            if (distance <= 7.0) hits.add(new Hit(i, p.depth(), distance));
        }
        hits.sort(java.util.Comparator.comparingDouble(Hit::depth).thenComparingDouble(Hit::distance));
        var result = new java.util.ArrayList<Integer>(hits.size());
        for (Hit hit : hits) result.add(hit.index());
        return result;
    }

    public int[] meshEdgeHit(ModelNode node, ViewportProjector projector,
                             double mouseX, double mouseY, int cx, int cy) {
        var hits = meshEdgeHits(node, projector, mouseX, mouseY, cx, cy);
        return hits.isEmpty() ? null : hits.get(0);
    }

    /** Returns all edge hits ordered front-to-back by average camera depth. */
    public java.util.List<int[]> meshEdgeHits(ModelNode node, ViewportProjector projector,
                                               double mouseX, double mouseY, int cx, int cy) {
        if (node == null) return java.util.List.of();
        var mesh = node.ensureMeshGeometry();
        if (mesh == null) return java.util.List.of();
        record Hit(int[] edge, double depth, double distance) {}
        var hits = new java.util.ArrayList<Hit>();
        for (int[] edge : ModelRenderer.meshEdges(mesh)) {
            var a = mesh.vertices().get(edge[0]);
            var b = mesh.vertices().get(edge[1]);
            var wa = TransformMath.applyHierarchy(new TransformMath.Point(a.x(), a.y(), a.z()), node);
            var wb = TransformMath.applyHierarchy(new TransformMath.Point(b.x(), b.y(), b.z()), node);
            var pa = projector.project(wa.x(), wa.y(), wa.z(), cx, cy, 300);
            var pb = projector.project(wb.x(), wb.y(), wb.z(), cx, cy, 300);
            if (pa == null || pb == null) continue;
            double distance = distanceToSegment(mouseX, mouseY, pa.x(), pa.y(), pb.x(), pb.y());
            if (distance <= 6.0) hits.add(new Hit(new int[]{edge[0], edge[1]},
                    (pa.depth() + pb.depth()) * 0.5, distance));
        }
        hits.sort(java.util.Comparator.comparingDouble(Hit::depth).thenComparingDouble(Hit::distance));
        var result = new java.util.ArrayList<int[]>(hits.size());
        for (Hit hit : hits) result.add(hit.edge());
        return result;
    }

    public int meshFaceHit(ModelNode node, ViewportProjector projector,
                              double mouseX, double mouseY, int cx, int cy) {
        var hits = meshFaceHits(node, projector, mouseX, mouseY, cx, cy);
        return hits.isEmpty() ? -1 : hits.get(0);
    }

    /** Returns all face hits ordered front-to-back by average camera depth. */
    public java.util.List<Integer> meshFaceHits(ModelNode node, ViewportProjector projector,
                                                 double mouseX, double mouseY, int cx, int cy) {
        if (node == null) return java.util.List.of();
        var mesh = node.ensureMeshGeometry();
        if (mesh == null) return java.util.List.of();
        record Hit(int face, double depth, double distance) {}
        var hits = new java.util.ArrayList<Hit>();
        for (int faceIndex = 0; faceIndex < mesh.faces().size(); faceIndex++) {
            int[] indices = mesh.faces().get(faceIndex).vertices();
            ViewportProjector.Point[] projected = new ViewportProjector.Point[indices.length];
            boolean valid = true;
            double depth = 0.0;
            for (int i = 0; i < indices.length; i++) {
                var v = mesh.vertices().get(indices[i]);
                TransformMath.Point world = TransformMath.applyHierarchy(
                        new TransformMath.Point(v.x(), v.y(), v.z()), node);
                projected[i] = projector.project(world.x(), world.y(), world.z(), cx, cy, 300);
                if (projected[i] == null) { valid = false; break; }
                depth += projected[i].depth();
            }
            if (!valid) continue;
            depth /= projected.length;
            double distance = pointToPolygon(mouseX, mouseY, projected);
            if (distance <= 8.0) hits.add(new Hit(faceIndex, depth, distance));
        }
        hits.sort(java.util.Comparator.comparingDouble(Hit::depth).thenComparingDouble(Hit::distance));
        var result = new java.util.ArrayList<Integer>(hits.size());
        for (Hit hit : hits) result.add(hit.face());
        return result;
    }

    private double pointToPolygon(double px, double py, ViewportProjector.Point[] polygon) {
        if (polygon.length == 0) return Double.POSITIVE_INFINITY;

        boolean inside = false;
        for (int i = 0, j = polygon.length - 1; i < polygon.length; j = i++) {
            double xi = polygon[i].x(), yi = polygon[i].y();
            double xj = polygon[j].x(), yj = polygon[j].y();
            boolean intersects = ((yi > py) != (yj > py))
                    && (px < (xj - xi) * (py - yi) / ((yj - yi) == 0 ? 1e-9 : (yj - yi)) + xi);
            if (intersects) inside = !inside;
        }
        if (inside) return 0.0;

        double best = Double.POSITIVE_INFINITY;
        for (int i = 0; i < polygon.length; i++) {
            var a = polygon[i];
            var b = polygon[(i + 1) % polygon.length];
            best = Math.min(best, distanceToSegment(px, py, a.x(), a.y(), b.x(), b.y()));
        }
        return best;
    }

    private double pointToQuad(double px, double py, ViewportProjector.Point[] q) {
        double d = 0;
        boolean inside = true;
        for (int i = 0; i < 4; i++) {
            ViewportProjector.Point a = q[i];
            ViewportProjector.Point b = q[(i + 1) % 4];
            double cross = (b.x() - a.x()) * (py - a.y()) - (b.y() - a.y()) * (px - a.x());
            if (i == 0) {
                inside = cross >= 0;
            } else if ((cross >= 0) != inside) {
                inside = false;
            }
            d = Math.max(d, distanceToSegment(px, py, a.x(), a.y(), b.x(), b.y()));
        }
        if (inside) return 0;
        return d;
    }

    public double faceDragAmount(GeometryFace face, ViewportProjector projector,
                                  double deltaX, double deltaY) {
        if (face == null || face == GeometryFace.NONE) return 0.0;
        Axis axis = switch (face) {
            case POS_X, NEG_X -> Axis.X;
            case POS_Y, NEG_Y -> Axis.Y;
            case POS_Z, NEG_Z -> Axis.Z;
            case NONE -> Axis.NONE;
        };
        return dragAmount(axis, projector, deltaX, deltaY);
    }

    public double dragAmount(Axis axis, ViewportProjector projector, double deltaX, double deltaY) {
        if(axis==Axis.NONE) return 0;
        double yaw=Math.toRadians(projector.cameraYaw());
        double pitch=Math.toRadians(projector.cameraPitch());
        double sx, sy;
        Axis base = axis;
        if (axis == Axis.NEG_X) base = Axis.X;
        else if (axis == Axis.NEG_Y) base = Axis.Y;
        else if (axis == Axis.NEG_Z) base = Axis.Z;

        if(base==Axis.X) { sx=Math.cos(yaw); sy=-Math.sin(yaw)*Math.sin(pitch); }
        else if(base==Axis.Y) { sx=0; sy=-Math.cos(pitch); }
        else { sx=-Math.sin(yaw); sy=-Math.cos(yaw)*Math.sin(pitch); }
        double len=Math.hypot(sx,sy);
        if(len<0.05) return 0;
        sx/=len; sy/=len;
        return (deltaX*sx + deltaY*sy) / 35.0;
    }

    private double distanceToSegment(double px,double py,double x1,double y1,double x2,double y2){
        double dx=x2-x1,dy=y2-y1;
        if(dx==0 && dy==0) return Math.hypot(px-x1,py-y1);
        double t=((px-x1)*dx+(py-y1)*dy)/(dx*dx+dy*dy);
        t=Math.max(0,Math.min(1,t));
        return Math.hypot(px-(x1+t*dx),py-(y1+t*dy));
    }
}
