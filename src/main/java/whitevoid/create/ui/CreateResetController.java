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

    public void clearArmedTopology(CreateViewportInteractionState interaction) {
        if (!componentTransformInput.armed()) return;
        interaction.topologyPathPickArmed = false;
        interaction.topologyPathSecondPick = false;
        interaction.topologyPathHasStart = false;
        interaction.topologyPathStartIndex = -1;
    }

    public boolean resetOnEscape(CreateCore core, CreateViewportInteractionState interaction, ViewportContext viewport) {
        reset(core, interaction, viewport);
        return true;
    }

    public void reset(
            CreateCore core,
            CreateViewportInteractionState interaction,
            ViewportContext viewport) {
        if (componentTransform.dragging()) {
            ModelNode node = viewport.selection().first(core.editorContext().model());
            componentTransform.abort(core, node);
        }
        resetThroughCycle(interaction);
        componentTransformInput.disarm();
        componentTransform.setAxis(ComponentTransformGizmo.Axis.NONE);
        interaction.mirrorArmed = false;
        interaction.topologyPathPickArmed = false;
        interaction.topologyPathSecondPick = false;
        interaction.topologyPathHasStart = false;
        interaction.topologyPathStartIndex = -1;
        interaction.activeVertex = -1;
        interaction.activeEdgeA = -1;
        interaction.activeEdgeB = -1;
        viewport.transform().setMode(TransformMode.SELECT);
    }

    private static void resetThroughCycle(CreateViewportInteractionState interaction) {
        interaction.throughLastIndex = -1;
        interaction.throughLastX = Double.NaN;
        interaction.throughLastY = Double.NaN;
    }
}
