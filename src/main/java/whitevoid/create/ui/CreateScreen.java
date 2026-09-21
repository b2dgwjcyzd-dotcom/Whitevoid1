package whitevoid.create.ui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;
import whitevoid.create.core.CreateCore;
import whitevoid.create.core.history.commands.AddCubeCommand;
import whitevoid.create.core.history.commands.DeleteNodeCommand;
import whitevoid.create.core.history.commands.DuplicateNodeCommand;
import whitevoid.create.core.history.commands.SetTransformCommand;
import whitevoid.create.core.history.commands.SetCubeGeometryCommand;
import whitevoid.create.core.history.commands.ResizeCubeFaceCommand;
import whitevoid.create.core.history.commands.SetMeshGeometryCommand;
import whitevoid.create.editor.geometry.MeshOperations;
import whitevoid.create.editor.geometry.MeshComponentTransforms;
import whitevoid.create.editor.geometry.MeshComponentSnapper;
import whitevoid.create.editor.geometry.MeshSelectionMode;
import whitevoid.create.editor.selection.SelectionMode;
import whitevoid.create.editor.geometry.GeometryFace;
import whitevoid.create.editor.transform.TransformMode;
import whitevoid.create.editor.viewport.ViewportContext;
import whitevoid.create.model.CubeGeometry;
import whitevoid.create.model.ModelNode;
import whitevoid.create.model.MeshGeometry;
import whitevoid.create.model.TransformMath;

public final class CreateScreen extends Screen {
    private final CreateCore core;
    private final CreateViewportInteractionState interaction = new CreateViewportInteractionState();
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
    private final CreateMeshModelingController meshModeling;
    private final CreateViewportHoverController hoverController =
            new CreateViewportHoverController(gizmo, componentGizmo, componentTransform, meshEditorHover);
    private final ViewportRenderer viewportRenderer = new ViewportRenderer();
    private final CreateViewportInput viewportInput;
    private final ViewportGizmo gizmo = new ViewportGizmo();
    private final ComponentTransformGizmo componentGizmo = new ComponentTransformGizmo();
    private final ComponentTransformController componentTransform = new ComponentTransformController(componentGizmo);
    private final ComponentTransformInputController componentTransformInput = new ComponentTransformInputController(componentTransform);
    private final ComponentTransformMouseController componentTransformMouse = new ComponentTransformMouseController(componentTransform);
    private final CreateComponentTransformInteractionController componentTransformInteraction =
            new CreateComponentTransformInteractionController(componentTransformMouse, componentTransformInput, componentTransform);
    private final MeshEditorController meshEditor = new MeshEditorController(gizmo);
    private final MeshEditorHoverController meshEditorHover = new MeshEditorHoverController(gizmo);
    private final MeshComponentDragController meshComponentDrag = new MeshComponentDragController(gizmo);
    private final CreateViewportOverlayRenderer overlayRenderer = new CreateViewportOverlayRenderer();

    private final CubeFaceEditorController cubeFaceEditor;

    private static final double MOVE_SNAP_INCREMENT = 0.25;
    private static final double ROTATE_SNAP_INCREMENT = 5.0;
    private static final double SCALE_SNAP_INCREMENT = 0.05;

    public CreateScreen(CreateCore core) {
        super(Text.literal("CREATE"));
        this.core = core;
        this.meshModeling = new CreateMeshModelingController(core, componentGizmo, componentTransform);
        this.viewportInput = new CreateViewportInput(core.editorContext().viewport().viewport().camera());
    }

    @Override protected void init() {
        core.editorContext().setEditing(true);
    }

    @Override public void close() {
        viewportInput.cancelDrag();
        componentTransform.cancel();
        core.editorContext().setEditing(false);
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
            interaction.topologyPathPickArmed = false;
            interaction.topologyPathSecondPick = false;
            interaction.topologyPathHasStart = false;
            interaction.topologyPathStartIndex = -1;
        }

        if (componentTransformInput.handleKey(keyCode, viewport, hasShiftDown(),
                interaction.proportionalEditing, interaction.proportionalRadius, core)) {
            return true;
        }

if (keyCode == 65 && viewport.transform().mode() == TransformMode.GEOMETRY) {
            var node = viewport.selection().first(core.editorContext().model());
            if (node != null) {
                if (hasAltDown()) viewport.meshComponentSelection().clear();
                else if (hasControlDown()) viewport.meshComponentSelection().invert(node);
                else viewport.meshComponentSelection().selectAll(node);
            }
            return true;
        }

