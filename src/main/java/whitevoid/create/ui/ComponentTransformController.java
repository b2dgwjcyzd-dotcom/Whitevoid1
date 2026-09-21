package whitevoid.create.ui;

import whitevoid.create.core.CreateCore;
import whitevoid.create.core.history.commands.SetMeshGeometryCommand;
import whitevoid.create.editor.geometry.MeshSelectionMode;
import whitevoid.create.editor.geometry.MeshComponentSelection;
import whitevoid.create.model.MeshGeometry;
import whitevoid.create.model.ModelNode;
import whitevoid.create.model.TransformMath;

import java.util.List;

/**
 * Owns the lifecycle/state of component-level Move/Rotate/Scale editing.
 * Actual geometry math remains in ComponentTransformGizmo and MeshComponentTransforms.
 */
public final class ComponentTransformController {
    public enum ConstraintMode { AXIS, PLANE }

    private final ComponentTransformGizmo gizmo;
    private ComponentTransformGizmo.Axis axis = ComponentTransformGizmo.Axis.NONE;
    private ComponentTransformGizmo.Operation operation = ComponentTransformGizmo.Operation.MOVE;
    private ComponentTransformGizmo.PivotMode pivotMode = ComponentTransformGizmo.PivotMode.MEDIAN;
    private ConstraintMode constraintMode = ConstraintMode.AXIS;
    private MeshGeometry oldMesh;
    private TransformMath.Point pivot;
    private double lastX;
    private double lastY;
    private boolean dragging;

    public ComponentTransformController(ComponentTransformGizmo gizmo) {
        this.gizmo = gizmo;
    }

    public ComponentTransformGizmo.Axis axis() { return axis; }
    public ComponentTransformGizmo.Operation operation() { return operation; }
    public ComponentTransformGizmo.PivotMode pivotMode() { return pivotMode; }
    public ConstraintMode constraintMode() { return constraintMode; }
    public TransformMath.Point pivot() { return pivot; }
    public boolean dragging() { return dragging; }

    public void setOperation(ComponentTransformGizmo.Operation operation) {
        if (operation != null) this.operation = operation;
    }

    public void setAxis(ComponentTransformGizmo.Axis axis) {
        this.axis = axis == null ? ComponentTransformGizmo.Axis.NONE : axis;
    }

    public void cyclePivotMode() {
        pivotMode = pivotMode.next();
    }

    public void setConstraintMode(ConstraintMode mode) {
        if (mode != null) constraintMode = mode;
    }

    public void begin(ModelNode node, MeshSelectionMode mode,
                      List<Integer> vertices, List<int[]> edges, List<Integer> faces,
                      MeshComponentSelection selection, double mouseX, double mouseY) {
        if (node == null) {
            cancel();
            return;
        }
        MeshGeometry mesh = node.ensureMeshGeometry();
        if (mesh == null) {
            cancel();
            return;
        }
        oldMesh = mesh.copy();
        pivot = gizmo.localPivot(node, mode, vertices, edges, faces, pivotMode, selection);
        lastX = mouseX;
        lastY = mouseY;
        dragging = true;
    }

    public void updateMouse(double mouseX, double mouseY) {
        if (!dragging) return;
        lastX = mouseX;
        lastY = mouseY;
    }

    public double deltaX(double mouseX) {
        return dragging ? mouseX - lastX : 0.0;
    }

    public double deltaY(double mouseY) {
        return dragging ? mouseY - lastY : 0.0;
    }

    public void finish(CreateCore core, ModelNode node) {
        if (!dragging) return;
        if (core != null && node != null && oldMesh != null) {
            MeshGeometry current = node.ensureMeshGeometry();
            if (current != null && !oldMesh.equals(current)) {
                core.editorContext().history().recordExecuted(
                        new SetMeshGeometryCommand(node, oldMesh, current.copy()));
            }
        }
        cancel();
    }

    public void cancel() {
        dragging = false;
        oldMesh = null;
        pivot = null;
        axis = ComponentTransformGizmo.Axis.NONE;
        lastX = 0.0;
        lastY = 0.0;
    }
}
