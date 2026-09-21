package whitevoid.create.editor.geometry;

import java.util.LinkedHashSet;
import java.util.Set;
import whitevoid.create.model.MeshGeometry;
import whitevoid.create.model.ModelNode;
import whitevoid.create.model.ModelRenderer;

/**
 * Focused bulk-selection operations for CREATE component selection.
 */
public final class MeshComponentBulkSelectionController {
    public void selectAll(MeshComponentSelection selection, ModelNode node) {
        if (selection == null || node == null) return;
        MeshGeometry mesh = node.ensureMeshGeometry();
        if (mesh == null) return;
        selection.clear();
        selection.setMode(selection.mode());
        for (int i = 0; i < mesh.vertices().size() && selection.mode() == MeshSelectionMode.VERTEX; i++) {
            selection.addVertex(node, i);
        }
        if (selection.mode() == MeshSelectionMode.EDGE) {
            for (int[] edge : ModelRenderer.meshEdges(mesh)) {
                selection.addEdge(node, edge[0], edge[1]);
            }
        } else if (selection.mode() == MeshSelectionMode.FACE) {
            for (int i = 0; i < mesh.faces().size(); i++) selection.addFace(node, i);
        }
    }

    public void invert(MeshComponentSelection selection, ModelNode node) {
        if (selection == null || node == null) return;
        MeshGeometry mesh = node.ensureMeshGeometry();
        if (mesh == null) return;

        if (selection.mode() == MeshSelectionMode.VERTEX) {
            Set<Integer> current = new LinkedHashSet<>(selection.vertexIndices());
            selection.clear();
            selection.setMode(MeshSelectionMode.VERTEX);
            for (int i = 0; i < mesh.vertices().size(); i++) {
                if (!current.contains(i)) selection.addVertex(node, i);
            }
        } else if (selection.mode() == MeshSelectionMode.EDGE) {
            Set<Long> current = new LinkedHashSet<>();
            for (int[] edge : selection.edgeIndices()) {
                current.add(MeshTopologySelection.edgeKey(edge[0], edge[1]));
            }
            selection.clear();
            selection.setMode(MeshSelectionMode.EDGE);
            for (int[] edge : ModelRenderer.meshEdges(mesh)) {
                long key = MeshTopologySelection.edgeKey(edge[0], edge[1]);
                if (!current.contains(key)) selection.addEdge(node, edge[0], edge[1]);
            }
        } else {
            Set<Integer> current = new LinkedHashSet<>(selection.faceIndices());
            selection.clear();
            selection.setMode(MeshSelectionMode.FACE);
            for (int i = 0; i < mesh.faces().size(); i++) {
                if (!current.contains(i)) selection.addFace(node, i);
            }
        }
    }
}