        if (viewport.transform().mode() == TransformMode.GEOMETRY && viewport.meshComponentSelection().size() > 0) {
            ComponentTransformGizmo.Axis requested = switch (keyCode) {
                case GLFW.GLFW_KEY_X -> ComponentTransformGizmo.Axis.X;
                case GLFW.GLFW_KEY_Y -> ComponentTransformGizmo.Axis.Y;
                case GLFW.GLFW_KEY_Z -> ComponentTransformGizmo.Axis.Z;
                default -> ComponentTransformGizmo.Axis.NONE;
            };
            if (requested != ComponentTransformGizmo.Axis.NONE && componentTransformInput.armed()) {
                if (componentTransform.constraintAxis() == requested) {
                    componentTransform.setConstraintMode(hasShiftDown() ? ComponentTransformController.ConstraintMode.PLANE : ComponentTransformController.ConstraintMode.AXIS);
                    if (!hasShiftDown()) {
                        componentTransform.clearConstraint();
                        componentTransform.setConstraintMode(ComponentTransformController.ConstraintMode.AXIS);
                    }
                } else {
                    componentTransform.setConstraintAxis(requested);
                    componentTransform.setConstraintMode(hasShiftDown() ? ComponentTransformController.ConstraintMode.PLANE : ComponentTransformController.ConstraintMode.AXIS);
                }
                return true;
            }
        }
        if (keyCode == 80 && viewport.transform().mode() == TransformMode.GEOMETRY && viewport.meshComponentSelection().size() > 0) {
            componentTransform.cyclePivotMode();
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_1 && viewport.transform().mode() == TransformMode.GEOMETRY) {
            setMeshSelectionMode(MeshSelectionMode.VERTEX);
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_2 && viewport.transform().mode() == TransformMode.GEOMETRY) {
            setMeshSelectionMode(MeshSelectionMode.EDGE);
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_3 && viewport.transform().mode() == TransformMode.GEOMETRY) {
            setMeshSelectionMode(MeshSelectionMode.FACE);
            return true;
        }

        // Mirror is a two-step operation: M arms it, then X/Y/Z chooses the axis.
        if (keyCode == GLFW.GLFW_KEY_M && viewport.transform().mode() == TransformMode.GEOMETRY
                && viewport.meshComponentSelection().size() > 0) {
            interaction.mirrorArmed = true;
            return true;
        }
        if (interaction.mirrorArmed && viewport.transform().mode() == TransformMode.GEOMETRY
                && viewport.meshComponentSelection().size() > 0) {
            int axis = switch (keyCode) {
                case GLFW.GLFW_KEY_X -> 0;
                case GLFW.GLFW_KEY_Y -> 1;
                case GLFW.GLFW_KEY_Z -> 2;
                default -> -1;
            };
            if (axis >= 0) {
                interaction.mirrorAxis = axis == 0 ? ComponentTransformGizmo.Axis.X
                        : axis == 1 ? ComponentTransformGizmo.Axis.Y : ComponentTransformGizmo.Axis.Z;
                meshModeling.mirrorSelectedComponents(axis);
                interaction.mirrorArmed = false;
                return true;
            }
        }

