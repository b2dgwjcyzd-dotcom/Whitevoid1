package whitevoid.create.ui;

import java.util.List;
import whitevoid.create.editor.geometry.MeshSelectionMode;
import whitevoid.create.editor.viewport.ViewportContext;
import whitevoid.create.model.ModelNode;
import whitevoid.create.model.MeshGeometry;

public final class MeshEditorController {
    public enum PickType { NONE, VERTEX, EDGE, FACE }

    public record PickResult(PickType type, int index, int edgeA, int edgeB) {
        public static PickResult none() { return new PickResult(PickType.NONE, -1, -1, -1); }
        public static PickResult vertex(int index) { return new PickResult(PickType.VERTEX, index, -1, -1); }
        public static PickResult edge(int a, int b) { return new PickResult(PickType.EDGE, -1, a, b); }
        public static PickResult face(int index) { return new PickResult(PickType.FACE, index, -1, -1); }
    }

    private final ViewportGizmo gizmo;
    private int throughIndex = -1;
    private double throughX = Double.NaN;
    private double throughY = Double.NaN;

    public MeshEditorController(ViewportGizmo gizmo) {
        this.gizmo = gizmo;
    }

    public PickResult pickAndSelect(ModelNode node, ViewportContext viewport,
                                     double mouseX, double mouseY, int cx, int cy,
                                     boolean selectThrough, boolean alt, boolean shift, boolean control) {
        if (node == null || viewport == null) return PickResult.none();

        MeshSelectionMode mode = viewport.meshComponentSelection().mode();
        var projector = new ViewportProjector(viewport.viewport().camera());

        switch (mode) {
            case VERTEX -> {
                List<Integer> hits = selectThrough
                        ? gizmo.meshVertexHits(node, projector, mouseX, mouseY, cx, cy)
                        : List.of(gizmo.meshVertexHit(node, projector, mouseX, mouseY, cx, cy));
                int vertex = choose(mode, mouseX, mouseY, hits.size());
                if (vertex < 0 || vertex >= hits.size()) return PickResult.none();
                vertex = hits.get(vertex);
                var selection = viewport.meshComponentSelection();
                if (alt) selection.removeVertex(node, vertex);
                else if (control && !shift && selection.activeVertex() >= 0 && selection.activeVertex() != vertex)
                    selection.selectShortestVertexPath(node, selection.activeVertex(), vertex);
                else if (shift) selection.toggleVertex(node, vertex);
                else selection.selectVertex(node, vertex);
                return PickResult.vertex(vertex);
            }
            case EDGE -> {
                List<int[]> hits;
                if (selectThrough) hits = gizmo.meshEdgeHits(node, projector, mouseX, mouseY, cx, cy);
                else {
                    int[] edge = gizmo.meshEdgeHit(node, projector, mouseX, mouseY, cx, cy);
                    hits = edge == null ? List.of() : List.of(edge);
                }
                int pick = choose(mode, mouseX, mouseY, hits.size());
                if (pick < 0) return PickResult.none();
                int[] edge = hits.get(pick);
                var selection = viewport.meshComponentSelection();
                if (alt) selection.removeEdge(node, edge[0], edge[1]);
                else if (shift) selection.toggleEdge(node, edge[0], edge[1]);
                else selection.selectEdge(node, edge[0], edge[1]);
                return PickResult.edge(edge[0], edge[1]);
            }
            case FACE -> {
                List<Integer> hits = selectThrough
                        ? gizmo.meshFaceHits(node, projector, mouseX, mouseY, cx, cy)
                        : List.of(gizmo.meshFaceHit(node, projector, mouseX, mouseY, cx, cy));
                int pick = choose(mode, mouseX, mouseY, hits.size());
                if (pick < 0) return PickResult.none();
                int face = hits.get(pick);
                var selection = viewport.meshComponentSelection();
                if (alt) selection.removeFace(node, face);
                else if (shift) selection.toggleFace(node, face);
                else selection.selectFace(node, face);
                return PickResult.face(face);
            }
        }
        return PickResult.none();
    }

    public void resetThroughCycle() {
        throughIndex = -1;
        throughX = Double.NaN;
        throughY = Double.NaN;
    }

    private int choose(MeshSelectionMode mode, double x, double y, int count) {
        if (count <= 0) return -1;
        if (Double.isNaN(throughX) || Math.abs(throughX - x) > 4.0
                || Math.abs(throughY - y) > 4.0 || throughIndex >= count - 1) {
            throughIndex = 0;
        } else {
            throughIndex++;
        }
        throughX = x;
        throughY = y;
        return Math.max(0, Math.min(throughIndex, count - 1));
    }
}
