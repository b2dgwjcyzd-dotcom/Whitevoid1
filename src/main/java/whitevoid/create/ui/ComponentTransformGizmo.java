package whitevoid.create.ui;

import whitevoid.create.editor.geometry.MeshSelectionMode;
import whitevoid.create.model.MeshGeometry;
import whitevoid.create.model.ModelNode;
import whitevoid.create.model.TransformMath;

public final class ComponentTransformGizmo {
    private static final double MIN_GIZMO_RADIUS = 1.5;
    private static final double MAX_GIZMO_RADIUS = 4.0;

    public static double gizmoRadius(ViewportProjector projector) {
        if (projector == null) return 2.4;
        return Math.max(MIN_GIZMO_RADIUS, Math.min(MAX_GIZMO_RADIUS,
                projector.cameraDistance() * 0.30));
    }

    public enum Axis { NONE, X, Y, Z }
    public enum Operation { MOVE, ROTATE, SCALE }
    public enum PivotMode {
        MEDIAN,
        ACTIVE,
        BOUNDS_CENTER;
        public PivotMode next() {
            return values()[(ordinal() + 1) % values().length];
        }
    }

    public TransformMath.Point pivot(ModelNode node, MeshSelectionMode mode,
                                     java.util.List<Integer> vertices,
                                     java.util.List<int[]> edges,
                                     java.util.List<Integer> faces) {
        if (node == null) return new TransformMath.Point(0,0,0);
        MeshGeometry mesh=node.ensureMeshGeometry();
        if(mesh==null) return TransformMath.applyHierarchy(new TransformMath.Point(0,0,0),node);

        java.util.LinkedHashSet<Integer> ids=new java.util.LinkedHashSet<>();
        if(mode==MeshSelectionMode.VERTEX) ids.addAll(vertices);
        else if(mode==MeshSelectionMode.EDGE) for(int[] e:edges){ids.add(e[0]);ids.add(e[1]);}
        else for(int fi:faces) if(fi>=0&&fi<mesh.faces().size()) for(int id:mesh.faces().get(fi).vertices()) ids.add(id);

        if(ids.isEmpty()) return TransformMath.applyHierarchy(new TransformMath.Point(0,0,0),node);
        double x=0,y=0,z=0; int count=0;
        for(int id:ids){
            if(id<0||id>=mesh.vertices().size()) continue;
            var v=mesh.vertices().get(id);
            TransformMath.Point w=TransformMath.applyHierarchy(new TransformMath.Point(v.x(),v.y(),v.z()),node);
            x+=w.x();y+=w.y();z+=w.z();count++;
        }
        if(count==0) return TransformMath.applyHierarchy(new TransformMath.Point(0,0,0),node);
        return new TransformMath.Point(x/count,y/count,z/count);
    }

    public TransformMath.Point localPivot(ModelNode node, MeshSelectionMode mode,
                                          java.util.List<Integer> vertices,
                                          java.util.List<int[]> edges,
                                          java.util.List<Integer> faces) {
        return localPivot(node, mode, vertices, edges, faces, PivotMode.MEDIAN);
    }

    public TransformMath.Point localPivot(ModelNode node, MeshSelectionMode mode,
                                          java.util.List<Integer> vertices,
                                          java.util.List<int[]> edges,
                                          java.util.List<Integer> faces,
                                          PivotMode pivotMode) {
        return localPivot(node, mode, vertices, edges, faces, pivotMode, null);
    }

