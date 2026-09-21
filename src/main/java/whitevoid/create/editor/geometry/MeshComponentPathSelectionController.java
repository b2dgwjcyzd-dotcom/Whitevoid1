package whitevoid.create.editor.geometry;

import java.util.List;
import whitevoid.create.model.MeshGeometry;
import whitevoid.create.model.ModelNode;

/**
 * Focused shortest-path selection operations for CREATE component selection.
 */
public final class MeshComponentPathSelectionController {
    public void selectShortestVertexPath(MeshComponentSelection selection, ModelNode node, int start, int goal) {
        if (selection == null || node == null) return;
        MeshGeometry mesh = node.ensureMeshGeometry();
        if (mesh == null) return;

        selection.setMode(MeshSelectionMode.VERTEX);
        List<Integer> path = MeshTopologySelection.shortestVertexPath(mesh, start, goal);
        for (int index : path) {
            selection.addVertex(node, index);
        }
        if (!path.isEmpty()) {
            selection.selectVertex(node, goal);
        }
    }

    public void selectShortestEdgePath(MeshComponentSelection selection, ModelNode node, int start, int goal) {
        if (selection == null || node == null) return;
        MeshGeometry mesh = node.ensureMeshGeometry();
        if (mesh == null) return;

        selection.setMode(MeshSelectionMode.EDGE);
        var path = MeshTopologySelection.shortestEdgePath(mesh, start, goal);
        for (long edge : path) {
            selection.addEdge(node, (int)(edge >>> 32), (int)edge);
        }

        if (path.isEmpty()) {
            List<Integer> vertices = MeshTopologySelection.shortestVertexPath(mesh, start, goal);
            if (vertices.size() >= 2) {
                long edge = MeshTopologySelection.edgeKey(
                        vertices.get(vertices.size() - 2), goal);
                selection.addEdge(node, (int)(edge >>> 32), (int)edge);
            }
        }
    }

    public void selectShortestPathBetweenActiveAnd(
            MeshComponentSelection selection,
            ModelNode node,
            int targetIndex
    ) {
        if (selection == null || node == null || !selection.matches(node)) return;

        if (selection.mode() == MeshSelectionMode.VERTEX && selection.activeVertex() >= 0) {
            selectShortestVertexPath(selection, node, selection.activeVertex(), targetIndex);
        } else if (selection.mode() == MeshSelectionMode.EDGE && selection.activeEdgeA() >= 0) {
            selectShortestEdgePath(selection, node, selection.activeEdgeA(), targetIndex);
        }
    }
}
