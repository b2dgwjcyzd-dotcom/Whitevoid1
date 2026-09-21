package whitevoid.create.editor.geometry;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import whitevoid.create.model.Model;
import whitevoid.create.model.ModelNode;

/**
 * Mutable component-selection state for the CREATE mesh editor.
 *
 * <p>Topology algorithms, bulk selection, boundary selection, path selection,
 * and operation reconciliation live in focused controllers. This class owns
 * only selection state and the small primitives those controllers use to
 * mutate it.</p>
 */
public final class MeshComponentSelection {
    private UUID nodeId;
    private MeshSelectionMode mode = MeshSelectionMode.FACE;
    private final Set<Integer> vertices = new java.util.LinkedHashSet<>();
    private final Set<Long> edges = new java.util.LinkedHashSet<>();
    private final Set<Integer> faces = new java.util.LinkedHashSet<>();

    private int activeVertex = -1;
    private long activeEdge = -1L;
    private int activeFace = -1;

    public void selectVertex(ModelNode node, int vertexIndex) {
        setSingle(node, MeshSelectionMode.VERTEX);
        vertices.add(vertexIndex);
        activeVertex = vertexIndex;
    }

    public void selectEdge(ModelNode node, int a, int b) {
        setSingle(node, MeshSelectionMode.EDGE);
        long key = edgeKey(a, b);
        edges.add(key);
        activeEdge = key;
    }

    public void selectFace(ModelNode node, int faceIndex) {
        setSingle(node, MeshSelectionMode.FACE);
        faces.add(faceIndex);
        activeFace = faceIndex;
    }

    public void toggleVertex(ModelNode node, int index) {
        toggle(node, MeshSelectionMode.VERTEX, index, -1);
    }

    public void toggleEdge(ModelNode node, int a, int b) {
        toggle(node, MeshSelectionMode.EDGE, a, b);
    }

    public void toggleFace(ModelNode node, int index) {
        toggle(node, MeshSelectionMode.FACE, index, -1);
    }

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

    public void setMode(MeshSelectionMode mode) {
        if (mode != null) this.mode = mode;
        clearSelectionOnly();
    }

    public void clear() {
        nodeId = null;
        clearSelectionOnly();
    }

    public boolean containsVertex(int index) {
        return vertices.contains(index);
    }

    public boolean containsEdge(int a, int b) {
        return edges.contains(edgeKey(a, b));
    }

    public boolean containsFace(int index) {
        return faces.contains(index);
    }

    public MeshSelectionMode mode() {
        return mode;
    }

    public int activeVertex() {
        return activeVertex;
    }

    public long activeEdgeKey() {
        return activeEdge;
    }

    public int activeEdgeA() {
        return activeEdge < 0 ? -1 : (int) (activeEdge >>> 32);
    }

    public int activeEdgeB() {
        return activeEdge < 0 ? -1 : (int) activeEdge;
    }

    public int activeFace() {
        return activeFace;
    }

    public int indexA() {
        if (mode == MeshSelectionMode.VERTEX) return first(vertices);
        if (mode == MeshSelectionMode.EDGE) {
            long key = firstLong(edges);
            return key < 0 ? -1 : (int) (key >>> 32);
        }
        return first(faces);
    }

    public int indexB() {
        if (mode != MeshSelectionMode.EDGE) return -1;
        long key = firstLong(edges);
        return key < 0 ? -1 : (int) key;
    }

    public List<Integer> vertexIndices() {
        return List.copyOf(vertices);
    }

    public List<int[]> edgeIndices() {
        List<int[]> result = new java.util.ArrayList<>();
        for (long key : edges) {
            result.add(new int[]{(int) (key >>> 32), (int) key});
        }
        return result;
    }

    public List<Integer> faceIndices() {
        return List.copyOf(faces);
    }

    public int size() {
        return switch (mode) {
            case VERTEX -> vertices.size();
            case EDGE -> edges.size();
            case FACE -> faces.size();
        };
    }

    public ModelNode node(Model model) {
        if (nodeId == null || isEmpty()) return null;
        for (ModelNode node : model.allNodes()) {
            if (node.id().equals(nodeId)) return node;
        }
        clear();
        return null;
    }

    public boolean matches(ModelNode node) {
        return node != null && nodeId != null && nodeId.equals(node.id()) && !isEmpty();
    }

    public boolean matches(ModelNode node, MeshSelectionMode expected) {
        return matches(node) && mode == expected;
    }

    public void syncLegacyFaceSelection(ModelNode node, MeshFaceSelection legacy) {
        if (legacy == null) return;
        if (node == null || !matches(node, MeshSelectionMode.FACE) || activeFace < 0) {
            legacy.clear();
            return;
        }
        legacy.select(node, activeFace);
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
        if (node == null) {
            clear();
            return;
        }
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
            long key = edgeKey(a, b);
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

    private void clearSelectionOnly() {
        vertices.clear();
        edges.clear();
        faces.clear();
        activeVertex = -1;
        activeEdge = -1L;
        activeFace = -1;
    }

    private boolean isEmpty() {
        return vertices.isEmpty() && edges.isEmpty() && faces.isEmpty();
    }

    private static long edgeKey(int a, int b) {
        int lo = Math.min(a, b);
        int hi = Math.max(a, b);
        return ((long) lo << 32) | (hi & 0xffffffffL);
    }

    private static int first(Set<Integer> set) {
        return set.isEmpty() ? -1 : set.iterator().next();
    }

    private static long firstLong(Set<Long> set) {
        return set.isEmpty() ? -1L : set.iterator().next();
    }
}
