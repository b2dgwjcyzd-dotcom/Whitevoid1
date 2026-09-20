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
