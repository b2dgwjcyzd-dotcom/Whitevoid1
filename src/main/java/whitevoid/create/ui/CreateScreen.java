package whitevoid.create.ui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;
import whitevoid.create.core.CreateCore;
import whitevoid.create.editor.geometry.GeometryFace;
import whitevoid.create.editor.transform.TransformMode;
import whitevoid.create.editor.viewport.ViewportContext;
import whitevoid.create.model.ModelNode;

public final class CreateScreen extends Screen {
    private final CreateCore core;
    private final CreateViewportInteractionState interaction = new CreateViewportInteractionState();

    // Foundational viewport/editor objects first; dependent controllers follow.
    private final ViewportGizmo gizmo = new ViewportGizmo();
    private final ComponentTransformGizmo componentGizmo = new ComponentTransformGizmo();
    private final ComponentTransformController componentTransform = new ComponentTransformController(componentGizmo);
    private final ComponentTransformInputController componentTransformInput = new ComponentTransformInputController(componentTransform);
    private final ComponentTransformMouseController componentTransformMouse = new ComponentTransformMouseController(componentTransform);
    private final MeshEditorController meshEditor = new MeshEditorController(gizmo);
    private final MeshEditorHoverController meshEditorHover = new MeshEditorHoverController(gizmo);
    private final MeshComponentDragController meshComponentDrag = new MeshComponentDragController(gizmo);

    private final CreateViewportSelectionController selectionController = new CreateViewportSelectionController();
    private final CreateTopologyPathInteractionController topologyPathController = new CreateTopologyPathInteractionController(selectionController);
    private final CreateMeshComponentInteractionController meshComponentInteraction =
            new CreateMeshComponentInteractionController(meshEditor, meshComponentDrag);
    private final CreateTransformGizmoInteractionController transformGizmoInteraction =
            new CreateTransformGizmoInteractionController(gizmo);
    private final CreateTransformGizmoDragController transformGizmoDrag =
            new CreateTransformGizmoDragController(gizmo);
    private final CreateVertexDragController vertexDrag = new CreateVertexDragController();
    private final CreateEdgeDragController edgeDrag = new CreateEdgeDragController();
    private final CreateMeshComponentDragFinishController meshComponentDragFinish =
            new CreateMeshComponentDragFinishController();
    private final CreateComponentBoxSelectionController componentBoxSelection =
            new CreateComponentBoxSelectionController();
    private final CreateViewportNodeSelectionController nodeSelection =
            new CreateViewportNodeSelectionController();
    private final CreateViewportHoverController hoverController =
            new CreateViewportHoverController(gizmo, componentGizmo, componentTransform, meshEditorHover);
    private final CreateComponentTransformInteractionController componentTransformInteraction =
            new CreateComponentTransformInteractionController(
                    componentTransformMouse, componentTransformInput, componentTransform);
    private final CreateViewportOverlayRenderer overlayRenderer = new CreateViewportOverlayRenderer();
    private final ViewportRenderer viewportRenderer = new ViewportRenderer();
    private final CreateViewportInput viewportInput;

    private final CreateMeshModelingController meshModeling;
    private final CreateNodeActionController nodeActions;
    private final CreateSelectionHotkeyController selectionHotkeys;
    private final CreateTransformHotkeyController transformHotkeys;
    private final CreateModelingHotkeyController modelingHotkeys;
    private final CreateResetController resetController;
    private final CreateViewportReleaseController viewportReleaseController;
    private final CreateViewportDragController viewportDragController;
    private final CreateViewportClickController viewportClickController;

    private final CubeFaceEditorController cubeFaceEditor;

    public CreateScreen(CreateCore core) {
        super(Text.literal("CREATE"));
        this.core = core;
        this.meshModeling = new CreateMeshModelingController(core, componentGizmo, componentTransform);
        this.nodeActions = new CreateNodeActionController(core);
        this.selectionHotkeys = new CreateSelectionHotkeyController(core, componentTransform);
        this.transformHotkeys = new CreateTransformHotkeyController(core, meshModeling);
        this.modelingHotkeys = new CreateModelingHotkeyController(core, meshModeling);
        this.resetController = new CreateResetController(componentTransformInput, componentTransform);
        this.cubeFaceEditor = new CubeFaceEditorController(gizmo);
        this.viewportReleaseController = new CreateViewportReleaseController(
                componentTransformInteraction,
                componentBoxSelection,
                meshComponentDragFinish,
                cubeFaceEditor,
                transformGizmoInteraction,
                selectionController,
                meshComponentDrag);
        this.viewportInput = new CreateViewportInput(core.editorContext().viewport().viewport().camera());
        this.viewportDragController = new CreateViewportDragController(
                componentTransformInteraction,
                componentBoxSelection,
                vertexDrag,
                edgeDrag,
                cubeFaceEditor,
                transformGizmoDrag,
                viewportInput);
        this.viewportClickController = new CreateViewportClickController(
                topologyPathController,
                meshComponentInteraction,
                componentTransformInteraction,
                transformGizmoInteraction,
                componentBoxSelection,
                nodeSelection,
                gizmo,
                cubeFaceEditor,
                viewportInput,
                componentTransformInput);
    }

