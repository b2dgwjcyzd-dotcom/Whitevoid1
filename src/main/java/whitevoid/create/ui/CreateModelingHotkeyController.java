package whitevoid.create.ui;

import org.lwjgl.glfw.GLFW;
import whitevoid.create.core.CreateCore;
import whitevoid.create.editor.geometry.MeshSelectionMode;
import whitevoid.create.editor.transform.TransformMode;
import whitevoid.create.editor.viewport.ViewportContext;

public final class CreateModelingHotkeyController {
    private final CreateCore core;
    private final CreateMeshModelingController meshModeling;

    public CreateModelingHotkeyController(CreateCore core, CreateMeshModelingController meshModeling) {
        this.core = core;
        this.meshModeling = meshModeling;
    }

    public boolean handle(CreateViewportInteractionState interaction, ViewportContext viewport,
                          int keyCode, boolean shiftDown, boolean controlDown) {
        if (keyCode == GLFW.GLFW_KEY_M
                && viewport.transform().mode() == TransformMode.GEOMETRY
                && viewport.meshComponentSelection().size() > 0) {
            interaction.mirrorArmed = true;
            return true;
        }

        if (interaction.mirrorArmed
                && viewport.transform().mode() == TransformMode.GEOMETRY
                && viewport.meshComponentSelection().size() > 0) {
            int axis = switch (keyCode) {
                case GLFW.GLFW_KEY_X -> 0;
                case GLFW.GLFW_KEY_Y -> 1;
                case GLFW.GLFW_KEY_Z -> 2;
                default -> -1;
            };
            if (axis >= 0) {
                interaction.mirrorAxis = axis == 0 ? ComponentTransformGizmo.Axis.X
                        : axis == 1 ? ComponentTransformGizmo.Axis.Y
                        : ComponentTransformGizmo.Axis.Z;
                meshModeling.mirrorSelectedComponents(axis);
                interaction.mirrorArmed = false;
                return true;
            }
        }

        if (keyCode == GLFW.GLFW_KEY_B) {
            if (controlDown
                    && viewport.transform().mode() == TransformMode.GEOMETRY
                    && viewport.meshComponentSelection().mode() == MeshSelectionMode.EDGE) {
                meshModeling.bevelSelectedEdge(shiftDown ? 1.0 : 0.25);
                return true;
            }

            if (viewport.transform().mode() == TransformMode.GEOMETRY
                    && viewport.meshComponentSelection().size() > 0) {
                var node = viewport.selection().first(core.editorContext().model());
                if (node != null) {
                    viewport.meshComponentSelection().selectBoundaryLoop(node);
                }
                interaction.throughLastIndex = -1;
                interaction.throughLastX = Double.NaN;
                interaction.throughLastY = Double.NaN;
                return true;
            }

            viewport.transform().setMode(TransformMode.GEOMETRY);
            viewport.geometryFaceSelection().clear();
            viewport.meshComponentSelection().clear();
            return true;
        }

        if (keyCode == GLFW.GLFW_KEY_E
                && viewport.transform().mode() == TransformMode.GEOMETRY) {
            if (viewport.meshComponentSelection().mode() == MeshSelectionMode.EDGE) {
                meshModeling.extrudeSelectedEdge(shiftDown ? 1.0 : 0.25);
            } else if (viewport.meshComponentSelection().size() > 1
                    && viewport.meshComponentSelection().mode() == MeshSelectionMode.FACE) {
                meshModeling.extrudeSelectedFaces(shiftDown ? 1.0 : 0.25);
            } else {
                meshModeling.extrudeSelectedFace(shiftDown ? 1.0 : 0.25);
            }
            return true;
        }

        if (keyCode == GLFW.GLFW_KEY_I
                && viewport.transform().mode() == TransformMode.GEOMETRY) {
            if (viewport.meshComponentSelection().mode() == MeshSelectionMode.FACE
                    && viewport.meshComponentSelection().size() > 1) {
                meshModeling.insetSelectedFaces(shiftDown ? 0.5 : 0.25);
            } else {
                meshModeling.insetSelectedFace(shiftDown ? 0.5 : 0.25);
            }
            return true;
        }

        return false;
    }
}
