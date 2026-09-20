package whitevoid.create.ui;

import whitevoid.create.editor.transform.TransformMode;
import whitevoid.create.model.ModelNode;
import whitevoid.create.model.TransformMath;

public final class ViewportGizmo {
    public Axis hoveredAxis(ModelNode node, TransformMode mode, ViewportProjector projector, double mouseX, double mouseY, int cx, int cy) {
        return hit(node, mode, projector, mouseX, mouseY, cx, cy);
    }
    public enum Axis { NONE, X, Y, Z }

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

    public double dragAmount(Axis axis, ViewportProjector projector, double deltaX, double deltaY) {
        if(axis==Axis.NONE) return 0;
        double yaw=Math.toRadians(projector.cameraYaw());
        double pitch=Math.toRadians(projector.cameraPitch());
        double sx, sy;
        if(axis==Axis.X) { sx=Math.cos(yaw); sy=-Math.sin(yaw)*Math.sin(pitch); }
        else if(axis==Axis.Y) { sx=0; sy=-Math.cos(pitch); }
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
