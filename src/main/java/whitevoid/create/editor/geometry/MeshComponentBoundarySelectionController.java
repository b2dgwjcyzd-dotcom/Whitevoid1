package whitevoid.create.editor.geometry;

import whitevoid.create.model.MeshGeometry;
import whitevoid.create.model.ModelNode;

/**
 * Focused boundary-selection operations for CREATE component selection.
 */
public final class MeshComponentBoundarySelectionController {
    public void selectBoundary(MeshComponentSelection selection, ModelNode node) {
        if (selection == null || node == null) return;
        MeshGeometry mesh = node.ensureMeshGeometry();
        if (mesh == null) return;

        selection.clear();
        if (selection.mode() == MeshSelectionMode.VERTEX) {
            selection.setMode(MeshSelectionMode.VERTEX);
            for (int vertex : MeshTopologySelection.boundaryVertices(mesh)) {
                selection.addVertex(node, vertex);
            }
        } else if (selection.mode() == MeshSelectionMode.EDGE) {
            selection.setMode(MeshSelectionMode.EDGE);
            for (long edge : MeshTopologySelection.boundaryEdges(mesh)) {
                selection.addEdge(node, (int)(edge >>> 32), (int)edge);
            }
        } else {
            selection.setMode(MeshSelectionMode.FACE);
            for (int i = 0; i < mesh.faces().size(); i++) {
                int[] face = mesh.faces().get(i).vertices();
                if (containsBoundaryEdge(mesh, face)) {
                    selection.addFace(node, i);
                }
            }
        }
    }

    public void selectBoundaryLoop(MeshComponentSelection selection, ModelNode node) {
        if (selection == null || node == null) return;
        MeshGeometry mesh = node.ensureMeshGeometry();
        if (mesh == null) return;

        if (selection.mode() == MeshSelectionMode.VERTEX) {
            selection.clear();
            selection.setMode(MeshSelectionMode.VERTEX);
            for (int vertex : MeshTopologySelection.boundaryVertices(mesh)) {
                selection.addVertex(node, vertex);
            }
        } else if (selection.mode() == MeshSelectionMode.EDGE) {
            selection.clear();
            selection.setMode(MeshSelectionMode.EDGE);
            for (long edge : MeshTopologySelection.boundaryEdges(mesh)) {
                selection.addEdge(node, (int)(edge >>> 32), (int)edge);
            }
        }
    }

    private boolean containsBoundaryEdge(MeshGeometry mesh, int[] face) {
        for (int i = 0; i < face.length; i++) {
            long edge = MeshTopologySelection.edgeKey(face[i], face[(i + 1) % face.length]);
            for (long boundary : MeshTopologySelection.boundaryEdges(mesh)) {
                if (boundary == edge) return true;
            }
        }
        return false;
    }
}
