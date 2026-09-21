package whitevoid.create.editor.geometry;

import java.util.List;
import whitevoid.create.model.MeshGeometry;
import whitevoid.create.model.ModelNode;

/**
 * Focused topology-selection coordinator for CREATE component selection.
 * Keeps topology traversal separate from the selection state container.
 */
public final class MeshComponentTopologySelectionController {
    public void selectEdgeLoop(MeshComponentSelection selection, ModelNode node, int a, int b) {
        if (selection == null || node == null) return;
        selection.setMode(MeshSelectionMode.EDGE);
        for (long edge : MeshTopologySelection.edgeLoop(node.ensureMeshGeometry(), a, b)) {
            selection.addEdge(node, (int) (edge >>> 32), (int) edge);
        }
    }

    public void selectFaceLoop(MeshComponentSelection selection, ModelNode node, int faceIndex) {
        if (selection == null || node == null) return;
        MeshGeometry mesh = node.ensureMeshGeometry();
        if (mesh == null) return;
        selection.setMode(MeshSelectionMode.FACE);
        for (int index : MeshTopologySelection.faceLoop(mesh, faceIndex)) {
            selection.addFace(node, index);
        }
    }

    public void selectEdgeRing(MeshComponentSelection selection, ModelNode node, int a, int b) {
        if (selection == null || node == null) return;
        MeshGeometry mesh = node.ensureMeshGeometry();
        if (mesh == null) return;
        selection.setMode(MeshSelectionMode.EDGE);
        for (long edge : MeshTopologySelection.edgeRing(mesh, a, b)) {
            selection.addEdge(node, (int) (edge >>> 32), (int) edge);
        }
    }

    public void selectFaceRing(MeshComponentSelection selection, ModelNode node, int faceIndex) {
        if (selection == null || node == null) return;
        MeshGeometry mesh = node.ensureMeshGeometry();
        if (mesh == null) return;
        selection.setMode(MeshSelectionMode.FACE);
        for (int index : MeshTopologySelection.faceRing(mesh, faceIndex)) {
            selection.addFace(node, index);
        }
    }

    public void selectShortestVertexPath(MeshComponentSelection selection, ModelNode node, int start, int goal) {
        if (selection == null || node == null) return;
        MeshGeometry mesh = node.ensureMeshGeometry();
        if (mesh == null) return;
        selection.setMode(MeshSelectionMode.VERTEX);
        List<Integer> path = MeshTopologySelection.shortestVertexPath(mesh, start, goal);
        for (int index : path) selection.addVertex(node, index);
    }

    public void selectShortestEdgePath(MeshComponentSelection selection, ModelNode node, int start, int goal) {
        if (selection == null || node == null) return;
        MeshGeometry mesh = node.ensureMeshGeometry();
        if (mesh == null) return;
        selection.setMode(MeshSelectionMode.EDGE);
        java.util.Set<Long> path = MeshTopologySelection.shortestEdgePath(mesh, start, goal);
        for (long edge : path) {
            selection.addEdge(node, MeshTopologySelection.edgeA(edge), MeshTopologySelection.edgeB(edge));
        }
        if (path.isEmpty()) {
            List<Integer> vertices = MeshTopologySelection.shortestVertexPath(mesh, start, goal);
            if (vertices.size() >= 2) {
                long edge = MeshTopologySelection.edgeKey(vertices.get(vertices.size() - 2), goal);
                selection.addEdge(node, (int) (edge >>> 32), (int) edge);
            }
        }
    }
}
