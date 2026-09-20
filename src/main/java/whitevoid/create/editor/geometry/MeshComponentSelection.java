package whitevoid.create.editor.geometry;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import whitevoid.create.model.Model;
import whitevoid.create.model.ModelNode;\nimport whitevoid.create.model.MeshGeometry;

public final class MeshComponentSelection {
    private UUID nodeId;
    private MeshSelectionMode mode = MeshSelectionMode.FACE;
    private final Set<Integer> vertices = new LinkedHashSet<>();
    private final Set<Long> edges = new LinkedHashSet<>();
    private final Set<Integer> faces = new LinkedHashSet<>();

    private int activeVertex = -1;
    private long activeEdge = -1L;
    private int activeFace = -1;

    public void selectVertex(ModelNode node, int vertexIndex) { setSingle(node, MeshSelectionMode.VERTEX); vertices.add(vertexIndex); activeVertex = vertexIndex; }
    public void selectEdge(ModelNode node, int a, int b) { setSingle(node, MeshSelectionMode.EDGE); long key=edgeKey(a,b); edges.add(key); activeEdge=key; }
    public void selectFace(ModelNode node, int faceIndex) { setSingle(node, MeshSelectionMode.FACE); faces.add(faceIndex); activeFace=faceIndex; }

    public void toggleVertex(ModelNode node, int index) { toggle(node, MeshSelectionMode.VERTEX, index, -1); }
    public void toggleEdge(ModelNode node, int a, int b) { toggle(node, MeshSelectionMode.EDGE, a, b); }
    public void toggleFace(ModelNode node, int index) { toggle(node, MeshSelectionMode.FACE, index, -1); }

    /** Adds a component without removing or toggling existing selection. */
    public void addVertex(ModelNode node, int index) {
        prepareForMultiSelect(node, MeshSelectionMode.VERTEX);
        vertices.add(index);
        activeVertex = index;
    }

    /** Adds an edge without removing or toggling existing selection. */
    public void addEdge(ModelNode node, int a, int b) {
        prepareForMultiSelect(node, MeshSelectionMode.EDGE);
        long key = edgeKey(a, b);
        edges.add(key);
        activeEdge = key;
    }

    /** Adds a face without removing or toggling existing selection. */
    public void addFace(ModelNode node, int index) {
        prepareForMultiSelect(node, MeshSelectionMode.FACE);
        faces.add(index);
        activeFace = index;
    }

    public void removeVertex(ModelNode node, int index) {
        if (!matchesMode(node, MeshSelectionMode.VERTEX)) return;
        vertices.remove(index);
        if (activeVertex == index) activeVertex = first(vertices);
        clearIfEmpty();
    }

    public void removeEdge(ModelNode node, int a, int b) {
        if (!matchesMode(node, MeshSelectionMode.EDGE)) return;
        long key = edgeKey(a, b);
        edges.remove(key);
        if (activeEdge == key) activeEdge = firstLong(edges);
        clearIfEmpty();
    }

    public void removeFace(ModelNode node, int index) {
        if (!matchesMode(node, MeshSelectionMode.FACE)) return;
        faces.remove(index);
        if (activeFace == index) activeFace = first(faces);
        clearIfEmpty();
    }

    private void prepareForMultiSelect(ModelNode node, MeshSelectionMode newMode) {
        if (node == null) return;
        if (nodeId == null || !nodeId.equals(node.id()) || mode != newMode) {
            clearSelectionOnly();
            nodeId = node.id();
            mode = newMode;
        }
    }

    private boolean matchesMode(ModelNode node, MeshSelectionMode expected) {
        return node != null && nodeId != null && nodeId.equals(node.id()) && mode == expected;
    }

    private void clearIfEmpty() {
        if (isEmpty()) nodeId = null;
    }

    private void setSingle(ModelNode node, MeshSelectionMode newMode) {
        clearSelectionOnly();
        if (node == null) return;
        nodeId = node.id();
        mode = newMode;
    }

