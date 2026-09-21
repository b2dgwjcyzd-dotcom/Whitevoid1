package whitevoid.create.ui;

import org.lwjgl.glfw.GLFW;
import whitevoid.create.core.CreateCore;
import whitevoid.create.editor.geometry.MeshSelectionMode;
import whitevoid.create.editor.transform.TransformMode;
import whitevoid.create.editor.viewport.ViewportContext;

public final class CreateSelectionHotkeyController {
    private final CreateCore core;
    private final ComponentTransformController componentTransform;

    public CreateSelectionHotkeyController(CreateCore core, ComponentTransformController componentTransform) {
        this.core = core;
        this.componentTransform = componentTransform;
    }

    public boolean handle(
            CreateViewportInteractionState interaction,
            ViewportContext viewport,
            int keyCode,
            boolean shiftDown,
            boolean altDown,
            boolean controlDown
    ) {
        if (viewport.transform().mode() != TransformMode.GEOMETRY) return false;

        var selection = viewport.meshComponentSelection();

        if (keyCode == GLFW.GLFW_KEY_A) {
            var node = viewport.selection().first(core.editorContext().model());
            if (node != null) {
                if (altDown) selection.clear();
                else if (controlDown) selection.invert(node);
                else selection.selectAll(node);
            }
            return true;
        }

        if (keyCode == GLFW.GLFW_KEY_P && selection.size() > 0) {
            componentTransform.cyclePivotMode();
            return true;
        }

        if (keyCode == GLFW.GLFW_KEY_1) return setMode(interaction, viewport, MeshSelectionMode.VERTEX);
        if (keyCode == GLFW.GLFW_KEY_2) return setMode(interaction, viewport, MeshSelectionMode.EDGE);
        if (keyCode == GLFW.GLFW_KEY_3) return setMode(interaction, viewport, MeshSelectionMode.FACE);

        if (keyCode == GLFW.GLFW_KEY_T) {
            interaction.selectThrough = !interaction.selectThrough;
            resetThroughCycle(interaction);
            return true;
        }

        if (keyCode == GLFW.GLFW_KEY_V
                && selection.mode() == MeshSelectionMode.VERTEX
                && selection.size() > 0) {
            interaction.topologyPathPickArmed = true;
            interaction.topologyPathSecondPick = true;
            return true;
        }

        if (keyCode == GLFW.GLFW_KEY_U && selection.size() > 0) {
            var node = viewport.selection().first(core.editorContext().model());
            if (node != null) {
                if (selection.mode() == MeshSelectionMode.EDGE && selection.activeEdgeA() >= 0) {
                    selection.selectEdgeLoop(node, selection.activeEdgeA(), selection.activeEdgeB());
                } else if (selection.mode() == MeshSelectionMode.FACE && selection.activeFace() >= 0) {
                    selection.selectFaceLoop(node, selection.activeFace());
                }
            }
            return true;
        }

        if (keyCode == GLFW.GLFW_KEY_K && selection.size() > 0) {
            var node = viewport.selection().first(core.editorContext().model());
            if (node != null) {
                if (selection.mode() == MeshSelectionMode.EDGE && selection.activeEdgeA() >= 0) {
                    selection.selectEdgeRing(node, selection.activeEdgeA(), selection.activeEdgeB());
                } else if (selection.mode() == MeshSelectionMode.FACE && selection.activeFace() >= 0) {
                    selection.selectFaceRing(node, selection.activeFace());
                }
            }
            return true;
        }

        if (keyCode == GLFW.GLFW_KEY_PERIOD && selection.size() > 0) {
            var node = viewport.selection().first(core.editorContext().model());
            if (node != null) selection.extend(node);
            return true;
        }

        if (keyCode == GLFW.GLFW_KEY_COMMA && selection.size() > 0) {
            var node = viewport.selection().first(core.editorContext().model());
            if (node != null) selection.shrink(node);
            return true;
        }

        return false;
    }

    private boolean setMode(
            CreateViewportInteractionState interaction,
            ViewportContext viewport,
            MeshSelectionMode mode
    ) {
        var selection = viewport.meshComponentSelection();
        selection.clear();
        resetThroughCycle(interaction);
        interaction.topologyPathPickArmed = false;
        interaction.topologyPathSecondPick = false;
        interaction.topologyPathHasStart = false;
        interaction.topologyPathStartIndex = -1;
        selection.setMode(mode);
        return true;
    }

    private static void resetThroughCycle(CreateViewportInteractionState interaction) {
        interaction.throughLastIndex = -1;
        interaction.throughLastX = Double.NaN;
        interaction.throughLastY = Double.NaN;
    }
}
