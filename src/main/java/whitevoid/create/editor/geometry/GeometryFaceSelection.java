package whitevoid.create.editor.geometry;

import java.util.UUID;
import whitevoid.create.model.Model;
import whitevoid.create.model.ModelNode;

public final class GeometryFaceSelection {
    private UUID nodeId;
    private GeometryFace face = GeometryFace.NONE;

    public void select(ModelNode node, GeometryFace face) {
        if (node == null || face == null || face == GeometryFace.NONE) {
            clear();
            return;
        }
        this.nodeId = node.id();
        this.face = face;
    }

    public void clear() {
        nodeId = null;
        face = GeometryFace.NONE;
    }

    public GeometryFace face() {
        return face;
    }

    public ModelNode node(Model model) {
        if (nodeId == null || face == GeometryFace.NONE) return null;
        for (ModelNode node : model.allNodes()) {
            if (node.id().equals(nodeId)) return node;
        }
        clear();
        return null;
    }

    public boolean matches(ModelNode node) {
        return node != null && nodeId != null && node.id().equals(nodeId) && face != GeometryFace.NONE;
    }
}
