package whitevoid.create.editor.geometry;

import java.util.UUID;
import whitevoid.create.model.Model;
import whitevoid.create.model.ModelNode;

public final class MeshComponentSelection {
    private UUID nodeId;
    private MeshSelectionMode mode = MeshSelectionMode.FACE;
    private int indexA = -1;
    private int indexB = -1;

    public void selectVertex(ModelNode node, int vertexIndex) {
        select(node, MeshSelectionMode.VERTEX, vertexIndex, -1);
    }

    public void selectEdge(ModelNode node, int a, int b) {
        select(node, MeshSelectionMode.EDGE, a, b);
    }

    public void selectFace(ModelNode node, int faceIndex) {
        select(node, MeshSelectionMode.FACE, faceIndex, -1);
    }

    private void select(ModelNode node, MeshSelectionMode mode, int a, int b) {
        if (node == null || a < 0 || (mode == MeshSelectionMode.EDGE && b < 0)) {
            clear();
            return;
        }
        nodeId = node.id();
        this.mode = mode;
        indexA = a;
        indexB = b;
    }

    public void setMode(MeshSelectionMode mode) {
        if (mode != null) this.mode = mode;
        indexA = -1;
        indexB = -1;
    }

    public void clear() {
        nodeId = null;
        indexA = -1;
        indexB = -1;
    }

    public MeshSelectionMode mode() { return mode; }
    public int indexA() { return indexA; }
    public int indexB() { return indexB; }

    public ModelNode node(Model model) {
        if (nodeId == null || indexA < 0) return null;
        for (ModelNode node : model.allNodes()) {
            if (node.id().equals(nodeId)) return node;
        }
        clear();
        return null;
    }

    public boolean matches(ModelNode node) {
        return node != null && nodeId != null && nodeId.equals(node.id()) && indexA >= 0;
    }
}
