package whitevoid.create.editor.geometry;

import java.util.LinkedHashSet;
import java.util.Set;
import whitevoid.create.model.MeshGeometry;
import whitevoid.create.model.ModelNode;

public final class MeshTopologySelection {
    private MeshTopologySelection() {}

    public static Set<Integer> linkedVertices(MeshGeometry mesh, Set<Integer> seeds) {
        Set<Integer> result = new LinkedHashSet<>(seeds);
        boolean changed;
        do {
            changed = false;
            for (MeshGeometry.Face face : mesh.faces()) {
                int[] v = face.vertices();
                boolean touches = false;
                for (int id : v) if (result.contains(id)) { touches = true; break; }
                if (touches) for (int id : v) changed |= result.add(id);
            }
        } while (changed);
        return result;
    }

    public static Set<Integer> linkedFaces(MeshGeometry mesh, Set<Integer> seeds) {
        Set<Integer> result = new LinkedHashSet<>(seeds);
        boolean changed;
        do {
            changed = false;
            for (int i = 0; i < mesh.faces().size(); i++) {
                if (result.contains(i)) continue;
                int[] a = mesh.faces().get(i).vertices();
                for (int j : result) {
                    if (shareVertex(a, mesh.faces().get(j).vertices())) {
                        if (result.add(i)) changed = true;
                        break;
                    }
                }
            }
        } while (changed);
        return result;
    }

    public static Set<Long> linkedEdges(MeshGeometry mesh, Set<Long> seeds) {
        Set<Long> result = new LinkedHashSet<>(seeds);
        boolean changed;
        do {
            changed = false;
            for (int[] edge : ModelRenderer.meshEdges(mesh)) {
                long key = edgeKey(edge[0], edge[1]);
                if (result.contains(key)) continue;
                for (long seed : result) {
                    int a = (int)(seed >>> 32), b = (int)seed;
                    if (edge[0] == a || edge[0] == b || edge[1] == a || edge[1] == b) {
                        if (result.add(key)) changed = true;
                        break;
                    }
                }
            }
        } while (changed);
        return result;
    }

    public static Set<Integer> selectedFacesForVertices(MeshGeometry mesh, Set<Integer> vertices) {
        Set<Integer> result = new LinkedHashSet<>();
        for (int i=0;i<mesh.faces().size();i++) {
            for (int v : mesh.faces().get(i).vertices()) {
                if (vertices.contains(v)) { result.add(i); break; }
            }
        }
        return result;
    }

    public static Set<Integer> selectedFacesForEdges(MeshGeometry mesh, Set<Long> edges) {
        Set<Integer> result = new LinkedHashSet<>();
        for (int i=0;i<mesh.faces().size();i++) {
            int[] v=mesh.faces().get(i).vertices();
            for(int j=0;j<v.length;j++) {
                long k=edgeKey(v[j],v[(j+1)%v.length]);
                if(edges.contains(k)){result.add(i);break;}
            }
        }
        return result;
    }

    private static boolean shareVertex(int[] a,int[] b) {
        for(int x:a) for(int y:b) if(x==y) return true;
        return false;
    }

    private static long edgeKey(int a,int b) {
        int lo=Math.min(a,b), hi=Math.max(a,b);
        return ((long)lo<<32)|(hi&0xffffffffL);
    }
}