        if (keyCode == GLFW.GLFW_KEY_B && hasControlDown()
                && viewport.transform().mode() == TransformMode.GEOMETRY
                && viewport.meshComponentSelection().mode() == MeshSelectionMode.EDGE) {
            meshModeling.bevelSelectedEdge(hasShiftDown() ? 1.0 : 0.25);
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_B
                && viewport.transform().mode() == TransformMode.GEOMETRY
                && viewport.meshComponentSelection().size() > 0) {
            var boundaryNode = viewport.selection().first(core.editorContext().model());
            if (boundaryNode != null) viewport.meshComponentSelection().selectBoundaryLoop(boundaryNode);
            resetThroughCycle();
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_B) {
            viewport.transform().setMode(TransformMode.GEOMETRY);
            viewport.geometryFaceSelection().clear();
            viewport.meshComponentSelection().clear();
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_E && viewport.transform().mode() == TransformMode.GEOMETRY) {
            if (viewport.meshComponentSelection().mode() == MeshSelectionMode.EDGE) {
                meshModeling.extrudeSelectedEdge(hasShiftDown() ? 1.0 : 0.25);
            } else if (viewport.meshComponentSelection().size() > 1
                    && viewport.meshComponentSelection().mode() == MeshSelectionMode.FACE) {
                meshModeling.extrudeSelectedFaces(hasShiftDown() ? 1.0 : 0.25);
            } else {
                meshModeling.extrudeSelectedFace(hasShiftDown() ? 1.0 : 0.25);
            }
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_I && viewport.transform().mode() == TransformMode.GEOMETRY) {
            if (viewport.meshComponentSelection().mode() == MeshSelectionMode.FACE
                    && viewport.meshComponentSelection().size() > 1) {
                meshModeling.insetSelectedFaces(hasShiftDown() ? 0.5 : 0.25);
            } else {
                meshModeling.insetSelectedFace(hasShiftDown() ? 0.5 : 0.25);
            }
            return true;
        }

        // Topology path selection: V = arm a second vertex pick, B = boundary.
if (keyCode == GLFW.GLFW_KEY_V && viewport.transform().mode() == TransformMode.GEOMETRY
        && viewport.meshComponentSelection().mode() == MeshSelectionMode.VERTEX
        && viewport.meshComponentSelection().size() > 0) {
    interaction.topologyPathPickArmed = true;
    interaction.topologyPathSecondPick = true;
    return true;
}
// Topology traversal: U = loop, K = ring.
if (keyCode == GLFW.GLFW_KEY_U && viewport.transform().mode() == TransformMode.GEOMETRY
        && viewport.meshComponentSelection().size() > 0) {
    var selection = viewport.meshComponentSelection();
    var selected = viewport.selection().first(core.editorContext().model());
    if (selected != null) {
        if (selection.mode() == MeshSelectionMode.EDGE && selection.activeEdgeA() >= 0) {
            selection.selectEdgeLoop(selected, selection.activeEdgeA(), selection.activeEdgeB());
        } else if (selection.mode() == MeshSelectionMode.FACE && selection.activeFace() >= 0) {
            selection.selectFaceLoop(selected, selection.activeFace());
        }
    }
    return true;
}
if (keyCode == GLFW.GLFW_KEY_K && viewport.transform().mode() == TransformMode.GEOMETRY
        && viewport.meshComponentSelection().size() > 0) {
    var selection = viewport.meshComponentSelection();
    var selected = viewport.selection().first(core.editorContext().model());
    if (selected != null) {
        if (selection.mode() == MeshSelectionMode.EDGE && selection.activeEdgeA() >= 0) {
            selection.selectEdgeRing(selected, selection.activeEdgeA(), selection.activeEdgeB());
        } else if (selection.mode() == MeshSelectionMode.FACE && selection.activeFace() >= 0) {
            selection.selectFaceRing(selected, selection.activeFace());
        }
    }
    return true;
}

// Selection expansion/contraction.
if (keyCode == GLFW.GLFW_KEY_PERIOD && viewport.transform().mode() == TransformMode.GEOMETRY
        && viewport.meshComponentSelection().size() > 0) {
    var selected = viewport.selection().first(core.editorContext().model());
    if (selected != null) viewport.meshComponentSelection().extend(selected);
    return true;
}
if (keyCode == GLFW.GLFW_KEY_COMMA && viewport.transform().mode() == TransformMode.GEOMETRY
        && viewport.meshComponentSelection().size() > 0) {
    var selected = viewport.selection().first(core.editorContext().model());
    if (selected != null) viewport.meshComponentSelection().shrink(selected);
    return true;
}

        if (keyCode == 90 && hasControlDown()) {
            if (hasShiftDown()) viewportContextHistoryRedo();
            else viewportContextHistoryUndo();
            return true;
        }
        if (keyCode == 89 && hasControlDown()) {
            viewportContextHistoryRedo();
            return true;
        }

        if (keyCode == GLFW.GLFW_KEY_T && viewport.transform().mode() == TransformMode.GEOMETRY) {
            interaction.selectThrough = !interaction.selectThrough;
            interaction.throughLastIndex = -1;
            interaction.throughLastX = Double.NaN;
            interaction.throughLastY = Double.NaN;
            return true;
        }

