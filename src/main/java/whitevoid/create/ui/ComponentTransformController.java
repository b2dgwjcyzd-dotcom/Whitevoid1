package whitevoid.create.ui;

import whitevoid.create.core.CreateCore;
import whitevoid.create.core.history.commands.SetMeshGeometryCommand;
import whitevoid.create.editor.geometry.MeshSelectionMode;
import whitevoid.create.editor.geometry.MeshComponentSelection;
import whitevoid.create.model.MeshGeometry;
import whitevoid.create.model.ModelNode;
import whitevoid.create.model.TransformMath;
import whitevoid.create.editor.geometry.MeshComponentTransforms;

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
    public double startX() { return lastX; }
    public double startY() { return lastY; }

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

    public MeshGeometry applyMove(MeshGeometry source, java.util.Set<Integer> ids, ModelNode node,
                                  ViewportProjector projector, double totalDx, double totalDy,
                                  int viewportWidth, int viewportHeight,
                                  ComponentTransformGizmo.Axis constraintAxis,
                                  boolean planeConstraint, boolean proportional,
                                  double proportionalRadius, boolean snap, double snapIncrement) {
        if (!dragging || source == null || ids == null || ids.isEmpty() || node == null || pivot == null) {
            return source;
        }

        ComponentTransformGizmo.Axis constrainedAxis =
                constraintAxis != null && constraintAxis != ComponentTransformGizmo.Axis.NONE
                        ? constraintAxis : axis;

        if (planeConstraint && constrainedAxis != ComponentTransformGizmo.Axis.NONE) {
            ComponentTransformGizmo.Axis a1 = constrainedAxis == ComponentTransformGizmo.Axis.X
                    ? ComponentTransformGizmo.Axis.Y : ComponentTransformGizmo.Axis.X;
            ComponentTransformGizmo.Axis a2 = constrainedAxis == ComponentTransformGizmo.Axis.Z
                    ? ComponentTransformGizmo.Axis.Y : ComponentTransformGizmo.Axis.Z;

            double amount1 = gizmo.amount(a1, projector, node, pivot,
                    viewportWidth / 2, viewportHeight / 2, totalDx, totalDy);
            double amount2 = gizmo.amount(a2, projector, node, pivot,
                    viewportWidth / 2, viewportHeight / 2, totalDx, totalDy);

            if (snap) {
                amount1 = snap(amount1, snapIncrement);
                amount2 = snap(amount2, snapIncrement);
            }

            int excluded = constrainedAxis == ComponentTransformGizmo.Axis.X ? 0
                    : constrainedAxis == ComponentTransformGizmo.Axis.Y ? 1 : 2;
            double dx = excluded == 0 ? 0 : (a1 == ComponentTransformGizmo.Axis.X ? amount1 : amount2);
            double dy = excluded == 1 ? 0 : (a1 == ComponentTransformGizmo.Axis.Y ? amount1 : amount2);
            double dz = excluded == 2 ? 0 : (a1 == ComponentTransformGizmo.Axis.Z ? amount1 : amount2);

            return proportional
                    ? MeshComponentTransforms.translateProportional(source, ids, pivot, proportionalRadius, dx, dy, dz)
                    : MeshComponentTransforms.translate(source, ids, dx, dy, dz);
        }

        double amount = gizmo.amount(constrainedAxis, projector, node, pivot,
                viewportWidth / 2, viewportHeight / 2, totalDx, totalDy);
        if (snap) amount = snap(amount, snapIncrement);

        int axisIndex = constrainedAxis == ComponentTransformGizmo.Axis.X ? 0
                : constrainedAxis == ComponentTransformGizmo.Axis.Y ? 1 : 2;
        double dx = axisIndex == 0 ? amount : 0;
        double dy = axisIndex == 1 ? amount : 0;
        double dz = axisIndex == 2 ? amount : 0;

        return proportional
                ? MeshComponentTransforms.translateProportional(source, ids, pivot, proportionalRadius, dx, dy, dz)
                : MeshComponentTransforms.translate(source, ids, dx, dy, dz);
    }

    public MeshGeometry applyNumeric(MeshGeometry source, java.util.Set<Integer> ids, double value,
                                      boolean planeConstraint, boolean proportional, double proportionalRadius) {
        if (source == null || ids == null || ids.isEmpty() || pivot == null
                || axis == ComponentTransformGizmo.Axis.NONE) return source;

        int axisIndex = axis == ComponentTransformGizmo.Axis.X ? 0
                : axis == ComponentTransformGizmo.Axis.Y ? 1 : 2;

        return switch (operation) {
            case MOVE -> {
                MeshGeometry result = source;
                if (planeConstraint) {
                    ComponentTransformGizmo.Axis a1 = axis == ComponentTransformGizmo.Axis.X
                            ? ComponentTransformGizmo.Axis.Y : ComponentTransformGizmo.Axis.X;
                    ComponentTransformGizmo.Axis a2 = axis == ComponentTransformGizmo.Axis.Z
                            ? ComponentTransformGizmo.Axis.Y : ComponentTransformGizmo.Axis.Z;
                    result = MeshComponentTransforms.translate(result, ids,
                            a1 == ComponentTransformGizmo.Axis.X ? value : 0,
                            a1 == ComponentTransformGizmo.Axis.Y ? value : 0,
                            a1 == ComponentTransformGizmo.Axis.Z ? value : 0);
                    result = MeshComponentTransforms.translate(result, ids,
                            a2 == ComponentTransformGizmo.Axis.X ? value : 0,
                            a2 == ComponentTransformGizmo.Axis.Y ? value : 0,
                            a2 == ComponentTransformGizmo.Axis.Z ? value : 0);
                } else {
                    result = proportional
                            ? MeshComponentTransforms.translateProportional(result, ids, pivot, proportionalRadius,
                                axisIndex == 0 ? value : 0, axisIndex == 1 ? value : 0, axisIndex == 2 ? value : 0)
                            : MeshComponentTransforms.translate(result, ids,
                                axisIndex == 0 ? value : 0, axisIndex == 1 ? value : 0, axisIndex == 2 ? value : 0);
                }
                yield result;
            }
            case ROTATE -> proportional
                    ? MeshComponentTransforms.rotateProportional(source, ids, pivot, proportionalRadius, axisIndex, value)
                    : MeshComponentTransforms.rotate(source, ids, pivot, axisIndex, value);
            case SCALE -> {
                double factor = Math.max(0.01, value);
                MeshGeometry result = source;
                if (planeConstraint) {
                    int a1 = axisIndex == 0 ? 1 : 0;
                    int a2 = axisIndex == 2 ? 1 : 2;
                    result = MeshComponentTransforms.scale(result, ids, pivot, a1, factor);
                    result = MeshComponentTransforms.scale(result, ids, pivot, a2, factor);
                } else {
                    result = proportional
                            ? MeshComponentTransforms.scaleProportional(result, ids, pivot, proportionalRadius, axisIndex, factor)
                            : MeshComponentTransforms.scale(result, ids, pivot, axisIndex, factor);
                }
                yield result;
            }
        };
    }

    private static double snap(double value, double increment) {
        if (increment <= 0.0) return value;
        return Math.round(value / increment) * increment;
    }

    public MeshGeometry applyRotate(MeshGeometry source, java.util.Set<Integer> ids, ModelNode node,
                                    ViewportProjector projector, double startX, double startY,
                                    double mouseX, double mouseY, int viewportWidth, int viewportHeight,
                                    ComponentTransformGizmo.Axis constraintAxis,
                                    boolean proportional, double proportionalRadius,
                                    boolean snap, double snapIncrement) {
        if (!dragging || source == null || ids == null || ids.isEmpty() || node == null || pivot == null) {
            return source;
        }
        ComponentTransformGizmo.Axis constrainedAxis =
                constraintAxis != null && constraintAxis != ComponentTransformGizmo.Axis.NONE
                        ? constraintAxis : axis;
        int axisIndex = constrainedAxis == ComponentTransformGizmo.Axis.X ? 0
                : constrainedAxis == ComponentTransformGizmo.Axis.Y ? 1 : 2;
        double degrees = gizmo.rotationAmount(constrainedAxis, node, pivot,
                startX, startY, mouseX, mouseY, projector,
                viewportWidth / 2, viewportHeight / 2);
        if (snap) degrees = snap(degrees, snapIncrement);
        return proportional
                ? MeshComponentTransforms.rotateProportional(source, ids, pivot, proportionalRadius, axisIndex, degrees)
                : MeshComponentTransforms.rotate(source, ids, pivot, axisIndex, degrees);
    }

    public MeshGeometry applyScale(MeshGeometry source, java.util.Set<Integer> ids, ModelNode node,
                                   ViewportProjector projector, double totalDx, double totalDy,
                                   int viewportWidth, int viewportHeight,
                                   ComponentTransformGizmo.Axis constraintAxis,
                                   boolean planeConstraint, boolean proportional,
                                   double proportionalRadius, boolean snap, double snapIncrement) {
        if (!dragging || source == null || ids == null || ids.isEmpty() || node == null || pivot == null) {
            return source;
        }

        ComponentTransformGizmo.Axis constrainedAxis =
                constraintAxis != null && constraintAxis != ComponentTransformGizmo.Axis.NONE
                        ? constraintAxis : axis;

        if (planeConstraint && constrainedAxis != ComponentTransformGizmo.Axis.NONE) {
            ComponentTransformGizmo.Axis a1 = constrainedAxis == ComponentTransformGizmo.Axis.X
                    ? ComponentTransformGizmo.Axis.Y : ComponentTransformGizmo.Axis.X;
            ComponentTransformGizmo.Axis a2 = constrainedAxis == ComponentTransformGizmo.Axis.Z
                    ? ComponentTransformGizmo.Axis.Y : ComponentTransformGizmo.Axis.Z;

            double factor1 = gizmo.scaleFactor(a1, projector, node, pivot,
                    viewportWidth / 2, viewportHeight / 2, totalDx, totalDy);
            double factor2 = gizmo.scaleFactor(a2, projector, node, pivot,
                    viewportWidth / 2, viewportHeight / 2, totalDx, totalDy);

            if (snap) {
                factor1 = snapScaleFactor(factor1, snapIncrement);
                factor2 = snapScaleFactor(factor2, snapIncrement);
            }

            source = scaleAxis(source, ids, pivot, proportional, proportionalRadius, a1, factor1);
            return scaleAxis(source, ids, pivot, proportional, proportionalRadius, a2, factor2);
        }

        double factor = gizmo.scaleFactor(constrainedAxis, projector, node, pivot,
                viewportWidth / 2, viewportHeight / 2, totalDx, totalDy);
        if (snap) factor = snapScaleFactor(factor, snapIncrement);
        return scaleAxis(source, ids, pivot, proportional, proportionalRadius, constrainedAxis, factor);
    }

    private static MeshGeometry scaleAxis(MeshGeometry source, java.util.Set<Integer> ids,
                                          TransformMath.Point pivot, boolean proportional,
                                          double proportionalRadius,
                                          ComponentTransformGizmo.Axis axis, double factor) {
        int axisIndex = axis == ComponentTransformGizmo.Axis.X ? 0
                : axis == ComponentTransformGizmo.Axis.Y ? 1 : 2;
        return proportional
                ? MeshComponentTransforms.scaleProportional(source, ids, pivot, proportionalRadius, axisIndex, factor)
                : MeshComponentTransforms.scale(source, ids, pivot, axisIndex, factor);
    }

    private static double snapScaleFactor(double factor, double increment) {
        if (increment <= 0.0) return factor;
        return Math.max(0.01, Math.round(factor / increment) * increment);
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
