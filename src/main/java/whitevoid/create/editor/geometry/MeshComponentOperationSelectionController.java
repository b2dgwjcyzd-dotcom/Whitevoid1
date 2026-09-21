package whitevoid.create.editor.geometry;

import whitevoid.create.model.ModelNode;

/**
 * Focused selection-state reconciliation for topology-changing CREATE operations.
 */
public final class MeshComponentOperationSelectionController {
    public void remap(MeshComponentSelection selection, ModelNode node, MeshOperations.OperationResult result) {
        if (selection == null || node == null || result == null) {
            if (selection != null) selection.clear();
            return;
        }

        MeshSelectionMode mode = selection.mode();
        var vertices = selection.vertexIndices();
        var edges = selection.edgeIndices();
        var faces = selection.faceIndices();
        int activeVertex = selection.activeVertex();
        long activeEdge = selection.activeEdgeKey();
        int activeFace = selection.activeFace();

        selection.clear();
        selection.setMode(mode);

        if (mode == MeshSelectionMode.VERTEX) {
            for (int index : vertices) {
                Integer mapped = result.vertexMapping().get(index);
                if (mapped != null && mapped >= 0 && mapped < result.mesh().vertices().size()) {
                    selection.addVertex(node, mapped);
                }
            }
            Integer mapped = result.vertexMapping().get(activeVertex);
            if (mapped != null && selection.containsVertex(mapped)) {
                selection.selectVertex(node, mapped);
                for (int index : vertices) {
                    Integer next = result.vertexMapping().get(index);
                    if (next != null && next != mapped) selection.addVertex(node, next);
                }
            }
        } else if (mode == MeshSelectionMode.EDGE) {
            for (int[] edge : edges) {
                Integer a = result.vertexMapping().get(edge[0]);
                Integer b = result.vertexMapping().get(edge[1]);
                if (a != null && b != null && a != b
                        && a >= 0 && b >= 0
                        && Math.max(a, b) < result.mesh().vertices().size()) {
                    selection.addEdge(node, a, b);
                }
            }
            int oldA = activeEdge < 0 ? -1 : (int)(activeEdge >>> 32);
            int oldB = activeEdge < 0 ? -1 : (int)activeEdge;
            Integer a = result.vertexMapping().get(oldA);
            Integer b = result.vertexMapping().get(oldB);
            if (a != null && b != null && selection.containsEdge(a, b)) {
                selection.selectEdge(node, a, b);
                for (int[] edge : edges) {
                    Integer ea = result.vertexMapping().get(edge[0]);
                    Integer eb = result.vertexMapping().get(edge[1]);
                    if (ea != null && eb != null && ea != a && eb != b) selection.addEdge(node, ea, eb);
                }
            }
        } else {
            for (int index : faces) {
                Integer mapped = result.faceMapping().get(index);
                if (mapped != null && mapped >= 0 && mapped < result.mesh().faces().size()) {
                    selection.addFace(node, mapped);
                }
            }
            Integer mapped = result.faceMapping().get(activeFace);
            if (mapped != null && selection.containsFace(mapped)) {
                selection.selectFace(node, mapped);
            }
        }
    }

    public void applyOperation(
            MeshComponentSelection selection,
            ModelNode node,
            MeshOperations.OperationResult result,
            MeshComponentSelection.RemapPolicy policy
    ) {
        if (selection == null || node == null || result == null) {
            if (selection != null) selection.clear();
            return;
        }
        if (policy == MeshComponentSelection.RemapPolicy.CREATED) {
            MeshSelectionMode mode = selection.mode();
            selection.clear();
            selection.setMode(mode);
            if (mode == MeshSelectionMode.VERTEX) {
                for (int index : result.createdVertices()) selection.addVertex(node, index);
            } else if (mode == MeshSelectionMode.EDGE) {
                for (long edge : result.createdEdges()) {
                    int a = (int)(edge >>> 32);
                    int b = (int)edge;
                    if (a >= 0 && b >= 0 && Math.max(a, b) < result.mesh().vertices().size()) {
                        selection.addEdge(node, a, b);
                    }
                }
            } else {
                for (int index : result.createdFaces()) selection.addFace(node, index);
            }
            return;
        }
        remap(selection, node, result);
    }

    public void applySelectionHint(
            MeshComponentSelection selection,
            ModelNode node,
            MeshOperations.OperationResult result
    ) {
        if (selection == null || node == null || result == null || result.selectionHint() == null) {
            if (selection != null) selection.clear();
            return;
        }
        MeshOperations.SelectionHint hint = result.selectionHint();
        selection.clear();

        if (!hint.vertices().isEmpty()) {
            selection.setMode(MeshSelectionMode.VERTEX);
            for (int index : hint.vertices()) {
                if (index >= 0 && index < result.mesh().vertices().size()) selection.addVertex(node, index);
            }
            if (hint.activeVertex() >= 0 && selection.containsVertex(hint.activeVertex())) {
                selection.selectVertex(node, hint.activeVertex());
            }
        } else if (!hint.edges().isEmpty()) {
            selection.setMode(MeshSelectionMode.EDGE);
            for (long key : hint.edges()) {
                int a = (int)(key >>> 32);
                int b = (int)key;
                if (a >= 0 && b >= 0 && Math.max(a, b) < result.mesh().vertices().size()) {
                    selection.addEdge(node, a, b);
                }
            }
            if (hint.activeEdge() >= 0) {
                int a = (int)(hint.activeEdge() >>> 32);
                int b = (int)hint.activeEdge();
                if (selection.containsEdge(a, b)) selection.selectEdge(node, a, b);
            }
        } else if (!hint.faces().isEmpty()) {
            selection.setMode(MeshSelectionMode.FACE);
            for (int index : hint.faces()) {
                if (index >= 0 && index < result.mesh().faces().size()) selection.addFace(node, index);
            }
            if (hint.activeFace() >= 0 && selection.containsFace(hint.activeFace())) {
                selection.selectFace(node, hint.activeFace());
            }
        }
    }
}
