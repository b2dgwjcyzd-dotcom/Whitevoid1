package whitevoid.create.ui;

import org.lwjgl.glfw.GLFW;
import whitevoid.create.core.CreateCore;
import whitevoid.create.core.history.commands.SetMeshGeometryCommand;
import whitevoid.create.editor.geometry.MeshComponentSelection;
import whitevoid.create.editor.geometry.MeshComponentTransforms;
import whitevoid.create.editor.geometry.MeshSelectionMode;
import whitevoid.create.editor.transform.TransformMode;
import whitevoid.create.editor.viewport.ViewportContext;
import whitevoid.create.model.MeshGeometry;
import whitevoid.create.model.ModelNode;

public final class ComponentTransformInputController {
    private final ComponentTransformController transform;

    private boolean armed;
    private boolean numericEntry;
    private boolean numericNegative;
    private final StringBuilder numericBuffer = new StringBuilder();

    public ComponentTransformInputController(ComponentTransformController transform) {
        this.transform = transform;
    }

    public boolean armed() {
        return armed;
    }

    public boolean numericEntry() {
        return numericEntry;
    }

    public boolean numericNegative() {
        return numericNegative;
    }

    public String numericBuffer() {
        return numericBuffer.toString();
    }

    public void disarm() {
        armed = false;
        clearNumeric();
        transform.clearConstraint();
    }

    public boolean handleKey(int keyCode, ViewportContext viewport, boolean shiftDown) {
        if (viewport.transform().mode() != TransformMode.GEOMETRY
                || viewport.meshComponentSelection().size() <= 0) {
            return false;
        }

        if (armed) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                disarm();
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_MINUS || keyCode == GLFW.GLFW_KEY_KP_SUBTRACT) {
                numericNegative = !numericNegative;
                numericEntry = true;
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_PERIOD || keyCode == GLFW.GLFW_KEY_KP_DECIMAL) {
                if (!numericBuffer.toString().contains(".")) numericBuffer.append('.');
                numericEntry = true;
                return true;
            }

            int digit = digit(keyCode);
            if (digit >= 0) {
                numericBuffer.append((char) ('0' + digit));
                numericEntry = true;
                return true;
            }

            if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
                if (numericEntry) {
                    return true;
                }
            }
        }

        if (keyCode == GLFW.GLFW_KEY_G) {
            armOrSetMode(viewport, ComponentTransformGizmo.Operation.MOVE);
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_R) {
            armOrSetMode(viewport, ComponentTransformGizmo.Operation.ROTATE);
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_S) {
            armOrSetMode(viewport, ComponentTransformGizmo.Operation.SCALE);
            return true;
        }

        if (armed) {
            ComponentTransformGizmo.Axis requested = switch (keyCode) {
                case GLFW.GLFW_KEY_X -> ComponentTransformGizmo.Axis.X;
                case GLFW.GLFW_KEY_Y -> ComponentTransformGizmo.Axis.Y;
                case GLFW.GLFW_KEY_Z -> ComponentTransformGizmo.Axis.Z;
                default -> ComponentTransformGizmo.Axis.NONE;
            };
            if (requested != ComponentTransformGizmo.Axis.NONE) {
                if (transform.constraintAxis() == requested && !shiftDown) {
                    transform.clearConstraint();
                } else {
                    transform.setConstraintAxis(requested);
                    transform.setConstraintMode(shiftDown
                            ? ComponentTransformController.ConstraintMode.PLANE
                            : ComponentTransformController.ConstraintMode.AXIS);
                }
                return true;
            }
        }

        return false;
    }

    public boolean applyNumeric(CreateCore core, ViewportContext viewport,
                                boolean proportional, double proportionalRadius) {
        if (!armed || !numericEntry || numericBuffer.length() == 0
                || transform.constraintAxis() == ComponentTransformGizmo.Axis.NONE) {
            return false;
        }

        ModelNode node = viewport.selection().first(core.editorContext().model());
        if (node == null) return false;

        double value;
        try {
            value = Double.parseDouble((numericNegative ? "-" : "") + numericBuffer);
        } catch (NumberFormatException ignored) {
            return false;
        }

        MeshComponentSelection selection = viewport.meshComponentSelection();
        MeshGeometry mesh = node.ensureMeshGeometry();
        if (mesh == null) return false;

        var ids = MeshComponentTransforms.affectedVertices(mesh, selection.mode(),
                selection.vertexIndices(), selection.edgeIndices(), selection.faceIndices());

        transform.begin(node, selection.mode(),
                selection.vertexIndices(), selection.edgeIndices(), selection.faceIndices(),
                selection, 0.0, 0.0);

        MeshGeometry before = mesh.copy();
        MeshGeometry updated = transform.applyNumeric(mesh.copy(), ids, value,
                transform.planeConstraint(), proportional, proportionalRadius);

        transform.cancel();

        if (!before.equals(updated)) {
            node.setMeshGeometry(updated);
            core.editorContext().history().recordExecuted(
                    new SetMeshGeometryCommand(node, before, updated.copy()));
        }

        disarm();
        return true;
    }

    private void armOrSetMode(ViewportContext viewport, ComponentTransformGizmo.Operation operation) {
        transform.clearConstraint();
        if (viewport.transform().mode() == TransformMode.GEOMETRY
                && viewport.meshComponentSelection().size() > 0) {
            transform.setOperation(operation);
            armed = true;
            clearNumeric();
        } else {
            viewport.transform().setMode(switch (operation) {
                case MOVE -> TransformMode.MOVE;
                case ROTATE -> TransformMode.ROTATE;
                case SCALE -> TransformMode.SCALE;
            });
        }
    }

    private void clearNumeric() {
        numericEntry = false;
        numericNegative = false;
        numericBuffer.setLength(0);
    }

    private static int digit(int keyCode) {
        return switch (keyCode) {
            case GLFW.GLFW_KEY_0, GLFW.GLFW_KEY_KP_0 -> 0;
            case GLFW.GLFW_KEY_1, GLFW.GLFW_KEY_KP_1 -> 1;
            case GLFW.GLFW_KEY_2, GLFW.GLFW_KEY_KP_2 -> 2;
            case GLFW.GLFW_KEY_3, GLFW.GLFW_KEY_KP_3 -> 3;
            case GLFW.GLFW_KEY_4, GLFW.GLFW_KEY_KP_4 -> 4;
            case GLFW.GLFW_KEY_5, GLFW.GLFW_KEY_KP_5 -> 5;
            case GLFW.GLFW_KEY_6, GLFW.GLFW_KEY_KP_6 -> 6;
            case GLFW.GLFW_KEY_7, GLFW.GLFW_KEY_KP_7 -> 7;
            case GLFW.GLFW_KEY_8, GLFW.GLFW_KEY_KP_8 -> 8;
            case GLFW.GLFW_KEY_9, GLFW.GLFW_KEY_KP_9 -> 9;
            default -> -1;
        };
    }
}