    private void toggle(ModelNode node, MeshSelectionMode newMode, int a, int b) {
        if (node == null) { clear(); return; }
        prepareForMultiSelect(node, newMode);
        if (newMode == MeshSelectionMode.VERTEX) {
            if (vertices.contains(a)) {
                vertices.remove(a);
                if (activeVertex == a) activeVertex = first(vertices);
            } else {
                vertices.add(a);
                activeVertex = a;
            }
        } else if (newMode == MeshSelectionMode.EDGE) {
            long key=edgeKey(a,b);
            if (edges.contains(key)) {
                edges.remove(key);
                if (activeEdge == key) activeEdge = firstLong(edges);
            } else {
                edges.add(key);
                activeEdge = key;
            }
        } else {
            if (faces.contains(a)) {
                faces.remove(a);
                if (activeFace == a) activeFace = first(faces);
            } else {
                faces.add(a);
                activeFace = a;
            }
        }
        if (isEmpty()) nodeId = null;
    }

    public void setMode(MeshSelectionMode mode) {
        if (mode != null) this.mode = mode;
        clearSelectionOnly();
    }

    public void clear() { nodeId = null; clearSelectionOnly(); }
    private void clearSelectionOnly() {
        vertices.clear(); edges.clear(); faces.clear();
        activeVertex = -1; activeEdge = -1L; activeFace = -1;
    }

    public int activeVertex() { return activeVertex; }
    public long activeEdgeKey() { return activeEdge; }
    public int activeEdgeA() { return activeEdge < 0 ? -1 : (int)(activeEdge >>> 32); }
    public int activeEdgeB() { return activeEdge < 0 ? -1 : (int)activeEdge; }
    public int activeFace() { return activeFace; }

    public MeshSelectionMode mode() { return mode; }
    public int indexA() {
        if (mode == MeshSelectionMode.VERTEX) return first(vertices);
        if (mode == MeshSelectionMode.EDGE) { long k=firstLong(edges); return k < 0 ? -1 : (int)(k >>> 32); }
        return first(faces);
    }
    public int indexB() {
        if (mode != MeshSelectionMode.EDGE) return -1;
        long k=firstLong(edges); return k < 0 ? -1 : (int)k;
    }

    public List<Integer> vertexIndices() { return List.copyOf(vertices); }
    public List<int[]> edgeIndices() {
        List<int[]> out=new ArrayList<>();
        for(long k:edges) out.add(new int[]{(int)(k>>>32),(int)k});
        return out;
    }
    public List<Integer> faceIndices() { return List.copyOf(faces); }
    public int size() {
        return mode == MeshSelectionMode.VERTEX ? vertices.size()
                : mode == MeshSelectionMode.EDGE ? edges.size() : faces.size();
    }

    /** Selects every component of the current mode on the given mesh. */
    public void selectAll(ModelNode node) {
        if (node == null) return;
        var mesh = node.ensureMeshGeometry();
        if (mesh == null) return;
        clearSelectionOnly();
        nodeId = node.id();
        if (mode == MeshSelectionMode.VERTEX) {
            for (int i = 0; i < mesh.vertices().size(); i++) vertices.add(i);
            activeVertex = first(vertices);
        } else if (mode == MeshSelectionMode.EDGE) {
            for (int[] edge : whitevoid.create.model.ModelRenderer.meshEdges(mesh)) {
                edges.add(edgeKey(edge[0], edge[1]));
            }
            activeEdge = firstLong(edges);
        } else {
            for (int i = 0; i < mesh.faces().size(); i++) faces.add(i);
            activeFace = first(faces);
        }
    }