        if (keyCode == GLFW.GLFW_KEY_N) {
            addCube();
            return true;
        }

        if (keyCode == GLFW.GLFW_KEY_DELETE || keyCode == GLFW.GLFW_KEY_BACKSPACE) {
            deleteSelectedNode();
            return true;
        }

        if (keyCode == GLFW.GLFW_KEY_D && hasControlDown()) {
            duplicateSelectedNode();
            return true;
        }

        if (keyCode == GLFW.GLFW_KEY_LEFT_BRACKET || keyCode == GLFW.GLFW_KEY_RIGHT_BRACKET) {
            resizeSelectedCube(keyCode == GLFW.GLFW_KEY_RIGHT_BRACKET);
            return true;
        }

        ModelNode node = viewport.selection().first(core.editorContext().model());
        if (node != null) {
            if (viewport.transform().mode() == TransformMode.GEOMETRY
                    && viewport.meshComponentSelection().matches(node)
                    && (viewport.meshComponentSelection().mode() == MeshSelectionMode.VERTEX
                        || viewport.meshComponentSelection().mode() == MeshSelectionMode.EDGE)) {
                double step = hasShiftDown() ? 0.1 : 0.25;
                if (keyCode == GLFW.GLFW_KEY_LEFT) { meshModeling.moveSelectedComponents(-step, 0, 0); return true; }
                if (keyCode == GLFW.GLFW_KEY_RIGHT) { meshModeling.moveSelectedComponents(step, 0, 0); return true; }
                if (keyCode == GLFW.GLFW_KEY_DOWN) { meshModeling.moveSelectedComponents(0, 0, step); return true; }
                if (keyCode == GLFW.GLFW_KEY_UP) { meshModeling.moveSelectedComponents(0, 0, -step); return true; }
                if (keyCode == GLFW.GLFW_KEY_SPACE) { meshModeling.moveSelectedComponents(0, step, 0); return true; }
            }
            double step = hasShiftDown() ? 0.1 : 1.0;
            if (viewport.transform().mode() == TransformMode.MOVE) {
                if (keyCode == 263) { transformMove(node, -step, 0, 0); return true; }
                if (keyCode == 262) { transformMove(node, step, 0, 0); return true; }
                if (keyCode == 264) { transformMove(node, 0, 0, step); return true; }
                if (keyCode == 265) { transformMove(node, 0, 0, -step); return true; }
                if (keyCode == 32) { transformMove(node, 0, step, 0); return true; }
            }
            if (viewport.transform().mode() == TransformMode.ROTATE) {
                if (keyCode == 263) { transformRotate(node, 0, -5, 0); return true; }
                if (keyCode == 262) { transformRotate(node, 0, 5, 0); return true; }
                if (keyCode == 264) { transformRotate(node, -5, 0, 0); return true; }
                if (keyCode == 265) { transformRotate(node, 5, 0, 0); return true; }
            }
            if (viewport.transform().mode() == TransformMode.SCALE) {
                if (keyCode == 263 || keyCode == 264) { transformScale(node, -0.1, -0.1, -0.1); return true; }
                if (keyCode == 262 || keyCode == 265) { transformScale(node, 0.1, 0.1, 0.1); return true; }
            }
        }

        if (keyCode == 27) { resetThroughCycle(); componentTransformInput.disarm(); componentTransform.setAxis(ComponentTransformGizmo.Axis.NONE); interaction.mirrorArmed=false; viewport.transform().setMode(TransformMode.SELECT); }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private void setMeshSelectionMode(MeshSelectionMode mode) {
        var selection = core.editorContext().viewport().meshComponentSelection();
        selection.clear();
        resetThroughCycle();
        selection.setMode(mode);
    }

    private void addCube() {
        var model = core.editorContext().model();
        var selected = core.editorContext().viewport().selection().first(model);

        double x = 0.0;
        double y = 0.0;
        double z = 0.0;
        if (selected != null) {
            x = selected.transform().x() + 3.0;
            y = selected.transform().y();
            z = selected.transform().z();
        }

        var command = new AddCubeCommand(
                model,
                model.root(),
                "Cube",
                new CubeGeometry(2.0, 2.0, 2.0),
                x, y, z
        );
        core.editorContext().history().execute(command);
        core.editorContext().viewport().selection().select(
                command.createdNode(),
                SelectionMode.SINGLE
        );
        core.editorContext().viewport().transform().setMode(TransformMode.SELECT);
    }

