package whitevoid.create.ui;

import whitevoid.create.editor.geometry.GeometryFace;
import whitevoid.create.editor.geometry.MeshSelectionMode;
import whitevoid.create.editor.viewport.ViewportContext;
import whitevoid.create.model.ModelNode;

public final class MeshEditorHoverController {
    public record HoverResult(int face, int vertex, int edgeA, int edgeB) {
        public static HoverResult none() { return new HoverResult(-1, -1, -1, -1); }
    }

    private final ViewportGizmo gizmo;

    public MeshEditorHoverController(ViewportGizmo gizmo) {
        this.gizmo = gizmo;
    }

    public HoverResult resolve(ModelNode node, ViewportContext viewport,
                               double mouseX, double mouseY, int cx, int cy,
                               ViewportGizmo.Axis gizmoAxis,
                               ComponentTransformGizmo.Axis componentAxis) {
        if (node == null || viewport == null
                || gizmoAxis != ViewportGizmo.Axis.NONE
                || componentAxis != ComponentTransformGizmo.Axis.NONE) {
            return HoverResult.none();
        }

        var projector = new ViewportProjector(viewport.viewport().camera());
        MeshSelectionMode mode = viewport.meshComponentSelection().mode();

        return switch (mode) {
            case FACE -> new HoverResult(
                    gizmo.meshFaceHit(node, projector, mouseX, mouseY, cx, cy), -1, -1, -1);
            case VERTEX -> new HoverResult(
                    -1, gizmo.meshVertexHit(node, projector, mouseX, mouseY, cx, cy), -1, -1);
            case EDGE -> {
                int[] edge = gizmo.meshEdgeHit(node, projector, mouseX, mouseY, cx, cy);
                yield edge == null
                        ? HoverResult.none()
                        : new HoverResult(-1, -1, edge[0], edge[1]);
            }
        };
    }

    public GeometryFace primitiveFace(ModelNode node, ViewportContext viewport,
                                      double mouseX, double mouseY, int cx, int cy,
                                      ViewportGizmo.Axis gizmoAxis,
                                      ComponentTransformGizmo.Axis componentAxis) {
        if (node == null || viewport == null
                || viewport.transform().mode() != whitevoid.create.editor.transform.TransformMode.GEOMETRY
                || gizmoAxis != ViewportGizmo.Axis.NONE
                || componentAxis != ComponentTransformGizmo.Axis.NONE) {
            return GeometryFace.NONE;
        }
        return gizmo.faceHit(node, new ViewportProjector(viewport.viewport().camera()),
                mouseX, mouseY, cx, cy);
    }
}
