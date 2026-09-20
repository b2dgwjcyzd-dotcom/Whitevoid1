package whitevoid.create.editor.geometry;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import whitevoid.create.model.MeshGeometry;
import whitevoid.create.model.TransformMath;

public final class MeshComponentTransforms {
    private MeshComponentTransforms() {}

    public static MeshGeometry translate(MeshGeometry mesh, Set<Integer> ids,
                                         double dx, double dy, double dz) {
        MeshGeometry result=mesh;
        for(int id:ids) result=MeshOperations.moveVertex(result,id,dx,dy,dz);
        return result;
    }

    public static MeshGeometry rotate(MeshGeometry mesh, Set<Integer> ids,
                                      TransformMath.Point pivot, int axis, double degrees) {
        double r=Math.toRadians(degrees);
        double cos=Math.cos(r), sin=Math.sin(r);
        java.util.ArrayList<MeshGeometry.Vertex> vertices=new java.util.ArrayList<>(mesh.vertices());
        for(int id:ids){
            if(id<0||id>=vertices.size()) continue;
            var v=vertices.get(id);
            double x=v.x()-pivot.x(), y=v.y()-pivot.y(), z=v.z()-pivot.z();
            double nx=x,ny=y,nz=z;
            if(axis==0){ ny=y*cos-z*sin; nz=y*sin+z*cos; }
            else if(axis==1){ nx=x*cos+z*sin; nz=-x*sin+z*cos; }
            else { nx=x*cos-y*sin; ny=x*sin+y*cos; }
            vertices.set(id,new MeshGeometry.Vertex(nx+pivot.x(),ny+pivot.y(),nz+pivot.z()));
        }
        return new MeshGeometry(vertices,mesh.faces());
    }

    public static MeshGeometry scale(MeshGeometry mesh, Set<Integer> ids,
                                     TransformMath.Point pivot, int axis, double factor) {
        java.util.ArrayList<MeshGeometry.Vertex> vertices=new java.util.ArrayList<>(mesh.vertices());
        for(int id:ids){
            if(id<0||id>=vertices.size()) continue;
            var v=vertices.get(id);
            double x=v.x(),y=v.y(),z=v.z();
            if(axis==0)x=pivot.x()+(x-pivot.x())*factor;
            else if(axis==1)y=pivot.y()+(y-pivot.y())*factor;
            else if(axis==2)z=pivot.z()+(z-pivot.z())*factor;
            vertices.set(id,new MeshGeometry.Vertex(x,y,z));
        }
        return new MeshGeometry(vertices,mesh.faces());
    }

    public static MeshGeometry mirror(MeshGeometry mesh, Set<Integer> ids,
                                      TransformMath.Point pivot, int axis) {
        java.util.ArrayList<MeshGeometry.Vertex> vertices=new java.util.ArrayList<>(mesh.vertices());
        for(int id:ids){
            if(id<0||id>=vertices.size()) continue;
            var v=vertices.get(id);
            double x=v.x(), y=v.y(), z=v.z();
            if(axis==0) x=2.0*pivot.x()-x;
            else if(axis==1) y=2.0*pivot.y()-y;
            else if(axis==2) z=2.0*pivot.z()-z;
            vertices.set(id,new MeshGeometry.Vertex(x,y,z));
        }
        return new MeshGeometry(vertices,mesh.faces());
    }

    public static Set<Integer> proportionalVertices(MeshGeometry mesh, Set<Integer> selected,
                                                   TransformMath.Point pivot, double radius) {
        Set<Integer> ids = new LinkedHashSet<>();
        double r2 = radius * radius;
        for (int i = 0; i < mesh.vertices().size(); i++) {
            var v = mesh.vertices().get(i);
            double dx=v.x()-pivot.x(), dy=v.y()-pivot.y(), dz=v.z()-pivot.z();
            if (dx*dx+dy*dy+dz*dz <= r2) ids.add(i);
        }
        return ids;
    }

