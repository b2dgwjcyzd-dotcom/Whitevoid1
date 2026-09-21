package whitevoid.create.ui;

import org.lwjgl.glfw.GLFW;
import whitevoid.create.core.CreateCore;
import whitevoid.create.editor.transform.TransformMode;
import whitevoid.create.editor.viewport.ViewportContext;

public final class CreateResetController {
    private final ComponentTransformInputController componentTransformInput;
    private final ComponentTransformController componentTransform;

    public CreateResetController(
            ComponentTransformInputController componentTransformInput,
            ComponentTransformController componentTransform) {
        this.componentTransformInput = componentTransformInput;
        this.componentTransform = componentTransform;
    }

    public boolean handleEscape(
            CreateViewportInteractionState interaction,
            ViewportContext viewport) {
        if (componentTransformInput.armed()) {
            interaction.topologyPathPickArmed = false;
            interaction.topologyPathSecondPick = false;
            interaction.topologyPathHasStart = false;
            interaction.topologyPathStartIndex = -1;
            return false;
        }

        reset(interaction, viewport);
        return true;
    }

    public void reset(
            CreateViewportInteractionState interaction,
            ViewportContext viewport) {
        resetThroughCycle(interaction);
        componentTransformInput.disarm();
        componentTransform.setAxis(ComponentTransformGizmo.Axis.NONE);
        interaction.mirrorArmed = false;
        interaction.gizmoDragging = false;
        interaction.componentDragging = false;
        interaction.vertexDragging = false;
        interaction.edgeDragging = false;
        interaction.componentBoxSelecting = false;
        interaction.topologyPathPickArmed = false;
        interaction.topologyPathSecondPick = false;
        interaction.topologyPathHasStart = false;
        interaction.topologyPathStartIndex = -1;
        interaction.activeVertex = -1;
        interaction.activeEdgeA = -1;
        interaction.activeEdgeB = -1;
        viewport.geometryFaceSelection().clear();
        viewport.meshComponentSelection().clear();
        viewport.transform().setMode(TransformMode.SELECT);
    }

    private static void resetThroughCycle(CreateViewportInteractionState interaction) {
        interaction.throughLastIndex = -1;
        interaction.throughLastX = Double.NaN;
        interaction.throughLastY = Double.NaN;
    }
}