    @Override protected void init() {
        core.ensureActiveProject();
        core.editorContext().setEditing(true);
    }

    @Override public void close() {
        viewportInput.cancelDrag();
        componentTransform.cancel();
        core.editorContext().setEditing(false);
        core.saveActiveProject();
        super.close();
    }

    @Override public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        ViewportContext viewport = core.editorContext().viewport();

        if (keyCode == GLFW.GLFW_KEY_ESCAPE && cubeFaceEditor.dragging()) {
            cubeFaceEditor.cancel();
            viewport.geometryFaceSelection().clear();
            return true;
        }

        if (componentTransformInput.armed() && keyCode == GLFW.GLFW_KEY_ESCAPE) {
            resetController.clearArmedTopology(interaction);
        }

        if (componentTransformInput.handleKey(keyCode, viewport, hasShiftDown(),
                interaction.proportionalEditing, interaction.proportionalRadius, core)) {
            return true;
        }

        if (selectionHotkeys.handle(
                interaction, viewport, keyCode, hasShiftDown(), hasAltDown(), hasControlDown())) {
            return true;
        }

        if (modelingHotkeys.handle(
                interaction, viewport, keyCode, hasShiftDown(), hasControlDown())) {
            return true;
        }

        ModelNode node = viewport.selection().first(core.editorContext().model());
        if (transformHotkeys.handle(interaction, viewport, node, keyCode, hasShiftDown(), hasControlDown())) {
            return true;
        }

        if (keyCode == GLFW.GLFW_KEY_N) {
            nodeActions.addCube();
            return true;
        }

        if (keyCode == GLFW.GLFW_KEY_DELETE || keyCode == GLFW.GLFW_KEY_BACKSPACE) {
            nodeActions.deleteSelectedNode();
            return true;
        }

        if (keyCode == GLFW.GLFW_KEY_D && hasControlDown()) {
            nodeActions.duplicateSelectedNode();
            return true;
        }

        if (keyCode == GLFW.GLFW_KEY_LEFT_BRACKET || keyCode == GLFW.GLFW_KEY_RIGHT_BRACKET) {
            nodeActions.resizeSelectedCube(keyCode == GLFW.GLFW_KEY_RIGHT_BRACKET, hasShiftDown());
            return true;
        }

        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            resetController.resetOnEscape(interaction, viewport);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (viewportClickController.handle(
                core,
                interaction,
                mouseX,
                mouseY,
                button,
                width,
                height,
                hasShiftDown(),
                hasAltDown(),
                hasControlDown())) {
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (viewportReleaseController.handle(
                core,
                interaction,
                core.editorContext().viewport(),
                mouseX,
                mouseY,
                button,
                width,
                height,
                hasAltDown(),
                hasShiftDown())) {
            return true;
        }

        if (viewportInput.mouseReleased(mouseX, mouseY, button)) return true;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (viewportDragController.handle(
                core,
                interaction,
                core.editorContext().viewport(),
                mouseX,
                mouseY,
                button,
                deltaX,
                deltaY,
                width,
                height,
                hasShiftDown(),
                hasControlDown())) {
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (viewportInput.mouseScrolled(verticalAmount)) return true;
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override public void mouseMoved(double mouseX, double mouseY) {
        if (!interaction.gizmoDragging && !interaction.componentDragging) {
            ViewportContext viewport = core.editorContext().viewport();
            ModelNode selected = viewport.selection().first(core.editorContext().model());
            hoverController.update(interaction, viewport, selected, mouseX, mouseY, width, height);
        }
        super.mouseMoved(mouseX, mouseY);
    }

    @Override public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context, mouseX, mouseY, delta);
        GeometryFace selectedFace = core.editorContext().viewport().geometryFaceSelection().face();
        viewportRenderer.render(context, width, height, core.editorContext().viewport(),
                core.editorContext().model(), interaction.hoveredAxis, interaction.hoveredFace, selectedFace, interaction.hoveredMeshFace,
                interaction.hoveredMeshVertex, interaction.hoveredMeshEdgeA, interaction.hoveredMeshEdgeB,
                interaction.hoveredComponentAxis, componentTransform.operation(), componentTransform.pivotMode(), interaction.selectThrough);

        overlayRenderer.renderStatus(
                context,
                width,
                height,
                core,
                interaction,
                componentTransform,
                componentTransformInput,
                hasControlDown()
        );
        overlayRenderer.renderComponentSelectionBox(context, interaction);
        super.render(context, mouseX, mouseY, delta);
    }

    @Override public boolean shouldPause() { return false; }
}