    /** Inverts the current component selection against the node's topology. */
    public void invert(ModelNode node) {
        if (node == null) return;
        var mesh = node.ensureMeshGeometry();
        if (mesh == null) return;
        nodeId = node.id();
        if (mode == MeshSelectionMode.VERTEX) {
            Set<Integer> next = new LinkedHashSet<>();
            for (int i = 0; i < mesh.vertices().size(); i++) if (!vertices.contains(i)) next.add(i);
            vertices.clear(); vertices.addAll(next); activeVertex = first(vertices);
        } else if (mode == MeshSelectionMode.EDGE) {
            Set<Long> next = new LinkedHashSet<>();
            for (int[] edge : whitevoid.create.model.ModelRenderer.meshEdges(mesh)) {
                long key = edgeKey(edge[0], edge[1]);
                if (!edges.contains(key)) next.add(key);
            }
            edges.clear(); edges.addAll(next); activeEdge = firstLong(edges);
        } else {
            Set<Integer> next = new LinkedHashSet<>();
            for (int i = 0; i < mesh.faces().size(); i++) if (!faces.contains(i)) next.add(i);
            faces.clear(); faces.addAll(next); activeFace = first(faces);
        }
        if (isEmpty()) nodeId = null;
    }
    public void selectEdgeLoop(ModelNode node, int a, int b) {
        if (node == null) return;
        prepareForMultiSelect(node, MeshSelectionMode.EDGE);
        edges.clear();
        edges.addAll(MeshTopologySelection.edgeLoop(node.ensureMeshGeometry(), a, b));
        activeEdge = edgeKey(a,b);
    }

    public void selectFaceLoop(ModelNode node, int faceIndex) {
        if (node == null) return;
        MeshGeometry mesh = node.ensureMeshGeometry();
        if (mesh == null) return;
        prepareForMultiSelect(node, MeshSelectionMode.FACE);
        faces.clear();
        faces.addAll(MeshTopologySelection.faceLoop(mesh, faceIndex));
        activeFace = faceIndex;
    }

    public void selectEdgeRing(ModelNode node, int a, int b) {
        if (node == null) return;
        MeshGeometry mesh = node.ensureMeshGeometry();
        if (mesh == null) return;
        prepareForMultiSelect(node, MeshSelectionMode.EDGE);
        edges.clear();
        edges.addAll(MeshTopologySelection.edgeRing(mesh, a, b));
        activeEdge = MeshTopologySelection.edgeKey(a, b);
    }

    public void selectFaceRing(ModelNode node, int faceIndex) {
        if (node == null) return;
        MeshGeometry mesh = node.ensureMeshGeometry();
        if (mesh == null) return;
        prepareForMultiSelect(node, MeshSelectionMode.FACE);
        faces.clear();
        faces.addAll(MeshTopologySelection.faceRing(mesh, faceIndex));
        activeFace = faceIndex;
    }

    public void selectLinked(ModelNode node) {
        if (node == null || !matches(node)) return;
        var mesh = node.ensureMeshGeometry();
        if (mesh == null) return;
        if (mode == MeshSelectionMode.VERTEX) {
            Set<Integer> seeds = new LinkedHashSet<>(vertices);
            vertices.clear();
            vertices.addAll(MeshTopologySelection.linkedVertices(mesh, seeds));
            activeVertex = first(vertices);
        } else if (mode == MeshSelectionMode.EDGE) {
            Set<Long> seeds = new LinkedHashSet<>(edges);
            edges.clear();
            edges.addAll(MeshTopologySelection.linkedEdges(mesh, seeds));
            activeEdge = firstLong(edges);
        } else {
            Set<Integer> seeds = new LinkedHashSet<>(faces);
            faces.clear();
            faces.addAll(MeshTopologySelection.linkedFaces(mesh, seeds));
            activeFace = first(faces);
        }
    }

    public boolean containsVertex(int i) { return vertices.contains(i); }
    public boolean containsEdge(int a,int b) { return edges.contains(edgeKey(a,b)); }
    public boolean containsFace(int i) { return faces.contains(i); }

    public ModelNode node(Model model) {
        if (nodeId == null || isEmpty()) return null;
        for (ModelNode node : model.allNodes()) if (node.id().equals(nodeId)) return node;
        clear(); return null;
    }
    public boolean matches(ModelNode node) { return node != null && nodeId != null && nodeId.equals(node.id()) && !isEmpty(); }
    private boolean isEmpty(){ return vertices.isEmpty() && edges.isEmpty() && faces.isEmpty(); }
    private static long edgeKey(int a,int b){ int lo=Math.min(a,b),hi=Math.max(a,b); return ((long)lo<<32)|(hi&0xffffffffL); }
    private static int first(Set<Integer> set){ return set.isEmpty()?-1:set.iterator().next(); }
    private static long firstLong(Set<Long> set){ return set.isEmpty()?-1L:set.iterator().next(); }
}
