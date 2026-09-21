package whitevoid.create.ui;

import whitevoid.create.core.CreateCore;
import whitevoid.create.editor.geometry.MeshSelectionMode;
import whitevoid.create.editor.viewport.ViewportContext;
import whitevoid.create.model.MeshGeometry;
import whitevoid.create.model.ModelNode;

public final class ComponentTransformMouseController {
    private final ComponentTransformController transform;

    public ComponentTransformMouseController(ComponentTransformController transform) {
        this.transform = transform;
    }

    public boolean beginKeyboardArmed(ModelNode node, ViewportContext viewport,
                                      double mouseX, double mouseY) {
        if (node == null || viewport.meshComponentSelection().size() <= 0
                || transform.constraintAxis() == ComponentTransformGizmo.Axis.NONE) {
            return false;
        }
        begin(node, viewport, mouseX, mouseY);
        return transform.dragging();
    }

    public boolean beginFromGizmo(ModelNode node, ViewportContext viewport,
                                  ViewportProjector projector, double mouseX, double mouseY,
                                  int centerX, int centerY) {
        MeshSelectionMode mode = viewport.meshComponentSelection().mode();
        ComponentTransformGizmo.Axis axis = transform.gizmo().hover(node, mode,
                viewport.meshComponentSelection().vertexIndices(),
                viewport.meshComponentSelection().edgeIndices(),
                viewport.meshComponentSelection().faceIndices(),
                projector, mouseX, mouseY, centerX, centerY,
                transform.operation(), transform.pivotMode(), viewport.meshComponentSelection());
        if (axis == ComponentTransformGizmo.Axis.NONE) return false;
        transform.setAxis(axis);
        transform.setConstraintMode(transform.planeConstraint()
                ? ComponentTransformController.ConstraintMode.PLANE
                : ComponentTransformController.ConstraintMode.AXIS);
        begin(node, viewport, mouseX, mouseY);
        return transform.dragging();
    }

    public void update(ModelNode node, ViewportContext viewport,
                       ViewportProjector projector, double mouseX, double mouseY,
                       int width, int height, boolean proportional, double proportionalRadius,
                       boolean snap) {
        if (!transform.dragging() || node == null) return;
        MeshGeometry mesh = node.ensureMeshGeometry();
        if (mesh == null) return;

        MeshSelectionMode mode = viewport.meshComponentSelection().mode();
        var selection = viewport.meshComponentSelection();
        var ids = whitevoid.create.editor.geometry.MeshComponentTransforms.affectedVertices(
                mesh, mode, selection.vertexIndices(), selection.edgeIndices(), selection.faceIndices());
        double totalDx = mouseX - transform.startX();
        double totalDy = mouseY - transform.startY();
        MeshGeometry updated = mesh.copy();

        switch (transform.operation()) {
            case MOVE -> updated = transform.applyMove(updated, ids, node, projector,
                    totalDx, totalDy, width, height, transform.constraintAxis(),
                    transform.planeConstraint(), proportional, proportionalRadius, snap, 0.25);
            case ROTATE -> updated = transform.applyRotate(updated, ids, node, projector,
                    transform.startX(), transform.startY(), mouseX, mouseY,
                    width, height, transform.constraintAxis(), proportional, proportionalRadius, snap, 5.0);
            case SCALE -> updated = transform.applyScale(updated, ids, node, projector,
                    totalDx, totalDy, width, height, transform.constraintAxis(),
                    transform.planeConstraint(), proportional, proportionalRadius, snap, 0.05);
        }
        node.setMeshGeometry(updated);
    }

    public void finish(CreateCore core, ModelNode node) {
        transform.finish(core, node);
    }

    private void begin(ModelNode node, ViewportContext viewport, double mouseX, double mouseY) {
        MeshSelectionMode mode = viewport.meshComponentSelection().mode();
        transform.begin(node, mode,
                viewport.meshComponentSelection().vertexIndices(),
                viewport.meshComponentSelection().edgeIndices(),
                viewport.meshComponentSelection().faceIndices(),
                viewport.meshComponentSelection(), mouseX, mouseY);
    }
}