    public TransformMath.Point localPivot(ModelNode node, MeshSelectionMode mode,
                                          java.util.List<Integer> vertices,
                                          java.util.List<int[]> edges,
                                          java.util.List<Integer> faces,
                                          PivotMode pivotMode,
                                          whitevoid.create.editor.geometry.MeshComponentSelection selection) {
        if(node==null) return new TransformMath.Point(0,0,0);
        MeshGeometry mesh=node.ensureMeshGeometry();
        if(mesh==null) return new TransformMath.Point(0,0,0);
        java.util.LinkedHashSet<Integer> ids=new java.util.LinkedHashSet<>();
        if(mode==MeshSelectionMode.VERTEX) ids.addAll(vertices);
        else if(mode==MeshSelectionMode.EDGE) for(int[] e:edges){ids.add(e[0]);ids.add(e[1]);}
        else for(int fi:faces) if(fi>=0&&fi<mesh.faces().size()) for(int id:mesh.faces().get(fi).vertices()) ids.add(id);
        if(ids.isEmpty()) return new TransformMath.Point(0,0,0);

        if(pivotMode == PivotMode.ACTIVE) {
            int id = -1;
            if (selection != null) {
                if (mode == MeshSelectionMode.VERTEX) id = selection.activeVertex();
                else if (mode == MeshSelectionMode.EDGE) {
                    int a = selection.activeEdgeA();
                    int b = selection.activeEdgeB();
                    if (a >= 0 && b >= 0 && a < mesh.vertices().size() && b < mesh.vertices().size()) {
                        var va = mesh.vertices().get(a);
                        var vb = mesh.vertices().get(b);
                        return new TransformMath.Point((va.x()+vb.x())*0.5, (va.y()+vb.y())*0.5, (va.z()+vb.z())*0.5);
                    }
                } else id = selection.activeFace();
            }
            if (id < 0 || (mode == MeshSelectionMode.VERTEX && id >= mesh.vertices().size())
                    || (mode == MeshSelectionMode.FACE && id >= mesh.faces().size())) {
                id = ids.iterator().next();
            }
            if (mode == MeshSelectionMode.FACE) {
                int[] face = mesh.faces().get(id).vertices();
                double x=0,y=0,z=0;
                for (int vertex : face) { var v=mesh.vertices().get(vertex); x+=v.x(); y+=v.y(); z+=v.z(); }
                return new TransformMath.Point(x/face.length,y/face.length,z/face.length);
            }
            var v=mesh.vertices().get(id);
            return new TransformMath.Point(v.x(),v.y(),v.z());
        }

        if(pivotMode == PivotMode.BOUNDS_CENTER) {
            double minX=Double.POSITIVE_INFINITY,minY=Double.POSITIVE_INFINITY,minZ=Double.POSITIVE_INFINITY;
            double maxX=Double.NEGATIVE_INFINITY,maxY=Double.NEGATIVE_INFINITY,maxZ=Double.NEGATIVE_INFINITY;
            for(int id:ids) if(id>=0&&id<mesh.vertices().size()){
                var v=mesh.vertices().get(id);
                minX=Math.min(minX,v.x()); minY=Math.min(minY,v.y()); minZ=Math.min(minZ,v.z());
                maxX=Math.max(maxX,v.x()); maxY=Math.max(maxY,v.y()); maxZ=Math.max(maxZ,v.z());
            }
            return new TransformMath.Point((minX+maxX)/2,(minY+maxY)/2,(minZ+maxZ)/2);
        }

        double x=0,y=0,z=0; int n=0;
        for(int id:ids) if(id>=0&&id<mesh.vertices().size()){
            var v=mesh.vertices().get(id); x+=v.x(); y+=v.y(); z+=v.z(); n++;
        }
        return n==0?new TransformMath.Point(0,0,0):new TransformMath.Point(x/n,y/n,z/n);
    }

    public Axis hover(ModelNode node, MeshSelectionMode mode, java.util.List<Integer> vertices,
                       java.util.List<int[]> edges, java.util.List<Integer> faces,
                       ViewportProjector projector, double mouseX, double mouseY, int cx, int cy,
                       Operation operation) {
        return hover(node, mode, vertices, edges, faces, projector, mouseX, mouseY, cx, cy,
                operation, PivotMode.MEDIAN, null);
    }

    public Axis hover(ModelNode node, MeshSelectionMode mode, java.util.List<Integer> vertices,
                       java.util.List<int[]> edges, java.util.List<Integer> faces,
                       ViewportProjector projector, double mouseX, double mouseY, int cx, int cy,
                       Operation operation, PivotMode pivotMode) {
        return hover(node, mode, vertices, edges, faces, projector, mouseX, mouseY, cx, cy,
                operation, pivotMode, null);
    }

