package whitevoid.create.ui;

import whitevoid.create.core.CreateCore;
import whitevoid.create.editor.geometry.GeometryFace;
import whitevoid.create.editor.transform.TransformMode;
import whitevoid.create.editor.viewport.ViewportContext;
import whitevoid.create.model.ModelNode;

public final class CreateViewportClickController {
    private final CreateTopologyPathInteractionController topologyPathController;
    private final CreateMeshComponentInteractionController meshComponentInteraction;
    private final CreateComponentTransformInteractionController componentTransformInteraction;
    private final CreateTransformGizmoInteractionController transformGizmoInteraction;
    private final CreateComponentBoxSelectionController componentBoxSelection;
    private final CreateViewportNodeSelectionController nodeSelection;
    private final ViewportGizmo gizmo;
    private final CubeFaceEditorController cubeFaceEditor;
    private final CreateViewportInput viewportInput;
    private final ComponentTransformInputController componentTransformInput;

    public CreateViewportClickController(
            CreateTopologyPathInteractionController topologyPathController,
            CreateMeshComponentInteractionController meshComponentInteraction,
            CreateComponentTransformInteractionController componentTransformInteraction,
            CreateTransformGizmoInteractionController transformGizmoInteraction,
            CreateComponentBoxSelectionController componentBoxSelection,
            CreateViewportNodeSelectionController nodeSelection,
            ViewportGizmo gizmo,
            CubeFaceEditorController cubeFaceEditor,
            CreateViewportInput viewportInput,
            ComponentTransformInputController componentTransformInput) {
        this.topologyPathController = topologyPathController;
        this.meshComponentInteraction = meshComponentInteraction;
        this.componentTransformInteraction = componentTransformInteraction;
        this.transformGizmoInteraction = transformGizmoInteraction;
        this.componentBoxSelection = componentBoxSelection;
        this.nodeSelection = nodeSelection;
        this.gizmo = gizmo;
        this.cubeFaceEditor = cubeFaceEditor;
        this.viewportInput = viewportInput;
        this.componentTransformInput = componentTransformInput;
    }

    public boolean handle(
            CreateCore core,
            CreateViewportInteractionState interaction,
            double mouseX,
            double mouseY,
            int button,
            int width,
            int height,
            boolean shiftDown,
            boolean altDown,
            boolean controlDown) {
        if (button == 0 && topologyPathController.handleLeftClick(
                core, interaction, mouseX, mouseY, width, height)) {
            return true;
        }

        if (viewportInput.mouseClicked(mouseX, mouseY, button)) return true;
        if (button != 0) return false;

        ViewportContext viewport = core.editorContext().viewport();
        ModelNode selected = viewport.selection().first(core.editorContext().model());

        if (selected != null && viewport.transform().mode() == TransformMode.GEOMETRY) {
            int cx = width / 2;
            int cy = height / 2;
            ViewportProjector projector = new ViewportProjector(viewport.viewport().camera());

            if (!shiftDown && !altDown
                    && viewport.meshComponentSelection().size() > 0
                    && componentTransformInput.armed()
                && componentTransformInput.constraintAxis() != ComponentTransformGizmo.Axis.NONE) {
                    interaction.componentDragging = componentTransformInteraction.beginKeyboardArmed(
                            selected, viewport, mouseX, mouseY);
                    return true;
                }

                if (componentTransformInteraction.beginFromGizmo(
                        selected, viewport, projector, mouseX, mouseY, cx, cy)) {
                    interaction.componentDragging = true;
                    return true;
                }
            }

            if (meshComponentInteraction.handleClick(
                    core, interaction, selected, viewport, mouseX, mouseY, cx, cy,
                    interaction.selectThrough, altDown, shiftDown, controlDown)) {
                return true;
            }

            GeometryFace clickedFace = gizmo.faceHit(selected, projector, mouseX, mouseY, cx, cy);
            if (clickedFace != GeometryFace.NONE) {
                viewport.geometryFaceSelection().select(selected, clickedFace);
                interaction.hoveredFace = clickedFace;
                cubeFaceEditor.begin(selected, clickedFace);
                return true;
            }
            viewport.geometryFaceSelection().clear();

            interaction.gizmoDragging = interaction.activeAxis != ViewportGizmo.Axis.NONE;
            interaction.hoveredAxis = interaction.activeAxis;
            if (interaction.gizmoDragging) {
                interaction.dragOldGeometry = selected.geometry();
                return true;
            }
        } else if (selected != null && viewport.transform().mode() != TransformMode.SELECT) {
            if (transformGizmoInteraction.begin(
                    core, interaction, selected, viewport, viewport.transform().mode(),
                    mouseX, mouseY, width / 2, height / 2)) {
                return true;
            }
        }

        if (componentBoxSelection.begin(core, interaction, selected, viewport, mouseX, mouseY)) {
            return true;
        }

        return nodeSelection.selectAt(core, viewport, mouseX, mouseY, width, height);
    }
}