    private void deleteSelectedNode() {
        var model = core.editorContext().model();
        var node = core.editorContext().viewport().selection().first(model);
        if (node == null || node == model.root()) return;

        core.editorContext().history().execute(new DeleteNodeCommand(model, node));
        core.editorContext().viewport().selection().clear();
        core.editorContext().viewport().transform().setMode(TransformMode.SELECT);
    }

    private void duplicateSelectedNode() {
        var model = core.editorContext().model();
        var node = core.editorContext().viewport().selection().first(model);
        if (node == null || node == model.root()) return;

        var command = new DuplicateNodeCommand(model, node);
        core.editorContext().history().execute(command);
        core.editorContext().viewport().selection().select(
                command.duplicatedNode(),
                SelectionMode.SINGLE
        );
    }

    private void resizeSelectedCube(boolean grow) {
        var node = core.editorContext().viewport().selection().first(core.editorContext().model());
        if (node == null || node.geometry() == null) return;

        var g = node.geometry();
        double step = hasShiftDown() ? 0.25 : 1.0;
        double factor = grow ? step : -step;

        double width = Math.max(0.1, g.width() + factor);
        double height = Math.max(0.1, g.height() + factor);
        double depth = Math.max(0.1, g.depth() + factor);

        core.editorContext().history().execute(
                new SetCubeGeometryCommand(node, new CubeGeometry(width, height, depth))
        );
    }

    private void transformMove(ModelNode node, double dx, double dy, double dz) {
        var t=node.transform();
        core.editorContext().history().execute(new SetTransformCommand(node,
                t.x()+dx,t.y()+dy,t.z()+dz,t.rotationX(),t.rotationY(),t.rotationZ(),t.scaleX(),t.scaleY(),t.scaleZ()));
    }

    private void transformRotate(ModelNode node, double dx, double dy, double dz) {
        var t=node.transform();
        core.editorContext().history().execute(new SetTransformCommand(node,
                t.x(),t.y(),t.z(),t.rotationX()+dx,t.rotationY()+dy,t.rotationZ()+dz,t.scaleX(),t.scaleY(),t.scaleZ()));
    }

    private void transformScale(ModelNode node, double dx, double dy, double dz) {
        var t=node.transform();
        core.editorContext().history().execute(new SetTransformCommand(node,
                t.x(),t.y(),t.z(),t.rotationX(),t.rotationY(),t.rotationZ(),
                Math.max(.01,t.scaleX()+dx),Math.max(.01,t.scaleY()+dy),Math.max(.01,t.scaleZ()+dz)));
    }


    private void viewportContextHistoryUndo() { core.editorContext().history().undo(); }
    private void viewportContextHistoryRedo() { core.editorContext().history().redo(); }

