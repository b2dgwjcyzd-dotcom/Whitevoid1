package whitevoid.create.ui;

import whitevoid.create.editor.geometry.MeshSelectionMode;
import whitevoid.create.model.MeshGeometry;
import whitevoid.create.model.ModelNode;
import whitevoid.create.model.TransformMath;

public final class ComponentTransformGizmo {
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
        if(node==null) return new TransformMath.Point(0,0,0);
        MeshGeometry mesh=node.ensureMeshGeometry();
        if(mesh==null) return new TransformMath.Point(0,0,0);
        java.util.LinkedHashSet<Integer> ids=new java.util.LinkedHashSet<>();
        if(mode==MeshSelectionMode.VERTEX) ids.addAll(vertices);
        else if(mode==MeshSelectionMode.EDGE) for(int[] e:edges){ids.add(e[0]);ids.add(e[1]);}
        else for(int fi:faces) if(fi>=0&&fi<mesh.faces().size()) for(int id:mesh.faces().get(fi).vertices()) ids.add(id);
        if(ids.isEmpty()) return new TransformMath.Point(0,0,0);

        if(pivotMode == PivotMode.ACTIVE) {
            int id=ids.iterator().next();
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
        if (operation == Operation.ROTATE) return rotationHit(node, mode, vertices, edges, faces, projector, mouseX, mouseY, cx, cy);
        return hit(node, mode, vertices, edges, faces, projector, mouseX, mouseY, cx, cy);
    }

    private Axis rotationHit(ModelNode node, MeshSelectionMode mode, java.util.List<Integer> vertices,
                              java.util.List<int[]> edges, java.util.List<Integer> faces,
                              ViewportProjector projector, double mouseX, double mouseY, int cx, int cy) {
        TransformMath.Point p=pivot(node,mode,vertices,edges,faces);
        var o=projector.project(p.x(),p.y(),p.z(),cx,cy,300);
        if(o==null)return Axis.NONE;
        double best=9; Axis result=Axis.NONE;
        double[][] dirs={{2,0,0},{0,2,0},{0,0,2}};
        Axis[] axes={Axis.X,Axis.Y,Axis.Z};
        for(int i=0;i<3;i++){
            var q3=new TransformMath.Point(p.x()+dirs[i][0],p.y()+dirs[i][1],p.z()+dirs[i][2]);
            var q=projector.project(q3.x(),q3.y(),q3.z(),cx,cy,300);
            if(q==null)continue;
            double radius=Math.hypot(q.x()-o.x(),q.y()-o.y());
            double d=Math.abs(Math.hypot(mouseX-o.x(),mouseY-o.y())-radius);
            if(d<best){best=d;result=axes[i];}
        }
        return result;
    }

    public Axis hit(ModelNode node, MeshSelectionMode mode, java.util.List<Integer> vertices,
                    java.util.List<int[]> edges, java.util.List<Integer> faces,
                    ViewportProjector projector, double mouseX,double mouseY,int cx,int cy) {
        TransformMath.Point p3=pivot(node,mode,vertices,edges,faces);
        var o=projector.project(p3.x(),p3.y(),p3.z(),cx,cy,300);
        if(o==null) return Axis.NONE;
        double best=12; Axis result=Axis.NONE;
        double[][] dirs={{2.0,0,0},{0,2.0,0},{0,0,2.0}};
        Axis[] axes={Axis.X,Axis.Y,Axis.Z};
        for(int i=0;i<3;i++){
            TransformMath.Point q3=new TransformMath.Point(p3.x()+dirs[i][0],p3.y()+dirs[i][1],p3.z()+dirs[i][2]);
            var q=projector.project(q3.x(),q3.y(),q3.z(),cx,cy,300);
            if(q==null) continue;
            double d=distance(mouseX,mouseY,o.x(),o.y(),q.x(),q.y());
            if(d<best){best=d;result=axes[i];}
        }
        return result;
    }

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

    public double rotationAmount(Axis axis, double dx, double dy) {
        if (axis == Axis.NONE) return 0;
        double sensitivity = 1.2;
        return (dx - dy) * sensitivity;
    }

    public double scaleFactor(Axis axis, ViewportProjector projector, double dx, double dy) {
        double amount = amount(axis, projector, dx, dy);
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
