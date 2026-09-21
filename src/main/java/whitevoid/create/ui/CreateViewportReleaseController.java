package whitevoid.create.ui;

import whitevoid.create.core.CreateCore;
import whitevoid.create.editor.viewport.ViewportContext;
import whitevoid.create.model.ModelNode;

public final class CreateViewportReleaseController {
    private final CreateComponentTransformInteractionController componentTransformInteraction;
    private final CreateComponentBoxSelectionController componentBoxSelection;
    private final CreateMeshComponentDragFinishController meshComponentDragFinish;
    private final CubeFaceEditorController cubeFaceEditor;
    private final CreateTransformGizmoInteractionController transformGizmoInteraction;
    private final CreateViewportSelectionController selectionController;
    private final MeshComponentDragController meshComponentDrag;

    public CreateViewportReleaseController(
            CreateComponentTransformInteractionController componentTransformInteraction,
            CreateComponentBoxSelectionController componentBoxSelection,
            CreateMeshComponentDragFinishController meshComponentDragFinish,
            CubeFaceEditorController cubeFaceEditor,
            CreateTransformGizmoInteractionController transformGizmoInteraction,
            CreateViewportSelectionController selectionController,
            MeshComponentDragController meshComponentDrag) {
        this.componentTransformInteraction = componentTransformInteraction;
        this.componentBoxSelection = componentBoxSelection;
        this.meshComponentDragFinish = meshComponentDragFinish;
        this.cubeFaceEditor = cubeFaceEditor;
        this.transformGizmoInteraction = transformGizmoInteraction;
        this.selectionController = selectionController;
        this.meshComponentDrag = meshComponentDrag;
    }

    public boolean handle(
            CreateCore core,
            CreateViewportInteractionState interaction,
            ViewportContext viewport,
            double mouseX,
            double mouseY,
            int button,
            int width,
            int height,
            boolean altDown,
            boolean shiftDown) {
        if (button != 0) return false;

        ModelNode node = viewport.selection().first(core.editorContext().model());

        if (interaction.componentDragging
                && componentTransformInteraction.finish(core, interaction, node, viewport.transform().mode())) {
            return true;
        }

        if (interaction.componentBoxSelecting
                && componentBoxSelection.finish(
                        core, interaction, node, viewport, mouseX, mouseY,
                        width, height, altDown, shiftDown, selectionController)) {
            return true;
        }

        if ((interaction.vertexDragging || interaction.edgeDragging)
                && meshComponentDragFinish.finish(core, interaction, node, meshComponentDrag)) {
            return true;
        }

        if (cubeFaceEditor.dragging()) {
            cubeFaceEditor.finish(core);
            return true;
        }

        if (interaction.gizmoDragging
                && transformGizmoInteraction.finish(core, interaction, node, viewport.transform().mode())) {
            return true;
        }

        return false;
    }
}