    @Override public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && topologyPathController.handleLeftClick(
                core, interaction, mouseX, mouseY, width, height)) {
            return true;
        }

        if (viewportInput.mouseClicked(mouseX, mouseY, button)) return true;
        if (button == 0) {
            ViewportContext viewport = core.editorContext().viewport();
            ModelNode selected = viewport.selection().first(core.editorContext().model());
            if (selected != null && viewport.transform().mode() == TransformMode.GEOMETRY) {
                int cx=width/2, cy=height/2;
                ViewportProjector projector = new ViewportProjector(viewport.viewport().camera());
                var meshMode = viewport.meshComponentSelection().mode();

                if (!hasShiftDown() && !hasAltDown() && viewport.meshComponentSelection().size() > 0
                        && componentTransformInput.armed()
                        && componentTransform.constraintAxis() != ComponentTransformGizmo.Axis.NONE) {
                    interaction.componentDragging = componentTransformInteraction.beginKeyboardArmed(
                            selected, viewport, mouseX, mouseY);
                    return true;
                }
                if (!hasShiftDown() && !hasAltDown() && viewport.meshComponentSelection().size() > 0) {
                    if (componentTransformInteraction.beginFromGizmo(
                            selected, viewport, projector, mouseX, mouseY, cx, cy)) {
                        interaction.componentDragging = true;
                        return true;
                    }
                }
                if (meshComponentInteraction.handleClick(
                        core, interaction, selected, viewport, mouseX, mouseY, cx, cy,
                        interaction.selectThrough, hasAltDown(), hasShiftDown(), hasControlDown())) {
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
                }
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
            // Empty-space drag in geometry mode starts component box selection.
            if (componentBoxSelection.begin(core, interaction, selected, viewport, mouseX, mouseY)) {
                return true;
            }

            return nodeSelection.selectAt(
                    core, core.editorContext().viewport(), mouseX, mouseY, width, height);
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (interaction.componentDragging && button == 0) {
            ModelNode node = core.editorContext().viewport().selection().first(core.editorContext().model());
            if (componentTransformInteraction.finish(
                    core, interaction, node, core.editorContext().viewport().transform().mode())) {
                return true;
            }
        }
        if (interaction.componentBoxSelecting && button == 0) {
            if (componentBoxSelection.finish(
                    core, interaction,
                    core.editorContext().viewport().selection().first(core.editorContext().model()),
                    core.editorContext().viewport(),
                    mouseX, mouseY, width, height, hasAltDown(), hasShiftDown(),
                    selectionController)) {
                return true;
            }
        }
        if ((interaction.vertexDragging || interaction.edgeDragging) && button == 0) {
            ModelNode node = core.editorContext().viewport().selection().first(core.editorContext().model());
            if (meshComponentDragFinish.finish(core, interaction, node, meshComponentDrag)) {
                return true;
            }
        }
        if (cubeFaceEditor.dragging() && button == 0) {
            cubeFaceEditor.finish(core);
            return true;
        }
        if (interaction.gizmoDragging && button == 0) {
            ModelNode node = core.editorContext().viewport().selection().first(core.editorContext().model());
            if (transformGizmoInteraction.finish(
                    core, interaction, node, core.editorContext().viewport().transform().mode())) {
                return true;
            }
        }
        if (viewportInput.mouseReleased(mouseX, mouseY, button)) return true;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (interaction.componentDragging && button == 0) {
            ModelNode node = core.editorContext().viewport().selection().first(core.editorContext().model());
            if (node != null) {
                componentTransformInteraction.update(
                        node,
                        core.editorContext().viewport(),
                        mouseX,
                        mouseY,
                        width,
                        height,
                        interaction.proportionalEditing,
                        interaction.proportionalRadius,
                        hasControlDown()
                );
            }
            return true;
        }
        if (interaction.componentBoxSelecting && button == 0) {
            if (componentBoxSelection.update(interaction, mouseX, mouseY)) return true;
        }
        if (interaction.vertexDragging && button == 0) {
            ModelNode node = core.editorContext().viewport().selection().first(core.editorContext().model());
            if (vertexDrag.update(
                    interaction, node, core.editorContext().viewport(), deltaX, deltaY)) {
                return true;
            }
        }
        if (interaction.edgeDragging && button == 0) {
            ModelNode node = core.editorContext().viewport().selection().first(core.editorContext().model());
            if (edgeDrag.update(
                    interaction, node, core.editorContext().viewport(), deltaX, deltaY)) {
                return true;
            }
        }
        if (cubeFaceEditor.dragging()) {
            cubeFaceEditor.update(core, deltaX, deltaY);
            return true;
        }
        if (interaction.gizmoDragging && button == 0) {
            ModelNode node = core.editorContext().viewport().selection().first(core.editorContext().model());
            if (transformGizmoDrag.update(
                    core, interaction, node, core.editorContext().viewport(), deltaX, deltaY)) {
                return true;
            }
        }
        if (viewportInput.mouseDragged(mouseX, mouseY, button, hasShiftDown())) return true;
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

    private static double snapScalar(double value, double increment) {
        if (increment <= 0.0) return value;
        return Math.rint(value / increment) * increment;
    }

    private static double snapScaleFactor(double factor, double increment) {
        if (increment <= 0.0) return Math.max(0.01, factor);
        double delta = factor - 1.0;
        return Math.max(0.01, 1.0 + Math.rint(delta / increment) * increment);
    }

    @Override public boolean shouldPause() { return false; }
}