    public Axis hover(ModelNode node, MeshSelectionMode mode, java.util.List<Integer> vertices,
                       java.util.List<int[]> edges, java.util.List<Integer> faces,
                       ViewportProjector projector, double mouseX, double mouseY, int cx, int cy,
                       Operation operation, PivotMode pivotMode,
                       whitevoid.create.editor.geometry.MeshComponentSelection selection) {
        if (operation == Operation.ROTATE) {
            return rotationHit(node, mode, vertices, edges, faces, projector, mouseX, mouseY, cx, cy, pivotMode, selection);
        }
        return hit(node, mode, vertices, edges, faces, projector, mouseX, mouseY, cx, cy, pivotMode, selection);
    }

    private Axis rotationHit(ModelNode node, MeshSelectionMode mode, java.util.List<Integer> vertices,
                              java.util.List<int[]> edges, java.util.List<Integer> faces,
                              ViewportProjector projector, double mouseX, double mouseY, int cx, int cy,
                              PivotMode pivotMode) {
        return rotationHit(node, mode, vertices, edges, faces, projector, mouseX, mouseY, cx, cy, pivotMode, null);
    }

    private Axis rotationHit(ModelNode node, MeshSelectionMode mode, java.util.List<Integer> vertices,
                              java.util.List<int[]> edges, java.util.List<Integer> faces,
                              ViewportProjector projector, double mouseX, double mouseY, int cx, int cy,
                              PivotMode pivotMode, whitevoid.create.editor.geometry.MeshComponentSelection selection) {
        TransformMath.Point localPivot = localPivot(node, mode, vertices, edges, faces, pivotMode, selection);
        TransformMath.Point worldPivot = TransformMath.applyHierarchy(localPivot, node);
        double best = 8.0;
        Axis result = Axis.NONE;

        int[][] planes = {{1, 2}, {0, 2}, {0, 1}};
        Axis[] axes = {Axis.X, Axis.Y, Axis.Z};
        for (int axis = 0; axis < 3; axis++) {
            double previousX = 0.0;
            double previousY = 0.0;
            boolean havePrevious = false;
            for (int i = 0; i <= 48; i++) {
                double angle = Math.PI * 2.0 * i / 48.0;
                double[] offset = {0.0, 0.0, 0.0};
                offset[planes[axis][0]] = Math.cos(angle) * gizmoRadius(projector);
                offset[planes[axis][1]] = Math.sin(angle) * gizmoRadius(projector);

                TransformMath.Point local = new TransformMath.Point(
                        localPivot.x() + offset[0],
                        localPivot.y() + offset[1],
                        localPivot.z() + offset[2]);
                TransformMath.Point world = TransformMath.applyHierarchy(local, node);
                var point = projector.project(world.x(), world.y(), world.z(), cx, cy, 300);
                if (point == null) continue;

                if (havePrevious) {
                    double distance = distance(mouseX, mouseY, previousX, previousY, point.x(), point.y());
                    if (distance < best) {
                        best = distance;
                        result = axes[axis];
                    }
                }
                previousX = point.x();
                previousY = point.y();
                havePrevious = true;
            }
        }
        return result;
    }

    public Axis hit(ModelNode node, MeshSelectionMode mode, java.util.List<Integer> vertices,
                    java.util.List<int[]> edges, java.util.List<Integer> faces,
                    ViewportProjector projector, double mouseX,double mouseY,int cx,int cy) {
        return hit(node, mode, vertices, edges, faces, projector, mouseX, mouseY, cx, cy, PivotMode.MEDIAN);
    }

    public Axis hit(ModelNode node, MeshSelectionMode mode, java.util.List<Integer> vertices,
                    java.util.List<int[]> edges, java.util.List<Integer> faces,
                    ViewportProjector projector, double mouseX,double mouseY,int cx,int cy,
                    PivotMode pivotMode) {
        return hit(node, mode, vertices, edges, faces, projector, mouseX, mouseY, cx, cy, pivotMode, null);
    }

