package whitevoid.create.editor.geometry;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import whitevoid.create.model.Model;
import whitevoid.create.model.ModelNode;

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

    public void addVertex(ModelNode node, int index) {
        prepareForMultiSelect(node, MeshSelectionMode.VERTEX);
        vertices.add(index);
        activeVertex = index;
    }

    public void addEdge(ModelNode node, int a, int b) {
        prepareForMultiSelect(node, MeshSelectionMode.EDGE);
        long key = edgeKey(a, b);
        edges.add(key);
        activeEdge = key;
    }

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

    private final MeshComponentOperationSelectionController operationSelection =
            new MeshComponentOperationSelectionController();

    public enum RemapPolicy {
        PRESERVE,
        CREATED
    }

    public void remap(ModelNode node, MeshOperations.OperationResult result) {
        operationSelection.remap(this, node, result);
    }

    public void applyOperation(ModelNode node, MeshOperations.OperationResult result,
                               RemapPolicy policy) {
        operationSelection.applyOperation(this, node, result, policy);
    }

    public void applySelectionHint(ModelNode node, MeshOperations.OperationResult result) {
        operationSelection.applySelectionHint(this, node, result);
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

    private final MeshComponentBulkSelectionController bulkSelection =
            new MeshComponentBulkSelectionController();

    public void selectAll(ModelNode node) {
        bulkSelection.selectAll(this, node);
    }

    public void invert(ModelNode node) {
        bulkSelection.invert(this, node);
    }
    private final MeshComponentTopologySelectionController topologySelection =
            new MeshComponentTopologySelectionController();

    public void selectEdgeLoop(ModelNode node, int a, int b) {
        topologySelection.selectEdgeLoop(this, node, a, b);
    }

    public void selectFaceLoop(ModelNode node, int faceIndex) {
        topologySelection.selectFaceLoop(this, node, faceIndex);
    }

    public void selectEdgeRing(ModelNode node, int a, int b) {
        topologySelection.selectEdgeRing(this, node, a, b);
    }

    public void selectFaceRing(ModelNode node, int faceIndex) {
        topologySelection.selectFaceRing(this, node, faceIndex);
    }

    private final MeshComponentPathSelectionController pathSelection =
            new MeshComponentPathSelectionController();

    public void selectShortestVertexPath(ModelNode node, int start, int goal) {
        pathSelection.selectShortestVertexPath(this, node, start, goal);
    }

    public void selectShortestEdgePath(ModelNode node, int start, int goal) {
        pathSelection.selectShortestEdgePath(this, node, start, goal);
    }

    private final MeshComponentSelectionExpansionController expansionSelection =
            new MeshComponentSelectionExpansionController();

    private final MeshComponentBoundarySelectionController boundarySelection =
            new MeshComponentBoundarySelectionController();

    public void selectBoundary(ModelNode node) {
        boundarySelection.selectBoundary(this, node);
    }

    public void selectShortestPathBetweenActiveAnd(ModelNode node, int targetIndex) {
        pathSelection.selectShortestPathBetweenActiveAnd(this, node, targetIndex);
    }

    public void selectBoundaryLoop(ModelNode node) {
        boundarySelection.selectBoundaryLoop(this, node);
    }

    public void selectLinked(ModelNode node) {
        expansionSelection.selectLinked(this, node);
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