    public static double proportionalWeight(MeshGeometry mesh, int vertexId,
                                             Set<Integer> selected, TransformMath.Point pivot,
                                             double radius) {
        if (selected.contains(vertexId)) return 1.0;
        var v=mesh.vertices().get(vertexId);
        double dx=v.x()-pivot.x(), dy=v.y()-pivot.y(), dz=v.z()-pivot.z();
        double distance=Math.sqrt(dx*dx+dy*dy+dz*dz);
        if (distance >= radius) return 0.0;
        double t=distance/radius;
        return 1.0 - (t*t*(3.0-2.0*t));
    }

    public static MeshGeometry translateProportional(MeshGeometry mesh, Set<Integer> selected,
                                                     TransformMath.Point pivot, double radius,
                                                     double dx, double dy, double dz) {
        java.util.ArrayList<MeshGeometry.Vertex> vertices=new java.util.ArrayList<>(mesh.vertices());
        for(int i=0;i<vertices.size();i++){
            double w=proportionalWeight(mesh,i,selected,pivot,radius);
            if(w<=0) continue;
            var v=vertices.get(i);
            vertices.set(i,new MeshGeometry.Vertex(v.x()+dx*w,v.y()+dy*w,v.z()+dz*w));
        }
        return new MeshGeometry(vertices,mesh.faces());
    }

    public static MeshGeometry rotateProportional(MeshGeometry mesh, Set<Integer> selected,
                                                  TransformMath.Point pivot, double radius,
                                                  int axis, double degrees) {
        java.util.ArrayList<MeshGeometry.Vertex> vertices=new java.util.ArrayList<>(mesh.vertices());
        for(int i=0;i<vertices.size();i++){
            double w=proportionalWeight(mesh,i,selected,pivot,radius);
            if(w<=0) continue;
            var v=vertices.get(i);
            double r=Math.toRadians(degrees*w), c=Math.cos(r), sn=Math.sin(r);
            double x=v.x()-pivot.x(),y=v.y()-pivot.y(),z=v.z()-pivot.z();
            double nx=x,ny=y,nz=z;
            if(axis==0){ny=y*c-z*sn;nz=y*sn+z*c;}
            else if(axis==1){nx=x*c+z*sn;nz=-x*sn+z*c;}
            else {nx=x*c-y*sn;ny=x*sn+y*c;}
            vertices.set(i,new MeshGeometry.Vertex(nx+pivot.x(),ny+pivot.y(),nz+pivot.z()));
        }
        return new MeshGeometry(vertices,mesh.faces());
    }

    public static MeshGeometry scaleProportional(MeshGeometry mesh, Set<Integer> selected,
                                                 TransformMath.Point pivot, double radius,
                                                 int axis, double factor) {
        java.util.ArrayList<MeshGeometry.Vertex> vertices=new java.util.ArrayList<>(mesh.vertices());
        for(int i=0;i<vertices.size();i++){
            double w=proportionalWeight(mesh,i,selected,pivot,radius);
            if(w<=0) continue;
            var v=vertices.get(i);
            double f=1.0+(factor-1.0)*w;
            double x=v.x(),y=v.y(),z=v.z();
            if(axis==0)x=pivot.x()+(x-pivot.x())*f;
            else if(axis==1)y=pivot.y()+(y-pivot.y())*f;
            else z=pivot.z()+(z-pivot.z())*f;
            vertices.set(i,new MeshGeometry.Vertex(x,y,z));
        }
        return new MeshGeometry(vertices,mesh.faces());
    }

    public static Set<Integer> affectedVertices(MeshGeometry mesh, MeshSelectionMode mode,
                                                 List<Integer> vertices, List<int[]> edges,
                                                 List<Integer> faces) {
        Set<Integer> ids=new LinkedHashSet<>();
        if(mode==MeshSelectionMode.VERTEX) ids.addAll(vertices);
        else if(mode==MeshSelectionMode.EDGE) for(int[] e:edges){ids.add(e[0]);ids.add(e[1]);}
        else for(int fi:faces) if(fi>=0&&fi<mesh.faces().size())
            for(int id:mesh.faces().get(fi).vertices()) ids.add(id);
        return ids;
    }
}
