package whitevoid.create.editor.geometry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import whitevoid.create.model.MeshGeometry;

/**
 * Immutable topology snapshot for a MeshGeometry.
 *
 * The mesh itself remains the source of truth; this object provides one
 * consistent adjacency view for selection, validation and modeling tools.
 * Build a new snapshot after a topology-changing operation.
 */
public final class MeshTopology {
    private final MeshGeometry mesh;
    private final Map<Long, List<Integer>> edgeFaces = new LinkedHashMap<>();
    private final Map<Integer, Set<Long>> vertexEdges = new LinkedHashMap<>();
    private final Map<Integer, Set<Integer>> faceNeighbors = new LinkedHashMap<>();

    private MeshTopology(MeshGeometry mesh) {
        if (mesh == null) throw new IllegalArgumentException("Mesh cannot be null");
        this.mesh = mesh;
        build();
    }

    public static MeshTopology of(MeshGeometry mesh) {
        return new MeshTopology(mesh);
    }

    public MeshGeometry mesh() {
        return mesh;
    }

    private void build() {
        for (int i = 0; i < mesh.vertices().size(); i++) {
            vertexEdges.put(i, new LinkedHashSet<>());
        }

        for (int faceIndex = 0; faceIndex < mesh.faces().size(); faceIndex++) {
            int[] ids = mesh.faces().get(faceIndex).vertices();
            for (int i = 0; i < ids.length; i++) {
                int a = ids[i];
                int b = ids[(i + 1) % ids.length];
                long edge = edgeKey(a, b);
                edgeFaces.computeIfAbsent(edge, ignored -> new ArrayList<>()).add(faceIndex);
                vertexEdges.computeIfAbsent(a, ignored -> new LinkedHashSet<>()).add(edge);
                vertexEdges.computeIfAbsent(b, ignored -> new LinkedHashSet<>()).add(edge);
            }
            faceNeighbors.put(faceIndex, new LinkedHashSet<>());
        }

        for (List<Integer> faces : edgeFaces.values()) {
            for (int i = 0; i < faces.size(); i++) {
                for (int j = i + 1; j < faces.size(); j++) {
                    faceNeighbors.get(faces.get(i)).add(faces.get(j));
                    faceNeighbors.get(faces.get(j)).add(faces.get(i));
                }
            }
        }
    }

    public List<Integer> adjacentFaces(long edge) {
        List<Integer> faces = edgeFaces.get(edge);
        return faces == null ? List.of() : Collections.unmodifiableList(faces);
    }

    public Set<Long> edges() {
        return Collections.unmodifiableSet(edgeFaces.keySet());
    }

    public Set<Long> edgesOfVertex(int vertex) {
        Set<Long> edges = vertexEdges.get(vertex);
        return edges == null ? Set.of() : Collections.unmodifiableSet(edges);
    }

    public Set<Integer> neighborsOfVertex(int vertex) {
        Set<Integer> result = new LinkedHashSet<>();
        for (long edge : edgesOfVertex(vertex)) {
            int a = edgeA(edge);
            int b = edgeB(edge);
            result.add(a == vertex ? b : a);
        }
        return result;
    }

    public Set<Integer> neighborsOfFace(int face) {
        Set<Integer> result = faceNeighbors.get(face);
        return result == null ? Set.of() : Collections.unmodifiableSet(result);
    }

    public Set<Long> boundaryEdges() {
        Set<Long> result = new LinkedHashSet<>();
        for (Map.Entry<Long, List<Integer>> entry : edgeFaces.entrySet()) {
            if (entry.getValue().size() == 1) result.add(entry.getKey());
        }
        return result;
    }

    public Set<Long> nonManifoldEdges() {
        Set<Long> result = new LinkedHashSet<>();
        for (Map.Entry<Long, List<Integer>> entry : edgeFaces.entrySet()) {
            if (entry.getValue().size() > 2) result.add(entry.getKey());
        }
        return result;
    }

    public boolean containsEdge(int a, int b) {
        return edgeFaces.containsKey(edgeKey(a, b));
    }

    public boolean isBoundaryEdge(int a, int b) {
        return adjacentFaces(edgeKey(a, b)).size() == 1;
    }

    public boolean isManifoldEdge(int a, int b) {
        return adjacentFaces(edgeKey(a, b)).size() == 2;
    }

    public static long edgeKey(int a, int b) {
        int lo = Math.min(a, b);
        int hi = Math.max(a, b);
        return ((long) lo << 32) | (hi & 0xffffffffL);
    }

    public static int edgeA(long key) {
        return (int) (key >>> 32);
    }

    public static int edgeB(long key) {
        return (int) key;
    }
}
