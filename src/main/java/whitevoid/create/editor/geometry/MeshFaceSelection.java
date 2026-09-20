package whitevoid.create.editor.geometry;

import java.util.UUID;
import whitevoid.create.model.Model;
import whitevoid.create.model.ModelNode;

public final class MeshFaceSelection {
    private UUID nodeId;
    private int faceIndex = -1;

    public void select(ModelNode node, int faceIndex) {
        if (node == null || faceIndex < 0) {
            clear();
            return;
        }
        this.nodeId = node.id();
        this.faceIndex = faceIndex;
    }

    public void clear() {
        nodeId = null;
        faceIndex = -1;
    }

    public int faceIndex() {
        return faceIndex;
    }

    public ModelNode node(Model model) {
        if (nodeId == null || faceIndex < 0) return null;
        for (ModelNode node : model.allNodes()) {
            if (node.id().equals(nodeId)) return node;
        }
        clear();
        return null;
    }

    public boolean matches(ModelNode node) {
        return node != null && nodeId != null
                && nodeId.equals(node.id()) && faceIndex >= 0;
    }
}