    private Axis hit(ModelNode node, MeshSelectionMode mode, java.util.List<Integer> vertices,
                    java.util.List<int[]> edges, java.util.List<Integer> faces,
                    ViewportProjector projector, double mouseX,double mouseY,int cx,int cy,
                    PivotMode pivotMode, whitevoid.create.editor.geometry.MeshComponentSelection selection) {
        TransformMath.Point p3=TransformMath.applyHierarchy(
                localPivot(node, mode, vertices, edges, faces, pivotMode, selection), node);
        var o=projector.project(p3.x(),p3.y(),p3.z(),cx,cy,300);
        if(o==null) return Axis.NONE;
        double best=12; Axis result=Axis.NONE;
        double[][] dirs={{gizmoRadius,0,0},{0,gizmoRadius,0},{0,0,gizmoRadius}};
        Axis[] axes={Axis.X,Axis.Y,Axis.Z};
        for(int i=0;i<3;i++){
            TransformMath.Point localAxisEnd = new TransformMath.Point(
                    localPivot(node, mode, vertices, edges, faces, pivotMode, selection).x() + dirs[i][0],
                    localPivot(node, mode, vertices, edges, faces, pivotMode, selection).y() + dirs[i][1],
                    localPivot(node, mode, vertices, edges, faces, pivotMode, selection).z() + dirs[i][2]);
            TransformMath.Point q3=TransformMath.applyHierarchy(localAxisEnd,node);
            var q=projector.project(q3.x(),q3.y(),q3.z(),cx,cy,300);
            if(q==null) continue;
            double d=distance(mouseX,mouseY,o.x(),o.y(),q.x(),q.y());
            if(d<best){best=d;result=axes[i];}
        }
        return result;
    }

    /**
     * Projects the actual local model axis and measures the mouse delta along it.
     * The old overload is kept for compatibility with non-component callers.
     */
    public double amount(Axis axis, ViewportProjector projector,double dx,double dy){
        if(axis==Axis.NONE)return 0;
        double yaw=Math.toRadians(projector.cameraYaw()), pitch=Math.toRadians(projector.cameraPitch());
        double sx,sy;
        if(axis==Axis.X){sx=Math.cos(yaw);sy=-Math.sin(yaw)*Math.sin(pitch);}
        else if(axis==Axis.Y){sx=0;sy=-Math.cos(pitch);}
        else {sx=-Math.sin(yaw);sy=-Math.cos(yaw)*Math.sin(pitch);}
        double len=Math.hypot(sx,sy); if(len<0.05)return 0;
        return (dx*(sx/len)+dy*(sy/len))/35.0;
    }

    public double amount(Axis axis, ViewportProjector projector, ModelNode node,
                         TransformMath.Point localPivot, int cx, int cy,
                         double dx, double dy) {
        if (axis == Axis.NONE || node == null || localPivot == null) return 0;

        TransformMath.Point worldPivot = TransformMath.applyHierarchy(localPivot, node);
        var origin = projector.project(worldPivot.x(), worldPivot.y(), worldPivot.z(), cx, cy, 300);
        if (origin == null) return 0;

        int axisIndex = axis == Axis.X ? 0 : axis == Axis.Y ? 1 : 2;
        double[] offset = {0.0, 0.0, 0.0};
        offset[axisIndex] = gizmoRadius(projector);
        TransformMath.Point worldEnd = TransformMath.applyHierarchy(
                new TransformMath.Point(localPivot.x() + offset[0],
                        localPivot.y() + offset[1],
                        localPivot.z() + offset[2]), node);
        var end = projector.project(worldEnd.x(), worldEnd.y(), worldEnd.z(), cx, cy, 300);
        if (end == null) return 0;

        double sx = end.x() - origin.x();
        double sy = end.y() - origin.y();
        double length = Math.hypot(sx, sy);
        if (length < 2.0) return 0;

        return (dx * (sx / length) + dy * (sy / length)) / 35.0;
    }

