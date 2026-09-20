package whitevoid.create.ui;

import whitevoid.create.editor.transform.TransformMode;
import whitevoid.create.model.ModelNode;
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
        if (node == null || node.geometry() == null) return GeometryFace.NONE;

        GeometryFace bestFace = GeometryFace.NONE;
        double bestDepth = Double.POSITIVE_INFINITY;
        double bestDistance = 8.0;

        double hx = node.geometry().width() * 0.5;
        double hy = node.geometry().height() * 0.5;
        double hz = node.geometry().depth() * 0.5;

        double[][] faces = {
                { hx, 0, 0}, {-hx, 0, 0},
                { 0, hy, 0}, { 0,-hy, 0},
                { 0, 0, hz}, { 0, 0,-hz}
        };
        GeometryFace[] faceTypes = {
                GeometryFace.POS_X, GeometryFace.NEG_X,
                GeometryFace.POS_Y, GeometryFace.NEG_Y,
                GeometryFace.POS_Z, GeometryFace.NEG_Z
        };

        TransformMath.Point[][] corners = {
                {
                        new TransformMath.Point(hx,-hy,-hz), new TransformMath.Point(hx,hy,-hz),
                        new TransformMath.Point(hx,hy,hz), new TransformMath.Point(hx,-hy,hz)
                },
                {
                        new TransformMath.Point(-hx,-hy,hz), new TransformMath.Point(-hx,hy,hz),
                        new TransformMath.Point(-hx,hy,-hz), new TransformMath.Point(-hx,-hy,-hz)
                },
                {
                        new TransformMath.Point(-hx,hy,-hz), new TransformMath.Point(-hx,hy,hz),
                        new TransformMath.Point(hx,hy,hz), new TransformMath.Point(hx,hy,-hz)
                },
                {
                        new TransformMath.Point(-hx,-hy,hz), new TransformMath.Point(-hx,-hy,-hz),
                        new TransformMath.Point(hx,-hy,-hz), new TransformMath.Point(hx,-hy,hz)
                },
                {
                        new TransformMath.Point(-hx,-hy,hz), new TransformMath.Point(hx,-hy,hz),
                        new TransformMath.Point(hx,hy,hz), new TransformMath.Point(-hx,hy,hz)
                },
                {
                        new TransformMath.Point(hx,-hy,-hz), new TransformMath.Point(-hx,-hy,-hz),
                        new TransformMath.Point(-hx,hy,-hz), new TransformMath.Point(hx,hy,-hz)
                }
        };

        for (int i = 0; i < 6; i++) {
            ViewportProjector.Point[] p = new ViewportProjector.Point[4];
            boolean valid = true;
            for (int j = 0; j < 4; j++) {
                TransformMath.Point world = TransformMath.applyHierarchy(corners[i][j], node);
                p[j] = projector.project(world.x(), world.y(), world.z(), cx, cy, 300);
                if (p[j] == null) {
                    valid = false;
                    break;
                }
            }
            if (!valid) continue;

            double d = pointToQuad(mouseX, mouseY, p);
            if (d <= bestDistance) {
                double depth = (p[0].depth() + p[1].depth() + p[2].depth() + p[3].depth()) / 4.0;
                if (depth < bestDepth) {
                    bestDepth = depth;
                    bestFace = faceTypes[i];
                }
            }
        }

        return bestFace;
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
