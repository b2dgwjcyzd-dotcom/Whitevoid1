package whitevoid.create.editor.geometry;

import java.util.LinkedHashSet;
import java.util.Set;
import whitevoid.create.model.MeshGeometry;
import whitevoid.create.model.ModelNode;

/**
 * Focused topology expansion/contraction operations for CREATE selection.
 */
public final class MeshComponentSelectionExpansionController {
    public void extend(MeshComponentSelection selection, ModelNode node) {
        if (selection == null || node == null || !selection.matches(node)) return;
        MeshGeometry mesh = node.ensureMeshGeometry();
        if (mesh == null) return;

        if (selection.mode() == MeshSelectionMode.VERTEX) {
            Set<Integer> current = new LinkedHashSet<>(selection.vertexIndices());
            selection.clear();
            selection.setMode(MeshSelectionMode.VERTEX);
            for (int index : MeshTopologySelection.growVertices(mesh, current)) {
                selection.addVertex(node, index);
            }
        } else if (selection.mode() == MeshSelectionMode.EDGE) {
            Set<Long> current = new LinkedHashSet<>();
            for (int[] edge : selection.edgeIndices()) {
                current.add(MeshTopologySelection.edgeKey(edge[0], edge[1]));
            }
            selection.clear();
            selection.setMode(MeshSelectionMode.EDGE);
            for (long edge : MeshTopologySelection.growEdges(mesh, current)) {
                selection.addEdge(node, (int)(edge >>> 32), (int)edge);
            }
        } else {
            Set<Integer> current = new LinkedHashSet<>(selection.faceIndices());
            selection.clear();
            selection.setMode(MeshSelectionMode.FACE);
            for (int index : MeshTopologySelection.growFaces(mesh, current)) {
                selection.addFace(node, index);
            }
        }
    }

    public void shrink(MeshComponentSelection selection, ModelNode node) {
        if (selection == null || node == null || !selection.matches(node)) return;
        MeshGeometry mesh = node.ensureMeshGeometry();
        if (mesh == null || selection.size() == 0) return;

        if (selection.mode() == MeshSelectionMode.VERTEX) {
            Set<Integer> current = new LinkedHashSet<>(selection.vertexIndices());
            Set<Integer> next = new LinkedHashSet<>();
            for (int vertex : current) {
                boolean boundary = false;
                for (int neighbor : MeshTopologySelection.vertexNeighbors(mesh, vertex)) {
                    if (!current.contains(neighbor)) {
                        boundary = true;
                        break;
                    }
                }
                if (!boundary) next.add(vertex);
            }
            selection.clear();
            selection.setMode(MeshSelectionMode.VERTEX);
            for (int index : next) selection.addVertex(node, index);
        } else if (selection.mode() == MeshSelectionMode.EDGE) {
            Set<Long> current = new LinkedHashSet<>();
            for (int[] edge : selection.edgeIndices()) {
                current.add(MeshTopologySelection.edgeKey(edge[0], edge[1]));
            }
            Set<Long> next = new LinkedHashSet<>();
            for (long edge : current) {
                boolean exposed = false;
                for (long neighbor : MeshTopologySelection.edgeNeighbors(mesh, edge)) {
                    if (!current.contains(neighbor)) {
                        exposed = true;
                        break;
                    }
                }
                if (!exposed) next.add(edge);
            }
            selection.clear();
            selection.setMode(MeshSelectionMode.EDGE);
            for (long edge : next) selection.addEdge(node, (int)(edge >>> 32), (int)edge);
        } else {
            Set<Integer> current = new LinkedHashSet<>(selection.faceIndices());
            Set<Integer> next = new LinkedHashSet<>();
            for (int face : current) {
                boolean exposed = false;
                int[] vertices = mesh.faces().get(face).vertices();
                for (int other = 0; other < mesh.faces().size(); other++) {
                    if (current.contains(other)) continue;
                    if (sharesEdge(vertices, mesh.faces().get(other).vertices())) {
                        exposed = true;
                        break;
                    }
                }
                if (!exposed) next.add(face);
            }
            selection.clear();
            selection.setMode(MeshSelectionMode.FACE);
            for (int index : next) selection.addFace(node, index);
        }
    }

    public void selectLinked(MeshComponentSelection selection, ModelNode node) {
        if (selection == null || node == null || !selection.matches(node)) return;
        MeshGeometry mesh = node.ensureMeshGeometry();
        if (mesh == null) return;

        if (selection.mode() == MeshSelectionMode.VERTEX) {
            Set<Integer> seeds = new LinkedHashSet<>(selection.vertexIndices());
            selection.clear();
            selection.setMode(MeshSelectionMode.VERTEX);
            for (int index : MeshTopologySelection.linkedVertices(mesh, seeds)) {
                selection.addVertex(node, index);
            }
        } else if (selection.mode() == MeshSelectionMode.EDGE) {
            Set<Long> seeds = new LinkedHashSet<>();
            for (int[] edge : selection.edgeIndices()) {
                seeds.add(MeshTopologySelection.edgeKey(edge[0], edge[1]));
            }
            selection.clear();
            selection.setMode(MeshSelectionMode.EDGE);
            for (long edge : MeshTopologySelection.linkedEdges(mesh, seeds)) {
                selection.addEdge(node, (int)(edge >>> 32), (int)edge);
            }
        } else {
            Set<Integer> seeds = new LinkedHashSet<>(selection.faceIndices());
            selection.clear();
            selection.setMode(MeshSelectionMode.FACE);
            for (int index : MeshTopologySelection.linkedFaces(mesh, seeds)) {
                selection.addFace(node, index);
            }
        }
    }

    private static boolean sharesEdge(int[] a, int[] b) {
        int shared = 0;
        for (int x : a) for (int y : b) if (x == y) shared++;
        return shared >= 2;
    }
}
