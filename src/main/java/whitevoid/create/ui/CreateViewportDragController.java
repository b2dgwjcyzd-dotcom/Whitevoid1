package whitevoid.create.ui;

import whitevoid.create.core.CreateCore;
import whitevoid.create.editor.viewport.ViewportContext;
import whitevoid.create.model.ModelNode;

public final class CreateViewportDragController {
    private final CreateComponentTransformInteractionController componentTransformInteraction;
    private final CreateComponentBoxSelectionController componentBoxSelection;
    private final CreateVertexDragController vertexDrag;
    private final CreateEdgeDragController edgeDrag;
    private final CubeFaceEditorController cubeFaceEditor;
    private final CreateTransformGizmoDragController transformGizmoDrag;
    private final CreateViewportInput viewportInput;

    public CreateViewportDragController(
            CreateComponentTransformInteractionController componentTransformInteraction,
            CreateComponentBoxSelectionController componentBoxSelection,
            CreateVertexDragController vertexDrag,
            CreateEdgeDragController edgeDrag,
            CubeFaceEditorController cubeFaceEditor,
            CreateTransformGizmoDragController transformGizmoDrag,
            CreateViewportInput viewportInput) {
        this.componentTransformInteraction = componentTransformInteraction;
        this.componentBoxSelection = componentBoxSelection;
        this.vertexDrag = vertexDrag;
        this.edgeDrag = edgeDrag;
        this.cubeFaceEditor = cubeFaceEditor;
        this.transformGizmoDrag = transformGizmoDrag;
        this.viewportInput = viewportInput;
    }

    public boolean handle(
            CreateCore core,
            CreateViewportInteractionState interaction,
            ViewportContext viewport,
            double mouseX,
            double mouseY,
            int button,
            double deltaX,
            double deltaY,
            int width,
            int height,
            boolean shiftDown,
            boolean controlDown) {
        if (button == 0 && interaction.componentDragging) {
            ModelNode node = viewport.selection().first(core.editorContext().model());
            if (node != null) {
                componentTransformInteraction.update(
                        node,
                        viewport,
                        mouseX,
                        mouseY,
                        width,
                        height,
                        interaction.proportionalEditing,
                        interaction.proportionalRadius,
                        controlDown);
            }
            return true;
        }

        if (button == 0 && interaction.componentBoxSelecting) {
            return componentBoxSelection.update(interaction, mouseX, mouseY);
        }

        if (button == 0 && interaction.vertexDragging) {
            ModelNode node = viewport.selection().first(core.editorContext().model());
            if (vertexDrag.update(interaction, node, viewport, deltaX, deltaY)) {
                return true;
            }
        }

        if (button == 0 && interaction.edgeDragging) {
            ModelNode node = viewport.selection().first(core.editorContext().model());
            if (edgeDrag.update(interaction, node, viewport, deltaX, deltaY)) {
                return true;
            }
        }

        if (cubeFaceEditor.dragging()) {
            cubeFaceEditor.update(core, deltaX, deltaY);
            return true;
        }

        if (button == 0 && interaction.gizmoDragging) {
            ModelNode node = viewport.selection().first(core.editorContext().model());
            if (transformGizmoDrag.update(core, interaction, node, viewport, deltaX, deltaY)) {
                return true;
            }
        }

        return viewportInput.mouseDragged(mouseX, mouseY, button, shiftDown);
    }
}
