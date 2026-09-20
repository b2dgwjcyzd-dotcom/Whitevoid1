package whitevoid.create.editor.geometry;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import whitevoid.create.model.MeshGeometry;
import whitevoid.create.model.ModelRenderer;

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

    /**
     * Selects a true quad edge loop by walking across the edge opposite to the
     * current edge in each adjacent quad. Non-quad/boundary regions stop.
     */
    public static Set<Long> edgeLoop(MeshGeometry mesh, int a, int b) {
        Set<Long> result = new LinkedHashSet<>();
        long start = edgeKey(a, b);
        if (!containsEdge(mesh, a, b)) return result;
        result.add(start);

        Set<Long> frontier = new LinkedHashSet<>();
        frontier.add(start);
        while (!frontier.isEmpty()) {
            Set<Long> next = new LinkedHashSet<>();
            for (long edge : frontier) {
                int ea = edgeA(edge), eb = edgeB(edge);
                for (int faceIndex : adjacentFaces(mesh, ea, eb)) {
                    long opposite = oppositeEdge(mesh.faces().get(faceIndex).vertices(), ea, eb);
                    if (opposite >= 0 && result.add(opposite)) next.add(opposite);
                }
            }
            frontier = next;
        }
        return result;
    }

    /**
     * Selects a face strip by crossing the edge opposite the incoming edge.
     * This is intentionally quad-aware; non-quad faces terminate the strip.
     */
    public static Set<Integer> faceLoop(MeshGeometry mesh, int faceIndex) {
        Set<Integer> result = new LinkedHashSet<>();
        if (faceIndex < 0 || faceIndex >= mesh.faces().size()) return result;
        int[] start = mesh.faces().get(faceIndex).vertices();
        if (start.length != 4) {
            result.add(faceIndex);
            return result;
        }

        for (int edgeSlot = 0; edgeSlot < 2; edgeSlot++) {
            int a = start[edgeSlot];
            int b = start[(edgeSlot + 1) % 4];
            walkFaceStrip(mesh, faceIndex, a, b, result);
            int c = start[(edgeSlot + 2) % 4];
            int d = start[(edgeSlot + 3) % 4];
            walkFaceStrip(mesh, faceIndex, c, d, result);
        }
        return result;
    }

    /** Edge ring: follows the opposite edge of each adjacent quad. */
    public static Set<Long> edgeRing(MeshGeometry mesh, int a, int b) {
        return edgeLoop(mesh, a, b);
    }

    /** Face ring: selects faces reached across opposite edges of the seed face. */
    public static Set<Integer> faceRing(MeshGeometry mesh, int faceIndex) {
        return faceLoop(mesh, faceIndex);
    }

    private static void walkFaceStrip(MeshGeometry mesh, int seedFace, int edgeA, int edgeB, Set<Integer> result) {
        Queue<Integer> queue = new ArrayDeque<>();
        Set<Integer> visited = new LinkedHashSet<>();
        queue.add(seedFace);
        visited.add(seedFace);

        while (!queue.isEmpty()) {
            int current = queue.remove();
            int[] face = mesh.faces().get(current).vertices();
            if (face.length != 4) continue;

            long opposite = oppositeEdge(face, edgeA, edgeB);
            if (opposite < 0) continue;

            int oa = edgeA(opposite), ob = edgeB(opposite);
            for (int neighbor : adjacentFaces(mesh, oa, ob)) {
                if (neighbor == current) continue;
                if (result.add(neighbor) && visited.add(neighbor)) {
                    queue.add(neighbor);
                }
            }
            edgeA = oa;
            edgeB = ob;
        }
    }

    private static long oppositeEdge(int[] face, int a, int b) {
        if (face.length != 4) return -1L;
        for (int i = 0; i < 4; i++) {
            int x = face[i];
            int y = face[(i + 1) % 4];
            if (edgeKey(x, y) == edgeKey(a, b)) {
                return edgeKey(face[(i + 2) % 4], face[(i + 3) % 4]);
            }
        }
        return -1L;
    }

    private static List<Integer> adjacentFaces(MeshGeometry mesh, int a, int b) {
        List<Integer> result = new ArrayList<>();
        long key = edgeKey(a, b);
        for (int i = 0; i < mesh.faces().size(); i++) {
            int[] face = mesh.faces().get(i).vertices();
            for (int j = 0; j < face.length; j++) {
                if (edgeKey(face[j], face[(j + 1) % face.length]) == key) {
                    result.add(i);
                    break;
                }
            }
        }
        return result;
    }

    private static boolean containsEdge(MeshGeometry mesh, int a, int b) {
        return !adjacentFaces(mesh, a, b).isEmpty();
    }

    /** Returns the shortest topological vertex path between two vertices. */
    public static List<Integer> shortestVertexPath(MeshGeometry mesh, int start, int goal) {
        List<Integer> empty = List.of();
        if (mesh == null || start < 0 || goal < 0 || start >= mesh.vertices().size() || goal >= mesh.vertices().size()) return empty;
        if (start == goal) return List.of(start);

        Map<Integer, Integer> previous = new LinkedHashMap<>();
        Queue<Integer> queue = new ArrayDeque<>();
        Set<Integer> visited = new LinkedHashSet<>();
        queue.add(start);
        visited.add(start);

        while (!queue.isEmpty()) {
            int current = queue.remove();
            for (int next : vertexNeighbors(mesh, current)) {
                if (!visited.add(next)) continue;
                previous.put(next, current);
                if (next == goal) {
                    ArrayList<Integer> path = new ArrayList<>();
                    int cursor = goal;
                    while (cursor != start) {
                        path.add(cursor);
                        cursor = previous.get(cursor);
                    }
                    path.add(start);
                    java.util.Collections.reverse(path);
                    return path;
                }
                queue.add(next);
            }
        }
        return empty;
    }

    /** Returns all mesh edges belonging to the shortest vertex path. */
    public static Set<Long> shortestEdgePath(MeshGeometry mesh, int start, int goal) {
        List<Integer> path = shortestVertexPath(mesh, start, goal);
        Set<Long> result = new LinkedHashSet<>();
        for (int i = 0; i + 1 < path.size(); i++) result.add(edgeKey(path.get(i), path.get(i + 1)));
        return result;
    }

    /** Boundary edges are edges used by exactly one face. */
    public static Set<Long> boundaryEdges(MeshGeometry mesh) {
        Map<Long, Integer> counts = new LinkedHashMap<>();
        for (MeshGeometry.Face face : mesh.faces()) {
            int[] v = face.vertices();
            for (int i = 0; i < v.length; i++) {
                long key = edgeKey(v[i], v[(i + 1) % v.length]);
                counts.put(key, counts.getOrDefault(key, 0) + 1);
            }
        }
        Set<Long> result = new LinkedHashSet<>();
        for (Map.Entry<Long, Integer> entry : counts.entrySet()) {
            if (entry.getValue() == 1) result.add(entry.getKey());
        }
        return result;
    }

    public static Set<Integer> boundaryVertices(MeshGeometry mesh) {
        Set<Integer> result = new LinkedHashSet<>();
        for (long edge : boundaryEdges(mesh)) {
            result.add(edgeA(edge));
            result.add(edgeB(edge));
        }
        return result;
    }

    /** Returns vertices directly connected to a vertex by a mesh edge. */
    public static Set<Integer> vertexNeighbors(MeshGeometry mesh, int vertex) {
        Set<Integer> result = new LinkedHashSet<>();
        for (MeshGeometry.Face face : mesh.faces()) {
            int[] v = face.vertices();
            for (int i = 0; i < v.length; i++) {
                if (v[i] != vertex) continue;
                result.add(v[(i + v.length - 1) % v.length]);
                result.add(v[(i + 1) % v.length]);
            }
        }
        return result;
    }

    public static Set<Integer> selectedFacesForVertices(MeshGeometry mesh, Set<Integer> vertices) {
        Set<Integer> result = new LinkedHashSet<>();
        for (int i = 0; i < mesh.faces().size(); i++) {
            for (int v : mesh.faces().get(i).vertices()) {
                if (vertices.contains(v)) { result.add(i); break; }
            }
        }
        return result;
    }

    public static Set<Integer> selectedFacesForEdges(MeshGeometry mesh, Set<Long> edges) {
        Set<Integer> result = new LinkedHashSet<>();
        for (int i = 0; i < mesh.faces().size(); i++) {
            int[] v = mesh.faces().get(i).vertices();
            for (int j = 0; j < v.length; j++) {
                if (edges.contains(edgeKey(v[j], v[(j + 1) % v.length]))) {
                    result.add(i);
                    break;
                }
            }
        }
        return result;
    }

    private static boolean shareVertex(int[] a, int[] b) {
        for (int x : a) for (int y : b) if (x == y) return true;
        return false;
    }

    private static int edgeA(long key) { return (int)(key >>> 32); }
    private static int edgeB(long key) { return (int)key; }

    public static long edgeKey(int a, int b) {
        int lo = Math.min(a, b), hi = Math.max(a, b);
        return ((long)lo << 32) | (hi & 0xffffffffL);
    }
}
