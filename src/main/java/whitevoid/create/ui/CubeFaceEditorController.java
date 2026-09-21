package whitevoid.create.ui;

import whitevoid.create.core.CreateCore;
import whitevoid.create.core.history.commands.ResizeCubeFaceCommand;
import whitevoid.create.editor.geometry.GeometryFace;
import whitevoid.create.editor.viewport.ViewportContext;
import whitevoid.create.model.CubeGeometry;
import whitevoid.create.model.ModelNode;

public final class CubeFaceEditorController {
    private final ViewportGizmo gizmo;

    private boolean dragging;
    private GeometryFace activeFace = GeometryFace.NONE;
    private CubeGeometry oldGeometry;
    private double oldX;
    private double oldY;
    private double oldZ;

    public CubeFaceEditorController(ViewportGizmo gizmo) {
        this.gizmo = gizmo;
    }

    public boolean dragging() {
        return dragging;
    }

    public GeometryFace activeFace() {
        return activeFace;
    }

    public void begin(ModelNode node, GeometryFace face) {
        if (node == null || face == null || face == GeometryFace.NONE || node.geometry() == null) {
            cancel();
            return;
        }

        dragging = true;
        activeFace = face;
        oldGeometry = node.geometry();
        oldX = node.transform().x();
        oldY = node.transform().y();
        oldZ = node.transform().z();
    }

    public void update(CreateCore core, double deltaX, double deltaY) {
        if (!dragging || activeFace == GeometryFace.NONE) return;

        ModelNode node = selectedNode(core);
        if (node == null || node.geometry() == null) {
            cancel();
            return;
        }

        var projector = new ViewportProjector(core.editorContext().viewport().viewport().camera());
        double amount = gizmo.faceDragAmount(activeFace, projector, deltaX, deltaY);

        CubeGeometry geometry = node.geometry();
        double width = geometry.width();
        double height = geometry.height();
        double depth = geometry.depth();

        double sign = switch (activeFace) {
            case POS_X, POS_Y, POS_Z -> 1.0;
            case NEG_X, NEG_Y, NEG_Z -> -1.0;
            case NONE -> 0.0;
        };

        double move = amount * 2.0 * sign;
        switch (activeFace) {
            case POS_X, NEG_X -> width = Math.max(0.1, width + move);
            case POS_Y, NEG_Y -> height = Math.max(0.1, height + move);
            case POS_Z, NEG_Z -> depth = Math.max(0.1, depth + move);
            case NONE -> { return; }
        }

        node.setGeometry(new CubeGeometry(width, height, depth));
        node.transform().position(
                node.transform().x() + amount * sign * axisX(activeFace),
                node.transform().y() + amount * sign * axisY(activeFace),
                node.transform().z() + amount * sign * axisZ(activeFace)
        );
    }

    public void finish(CreateCore core) {
        if (!dragging) return;

        ModelNode node = selectedNode(core);
        if (node != null && oldGeometry != null && node.geometry() != null) {
            var transform = node.transform();
            boolean changed = !oldGeometry.equals(node.geometry())
                    || oldX != transform.x()
                    || oldY != transform.y()
                    || oldZ != transform.z();

            if (changed) {
                core.editorContext().history().recordExecuted(
                        new ResizeCubeFaceCommand(
                                node,
                                oldGeometry,
                                node.geometry(),
                                oldX, oldY, oldZ,
                                transform.x(), transform.y(), transform.z()));
            }
        }

        cancel();
    }

    /** Cancels the active drag and restores the state captured at begin(). */
    public void abort(CreateCore core) {
        if (!dragging) return;

        ModelNode node = selectedNode(core);
        if (node != null && oldGeometry != null) {
            node.setGeometry(oldGeometry);
            node.transform().position(oldX, oldY, oldZ);
        }
        cancel();
    }

    public void cancel() {
        dragging = false;
        activeFace = GeometryFace.NONE;
        oldGeometry = null;
    }

    private ModelNode selectedNode(CreateCore core) {
        ViewportContext viewport = core.editorContext().viewport();
        return viewport.geometryFaceSelection().node(core.editorContext().model());
    }

    private static double axisX(GeometryFace face) {
        return face == GeometryFace.POS_X || face == GeometryFace.NEG_X ? 1.0 : 0.0;
    }

    private static double axisY(GeometryFace face) {
        return face == GeometryFace.POS_Y || face == GeometryFace.NEG_Y ? 1.0 : 0.0;
    }

    private static double axisZ(GeometryFace face) {
        return face == GeometryFace.POS_Z || face == GeometryFace.NEG_Z ? 1.0 : 0.0;
    }
}