    /**
     * Measures rotation against the actual projected 3D ring for the selected axis.
     * This keeps drag direction consistent with the visible local rotation ring.
     */
    public double rotationAmount(Axis axis, ModelNode node, TransformMath.Point localPivot,
                                 double startX, double startY, double mouseX, double mouseY,
                                 ViewportProjector projector, int cx, int cy) {
        if (axis == Axis.NONE || node == null || localPivot == null) return 0;

        TransformMath.Point worldPivot = TransformMath.applyHierarchy(localPivot, node);
        var center = projector.project(worldPivot.x(), worldPivot.y(), worldPivot.z(), cx, cy, 300);
        if (center == null) return 0;

        int axisIndex = axis == Axis.X ? 0 : axis == Axis.Y ? 1 : 2;
        int[] plane = axisIndex == 0 ? new int[]{1, 2}
                : axisIndex == 1 ? new int[]{0, 2}
                : new int[]{0, 1};

        double bestDistance = 24.0;
        double bestTangentX = 0.0;
        double bestTangentY = 0.0;
        boolean found = false;

        for (int i = 0; i < 64; i++) {
            double a0 = Math.PI * 2.0 * i / 64.0;
            double a1 = Math.PI * 2.0 * (i + 1) / 64.0;
            var p0 = projectRingPoint(node, localPivot, plane, a0, projector, cx, cy);
            var p1 = projectRingPoint(node, localPivot, plane, a1, projector, cx, cy);
            if (p0 == null || p1 == null) continue;

            double distance = distance(startX, startY, p0.x(), p0.y(), p1.x(), p1.y());
            if (distance < bestDistance) {
                bestDistance = distance;
                bestTangentX = p1.x() - p0.x();
                bestTangentY = p1.y() - p0.y();
                found = true;
            }
        }

        double startAngle = Math.atan2(startY - center.y(), startX - center.x());
        double currentAngle = Math.atan2(mouseY - center.y(), mouseX - center.x());
        double delta = normalizeDegrees(Math.toDegrees(currentAngle - startAngle));

        if (!found) return delta;

        double movementX = mouseX - startX;
        double movementY = mouseY - startY;
        double tangentLength = Math.hypot(bestTangentX, bestTangentY);
        if (tangentLength > 0.001) {
            double dot = movementX * bestTangentX + movementY * bestTangentY;
            if (dot < 0.0) delta = -Math.abs(delta);
            else if (dot > 0.0) delta = Math.abs(delta);
        }
        return delta;
    }

    private TransformMath.Point projectRingPoint(ModelNode node, TransformMath.Point localPivot,
                                                   int[] plane, double angle,
                                                   ViewportProjector projector, int cx, int cy) {
        double[] offset = {0.0, 0.0, 0.0};
        offset[plane[0]] = Math.cos(angle) * gizmoRadius(projector);
        offset[plane[1]] = Math.sin(angle) * gizmoRadius(projector);
        var local = new TransformMath.Point(localPivot.x() + offset[0],
                localPivot.y() + offset[1], localPivot.z() + offset[2]);
        var world = TransformMath.applyHierarchy(local, node);
        return projector.project(world.x(), world.y(), world.z(), cx, cy, 300);
    }

    private double normalizeDegrees(double degrees) {
        while (degrees > 180.0) degrees -= 360.0;
        while (degrees < -180.0) degrees += 360.0;
        return degrees;
    }

    private double distance(double px, double py, double x1, double y1, double x2, double y2) {
        double dx = x2 - x1, dy = y2 - y1;
        if (dx == 0.0 && dy == 0.0) return Math.hypot(px - x1, py - y1);
        double t = ((px - x1) * dx + (py - y1) * dy) / (dx * dx + dy * dy);
        t = Math.max(0.0, Math.min(1.0, t));
        return Math.hypot(px - (x1 + t * dx), py - (y1 + t * dy));
    }

    public double rotationAmount(Axis axis, double dx, double dy) {
        if (axis == Axis.NONE) return 0;
        return (dx - dy) * 1.2;
    }

    public double scaleFactor(Axis axis, ViewportProjector projector, double dx, double dy) {
        double amount = amount(axis, projector, dx, dy);
        return Math.max(0.01, 1.0 + amount * 0.5);
    }

    public double scaleFactor(Axis axis, ViewportProjector projector, ModelNode node,
                              TransformMath.Point localPivot, int cx, int cy,
                              double dx, double dy) {
        double amount = amount(axis, projector, node, localPivot, cx, cy, dx, dy);
        return Math.max(0.01, 1.0 + amount * 0.5);
    }

    private double distance(double px,double py,double x1,double y1,double x2,double y2){
        double dx=x2-x1,dy=y2-y1;
        if(dx==0&&dy==0)return Math.hypot(px-x1,py-y1);
        double t=((px-x1)*dx+(py-y1)*dy)/(dx*dx+dy*dy);
        t=Math.max(0,Math.min(1,t));
        return Math.hypot(px-(x1+t*dx),py-(y1+t*dy));
    }
}
