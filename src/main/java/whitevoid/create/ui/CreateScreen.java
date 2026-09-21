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
    private final ViewportRenderer viewportRenderer = new ViewportRenderer();
    private final CreateViewportInput viewportInput;
    private final ViewportGizmo gizmo = new ViewportGizmo();
    private final ComponentTransformGizmo componentGizmo = new ComponentTransformGizmo();
    private final ComponentTransformController componentTransform = new ComponentTransformController(componentGizmo);
    private final MeshEditorController meshEditor = new MeshEditorController(gizmo);
    private final MeshEditorHoverController meshEditorHover = new MeshEditorHoverController(gizmo);
    private final MeshComponentDragController meshComponentDrag = new MeshComponentDragController(gizmo);
    private ViewportGizmo.Axis activeAxis = ViewportGizmo.Axis.NONE;
    private boolean gizmoDragging;
    private ViewportGizmo.Axis hoveredAxis = ViewportGizmo.Axis.NONE;
    private double dragOldX,dragOldY,dragOldZ,dragOldRx,dragOldRy,dragOldRz,dragOldSx,dragOldSy,dragOldSz;
    private CubeGeometry dragOldGeometry;
    private GeometryFace hoveredFace = GeometryFace.NONE;
    private final CubeFaceEditorController cubeFaceEditor;
    private int hoveredMeshFace = -1;
    private int hoveredMeshVertex = -1;
    private int hoveredMeshEdgeA = -1;
    private int hoveredMeshEdgeB = -1;
    private boolean vertexDragging;
    private int activeVertex = -1;
    private MeshGeometry vertexDragOldMesh;
    private boolean edgeDragging;
    private int activeEdgeA = -1;
    private int activeEdgeB = -1;
    private MeshGeometry edgeDragOldMesh;
    private boolean componentBoxSelecting;
    private double boxStartX, boxStartY, boxCurrentX, boxCurrentY;
    private boolean componentDragging;
    private ComponentTransformGizmo.Axis componentAxis = ComponentTransformGizmo.Axis.NONE;
    private ComponentTransformGizmo.Operation componentOperation = ComponentTransformGizmo.Operation.MOVE;
    private ComponentTransformGizmo.PivotMode componentPivotMode = ComponentTransformGizmo.PivotMode.MEDIAN;
    private ComponentTransformGizmo.Axis hoveredComponentAxis = ComponentTransformGizmo.Axis.NONE;
    private MeshGeometry componentDragOldMesh;
    private double componentDragLastX, componentDragLastY;
    private double componentDragStartX, componentDragStartY;
    private TransformMath.Point componentDragPivot;
    private ComponentTransformGizmo.Axis mirrorAxis = ComponentTransformGizmo.Axis.X;
    private ComponentTransformGizmo.Axis componentConstraintAxis = ComponentTransformGizmo.Axis.NONE;
    private boolean componentPlaneConstraint;
    private boolean componentKeyboardTransformArmed;
    private boolean componentNumericEntry;
    private boolean topologyPathPickArmed;
    private boolean topologyPathSecondPick;
    private boolean topologyPathHasStart;
    private int topologyPathStartIndex = -1;

    private StringBuilder componentNumericBuffer = new StringBuilder();
    private boolean componentNumericNegative;
    private boolean proportionalEditing;
    private double proportionalRadius = 3.0;
    private boolean mirrorArmed;
    private boolean selectThrough;
    private MeshSelectionMode throughLastMode;
    private double throughLastX = Double.NaN;
    private double throughLastY = Double.NaN;
    private int throughLastIndex;
    private static final double MOVE_SNAP_INCREMENT = 0.25;
    private static final double ROTATE_SNAP_INCREMENT = 5.0;
    private static final double SCALE_SNAP_INCREMENT = 0.05;

    public CreateScreen(CreateCore core) {
        super(Text.literal("CREATE"));
        this.core = core;
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

        if (componentKeyboardTransformArmed && viewport.transform().mode() == TransformMode.GEOMETRY
                && viewport.meshComponentSelection().size() > 0) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            topologyPathPickArmed = false;
            topologyPathSecondPick = false;
            topologyPathHasStart = false;
            topologyPathStartIndex = -1;
                componentKeyboardTransformArmed = false;
                componentConstraintAxis = ComponentTransformGizmo.Axis.NONE;
                componentPlaneConstraint = false;
                componentNumericEntry = false;
                componentNumericBuffer.setLength(0);
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_MINUS || keyCode == GLFW.GLFW_KEY_KP_SUBTRACT) {
                componentNumericNegative = !componentNumericNegative;
                componentNumericEntry = true;
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_PERIOD || keyCode == GLFW.GLFW_KEY_KP_DECIMAL) {
                if (!componentNumericBuffer.toString().contains(".")) componentNumericBuffer.append('.');
                componentNumericEntry = true;
                return true;
            }
            int digit = switch (keyCode) {
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
            if (digit >= 0) {
                componentNumericBuffer.append((char)('0' + digit));
                componentNumericEntry = true;
                return true;
            }
        }
if ((keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER)
                && componentNumericEntry && componentKeyboardTransformArmed) {
            applyNumericComponentTransform();
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
        if (keyCode == 71) { componentConstraintAxis = ComponentTransformGizmo.Axis.NONE; componentPlaneConstraint = false; componentKeyboardTransformArmed = viewport.transform().mode()==TransformMode.GEOMETRY && viewport.meshComponentSelection().size()>0; if (componentKeyboardTransformArmed) componentOperation=ComponentTransformGizmo.Operation.MOVE; else viewport.transform().setMode(TransformMode.MOVE); return true; }
        if (keyCode == 82) { componentConstraintAxis = ComponentTransformGizmo.Axis.NONE; componentPlaneConstraint = false; componentKeyboardTransformArmed = viewport.transform().mode()==TransformMode.GEOMETRY && viewport.meshComponentSelection().size()>0; if (componentKeyboardTransformArmed) componentOperation=ComponentTransformGizmo.Operation.ROTATE; else viewport.transform().setMode(TransformMode.ROTATE); return true; }
        if (keyCode == 83) { componentConstraintAxis = ComponentTransformGizmo.Axis.NONE; componentPlaneConstraint = false; componentKeyboardTransformArmed = viewport.transform().mode()==TransformMode.GEOMETRY && viewport.meshComponentSelection().size()>0; if (componentKeyboardTransformArmed) componentOperation=ComponentTransformGizmo.Operation.SCALE; else viewport.transform().setMode(TransformMode.SCALE); return true; }

        if (viewport.transform().mode() == TransformMode.GEOMETRY && viewport.meshComponentSelection().size() > 0) {
            ComponentTransformGizmo.Axis requested = switch (keyCode) {
                case GLFW.GLFW_KEY_X -> ComponentTransformGizmo.Axis.X;
                case GLFW.GLFW_KEY_Y -> ComponentTransformGizmo.Axis.Y;
                case GLFW.GLFW_KEY_Z -> ComponentTransformGizmo.Axis.Z;
                default -> ComponentTransformGizmo.Axis.NONE;
            };
            if (requested != ComponentTransformGizmo.Axis.NONE && componentKeyboardTransformArmed) {
                if (componentConstraintAxis == requested) {
                    componentPlaneConstraint = !componentPlaneConstraint && hasShiftDown();
                    if (!hasShiftDown()) {
                        componentConstraintAxis = ComponentTransformGizmo.Axis.NONE;
                        componentPlaneConstraint = false;
                    }
                } else {
                    componentConstraintAxis = requested;
                    componentPlaneConstraint = hasShiftDown();
                }
                return true;
            }
        }
        if (keyCode == 80 && viewport.transform().mode() == TransformMode.GEOMETRY && viewport.meshComponentSelection().size() > 0) {
            componentPivotMode = componentPivotMode.next();
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
            mirrorArmed = true;
            return true;
        }
        if (mirrorArmed && viewport.transform().mode() == TransformMode.GEOMETRY
                && viewport.meshComponentSelection().size() > 0) {
            int axis = switch (keyCode) {
                case GLFW.GLFW_KEY_X -> 0;
                case GLFW.GLFW_KEY_Y -> 1;
                case GLFW.GLFW_KEY_Z -> 2;
                default -> -1;
            };
            if (axis >= 0) {
                mirrorAxis = axis == 0 ? ComponentTransformGizmo.Axis.X
                        : axis == 1 ? ComponentTransformGizmo.Axis.Y : ComponentTransformGizmo.Axis.Z;
                mirrorSelectedComponents(axis);
                mirrorArmed = false;
                return true;
            }
        }

        if (keyCode == GLFW.GLFW_KEY_B && hasControlDown()
                && viewport.transform().mode() == TransformMode.GEOMETRY
                && viewport.meshComponentSelection().mode() == MeshSelectionMode.EDGE) {
            bevelSelectedEdge(hasShiftDown() ? 1.0 : 0.25);
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
                extrudeSelectedEdge(hasShiftDown() ? 1.0 : 0.25);
            } else if (viewport.meshComponentSelection().size() > 1
                    && viewport.meshComponentSelection().mode() == MeshSelectionMode.FACE) {
                extrudeSelectedFaces(hasShiftDown() ? 1.0 : 0.25);
            } else {
                extrudeSelectedFace(hasShiftDown() ? 1.0 : 0.25);
            }
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_I && viewport.transform().mode() == TransformMode.GEOMETRY) {
            if (viewport.meshComponentSelection().mode() == MeshSelectionMode.FACE
                    && viewport.meshComponentSelection().size() > 1) {
                insetSelectedFaces(hasShiftDown() ? 0.5 : 0.25);
            } else {
                insetSelectedFace(hasShiftDown() ? 0.5 : 0.25);
            }
            return true;
        }

        // Topology path selection: V = arm a second vertex pick, B = boundary.
if (keyCode == GLFW.GLFW_KEY_V && viewport.transform().mode() == TransformMode.GEOMETRY
        && viewport.meshComponentSelection().mode() == MeshSelectionMode.VERTEX
        && viewport.meshComponentSelection().size() > 0) {
    topologyPathPickArmed = true;
    topologyPathSecondPick = true;
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
            selectThrough = !selectThrough;
            throughLastIndex = -1;
            throughLastX = Double.NaN;
            throughLastY = Double.NaN;
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
                if (keyCode == GLFW.GLFW_KEY_LEFT) { moveSelectedComponents(-step, 0, 0); return true; }
                if (keyCode == GLFW.GLFW_KEY_RIGHT) { moveSelectedComponents(step, 0, 0); return true; }
                if (keyCode == GLFW.GLFW_KEY_DOWN) { moveSelectedComponents(0, 0, step); return true; }
                if (keyCode == GLFW.GLFW_KEY_UP) { moveSelectedComponents(0, 0, -step); return true; }
                if (keyCode == GLFW.GLFW_KEY_SPACE) { moveSelectedComponents(0, step, 0); return true; }
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

        if (keyCode == 27) { resetThroughCycle(); componentKeyboardTransformArmed=false; componentConstraintAxis=ComponentTransformGizmo.Axis.NONE; componentPlaneConstraint=false; componentNumericEntry=false; componentNumericBuffer.setLength(0); componentNumericNegative=false; mirrorArmed=false; viewport.transform().setMode(TransformMode.SELECT); }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private void setMeshSelectionMode(MeshSelectionMode mode) {
        var selection = core.editorContext().viewport().meshComponentSelection();
        selection.clear();
        resetThroughCycle();
        selection.setMode(mode);
    }

    private void mirrorSelectedComponents(int axis) {
        var viewport = core.editorContext().viewport();
        var node = viewport.meshComponentSelection().node(core.editorContext().model());
        if (node == null) return;
        var oldMesh = node.ensureMeshGeometry();
        if (oldMesh == null) return;

        var selection = viewport.meshComponentSelection();
        var ids = MeshComponentTransforms.affectedVertices(oldMesh, selection.mode(),
                selection.vertexIndices(), selection.edgeIndices(), selection.faceIndices());
        if (ids.isEmpty()) return;

        var pivot = componentGizmo.localPivot(node, selection.mode(),
                selection.vertexIndices(), selection.edgeIndices(), selection.faceIndices(), componentPivotMode);
        var newMesh = MeshComponentTransforms.mirror(oldMesh, ids, pivot, axis);
        if (newMesh.equals(oldMesh)) return;

        core.editorContext().history().execute(
                new SetMeshGeometryCommand(node, oldMesh.copy(), newMesh));
    }

(double dx, double dy, double dz) {    private void moveSelectedComponents
        var viewport = core.editorContext().viewport();
        var node = viewport.meshComponentSelection().node(core.editorContext().model());
        if (node == null) return;
        var oldMesh = node.ensureMeshGeometry();
        if (oldMesh == null) return;

        java.util.LinkedHashSet<Integer> indices = new java.util.LinkedHashSet<>();
        if (viewport.meshComponentSelection().mode() == MeshSelectionMode.VERTEX) {
            indices.addAll(viewport.meshComponentSelection().vertexIndices());
        } else {
            for (int[] edge : viewport.meshComponentSelection().edgeIndices()) {
                indices.add(edge[0]);
                indices.add(edge[1]);
            }
        }
        if (indices.isEmpty()) return;

        var newMesh = oldMesh.copy();
        for (int index : indices) {
            newMesh = MeshOperations.moveVertex(newMesh, index, dx, dy, dz);
        }
        core.editorContext().history().execute(
                new SetMeshGeometryCommand(node, oldMesh.copy(), newMesh));
    }

    private void moveSelectedVertex(double dx, double dy, double dz) {
        var viewport = core.editorContext().viewport();
        var node = viewport.meshComponentSelection().node(core.editorContext().model());
        if (node == null || viewport.meshComponentSelection().mode() != MeshSelectionMode.VERTEX) return;
        int index = viewport.meshComponentSelection().indexA();
        var oldMesh = node.ensureMeshGeometry();
        if (oldMesh == null) return;
        var newMesh = MeshOperations.moveVertex(oldMesh, index, dx, dy, dz);
        core.editorContext().history().execute(
                new SetMeshGeometryCommand(node, oldMesh.copy(), newMesh));
        viewport.meshComponentSelection().selectVertex(node, index);
    }

    private void extrudeSelectedEdge(double amount) {
        var viewport = core.editorContext().viewport();
        var model = core.editorContext().model();
        var selection = viewport.meshComponentSelection();
        var node = selection.node(model);
        if (node == null || selection.size() == 0) return;

        var oldMesh = node.ensureMeshGeometry();
        if (oldMesh == null) return;

        if (selection.size() > 1) {
            var selected = new java.util.LinkedHashSet<Long>();
            for (int[] edge : selection.edgeIndices()) {
                selected.add(MeshTopologySelection.edgeKey(edge[0], edge[1]));
            }

            MeshOperations.OperationResult result =
                    MeshOperations.extrudeEdgesResult(oldMesh, selected, amount);
            var newMesh = result.mesh();
            core.editorContext().history().execute(
                    new SetMeshGeometryCommand(node, oldMesh.copy(), newMesh));

            selection.applySelectionHint(node, result);
            return;
        }

        int a = selection.indexA();
        int b = selection.indexB();
        if (a < 0 || b < 0) return;

        var newMesh = MeshOperations.extrudeEdge(oldMesh, a, b, amount);
        core.editorContext().history().execute(
                new SetMeshGeometryCommand(node, oldMesh.copy(), newMesh));
        int newA = newMesh.vertices().size() - 2;
        int newB = newMesh.vertices().size() - 1;
        selection.selectEdge(node, newA, newB);
    }

    private void bevelSelectedEdge(double amount) {
        var viewport = core.editorContext().viewport();
        var model = core.editorContext().model();
        var selection = viewport.meshComponentSelection();
        var node = selection.node(model);
        if (node == null || selection.size() == 0) return;

        var oldMesh = node.ensureMeshGeometry();
        if (oldMesh == null) return;

        try {
            if (selection.size() > 1) {
                var selected = new java.util.LinkedHashSet<Long>();
                for (int[] edge : selection.edgeIndices()) {
                    selected.add(MeshTopologySelection.edgeKey(edge[0], edge[1]));
                }

                MeshOperations.OperationResult result =
                        MeshOperations.bevelEdgesResult(oldMesh, selected, amount);
                var newMesh = result.mesh();
                if (result.createdFaces().isEmpty()) return;

                core.editorContext().history().execute(
                        new SetMeshGeometryCommand(node, oldMesh.copy(), newMesh));

                selection.applySelectionHint(node, result);
            } else {
                int a = selection.indexA();
                int b = selection.indexB();
                if (a < 0 || b < 0) return;

                var newMesh = MeshOperations.bevelEdge(oldMesh, a, b, amount);
                core.editorContext().history().execute(
                        new SetMeshGeometryCommand(node, oldMesh.copy(), newMesh));
                selection.clear();
            }
        } catch (IllegalArgumentException ignored) {
            // Bevel requires manifold selected edges with exactly two adjacent faces.
        }
    }

    private void insetSelectedFaces(double amount) {
        var viewport = core.editorContext().viewport();
        var model = core.editorContext().model();
        var selection = viewport.meshComponentSelection();
        var node = selection.node(model);
        if (node == null || selection.faceIndices().isEmpty()) return;

        var selected = new java.util.LinkedHashSet<>(selection.faceIndices());
        var oldMesh = node.ensureMeshGeometry();
        if (oldMesh == null) return;

        MeshOperations.OperationResult result =
                MeshOperations.insetFacesResult(oldMesh, selected, amount);
        if (result.createdFaces().isEmpty()) return;

        core.editorContext().history().execute(
                new SetMeshGeometryCommand(node, oldMesh.copy(), result.mesh()));

        selection.applySelectionHint(node, result);
        viewport.geometryFaceSelection().clear();
    }

    private void extrudeSelectedFaces(double amount) {
        var viewport = core.editorContext().viewport();
        var model = core.editorContext().model();
        var selection = viewport.meshComponentSelection();
        var node = selection.node(model);
        if (node == null || selection.faceIndices().isEmpty()) return;

        var selected = new java.util.LinkedHashSet<>(selection.faceIndices());
        var oldMesh = node.ensureMeshGeometry();
        if (oldMesh == null) return;

        MeshOperations.OperationResult result =
                MeshOperations.extrudeFacesResult(oldMesh, selected, amount);
        if (result.createdFaces().isEmpty()) return;

        core.editorContext().history().execute(
                new SetMeshGeometryCommand(node, oldMesh.copy(), result.mesh()));

        selection.applySelectionHint(node, result);
        viewport.geometryFaceSelection().clear();
    }

    private void insetSelectedFace(double amount) {
        var viewport = core.editorContext().viewport();
        var model = core.editorContext().model();
        var selection = viewport.meshComponentSelection();
        var node = selection.node(model);
        int faceIndex = selection.activeFace();
        if (node == null || faceIndex < 0 || !selection.containsFace(faceIndex)) return;

        var oldMesh = node.ensureMeshGeometry();
        if (oldMesh == null || faceIndex >= oldMesh.faces().size()) return;

        MeshOperations.OperationResult result =
                MeshOperations.insetFaceResult(oldMesh, faceIndex, amount);
        if (result.createdFaces().isEmpty()) return;

        core.editorContext().history().execute(
                new SetMeshGeometryCommand(node, oldMesh.copy(), result.mesh()));

        selection.applySelectionHint(node, result);
        viewport.geometryFaceSelection().clear();
    }

    private void extrudeSelectedFace(double amount) {
        var viewport = core.editorContext().viewport();
        var model = core.editorContext().model();
        var selection = viewport.meshComponentSelection();
        var node = selection.node(model);
        int faceIndex = selection.activeFace();
        if (node == null || faceIndex < 0 || !selection.containsFace(faceIndex)) return;

        var oldMesh = node.ensureMeshGeometry();
        if (oldMesh == null || faceIndex >= oldMesh.faces().size()) return;

        MeshOperations.OperationResult result =
                MeshOperations.extrudeFaceResult(oldMesh, faceIndex, amount);
        if (result.createdFaces().isEmpty()) return;

        core.editorContext().history().execute(
                new SetMeshGeometryCommand(node, oldMesh.copy(), result.mesh()));

        selection.applySelectionHint(node, result);
        viewport.geometryFaceSelection().clear();
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
        if (button == 0 && topologyPathPickArmed && topologyPathSecondPick) {
            ViewportContext viewport = core.editorContext().viewport();
            ModelNode node = viewport.selection().first(core.editorContext().model());
            if (node != null && viewport.transform().mode() == TransformMode.GEOMETRY
                    && viewport.meshComponentSelection().mode() == MeshSelectionMode.VERTEX) {
                MeshGeometry mesh = node.ensureMeshGeometry();
                if (mesh != null) {
                    int hit = hitTestMeshVertex(node, mesh, viewport, mouseX, mouseY);
                    if (hit >= 0) {
                        if (!topologyPathHasStart) {
                            viewport.meshComponentSelection().selectVertex(node, hit);
                            topologyPathHasStart = true;
                            topologyPathStartIndex = hit;
                            return true;
                        }
                        viewport.meshComponentSelection().selectShortestVertexPath(
                                node, topologyPathStartIndex, hit);
                        topologyPathPickArmed = false;
                        topologyPathSecondPick = false;
                        topologyPathHasStart = false;
                        topologyPathStartIndex = -1;
                        return true;
                    }
                }
            }
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
                        && componentKeyboardTransformArmed
                        && componentConstraintAxis != ComponentTransformGizmo.Axis.NONE) {
                    componentAxis = componentConstraintAxis;
                    componentDragging = true;
                    componentKeyboardTransformArmed = false;
                    componentDragStartX = mouseX;
                    componentDragStartY = mouseY;
                    componentDragLastX = mouseX;
                    componentDragLastY = mouseY;
                    componentDragOldMesh = selected.ensureMeshGeometry();
                    if (componentDragOldMesh != null) componentDragOldMesh = componentDragOldMesh.copy();
                    componentDragPivot = componentGizmo.localPivot(selected, meshMode,
                            viewport.meshComponentSelection().vertexIndices(),
                            viewport.meshComponentSelection().edgeIndices(),
                            viewport.meshComponentSelection().faceIndices(), componentPivotMode,
                            viewport.meshComponentSelection());
                    return true;
                }
                if (!hasShiftDown() && !hasAltDown() && viewport.meshComponentSelection().size() > 0) {
                    componentAxis = componentGizmo.hover(selected, meshMode,
                            viewport.meshComponentSelection().vertexIndices(),
                            viewport.meshComponentSelection().edgeIndices(),
                            viewport.meshComponentSelection().faceIndices(),
                            projector, mouseX, mouseY, cx, cy, componentOperation, componentPivotMode,
                            viewport.meshComponentSelection());
                    if (componentAxis != ComponentTransformGizmo.Axis.NONE) {
                        componentDragging = true;
                        componentDragStartX = mouseX;
                        componentDragStartY = mouseY;
                        componentDragLastX = mouseX;
                        componentDragLastY = mouseY;
                        componentDragOldMesh = selected.ensureMeshGeometry();
                        if (componentDragOldMesh != null) componentDragOldMesh = componentDragOldMesh.copy();
                        componentDragPivot = componentGizmo.localPivot(selected, meshMode,
                                viewport.meshComponentSelection().vertexIndices(),
                                viewport.meshComponentSelection().edgeIndices(),
                                viewport.meshComponentSelection().faceIndices(), componentPivotMode, viewport.meshComponentSelection());
                        return true;
                    }
                }
                MeshEditorController.PickResult meshPick = meshEditor.pickAndSelect(
                        selected, viewport, mouseX, mouseY, cx, cy,
                        selectThrough, hasAltDown(), hasShiftDown(), hasControlDown());
                if (meshPick.type() != MeshEditorController.PickType.NONE) {
                    viewport.geometryFaceSelection().clear();
                    switch (meshPick.type()) {
                        case VERTEX -> {
                            activeVertex = meshPick.index();
                            if (hasShiftDown() || hasAltDown() || hasControlDown()) return true;
                            var mesh = selected.ensureMeshGeometry();
                            if (mesh != null) {
                                vertexDragOldMesh = mesh.copy();
                                meshComponentDrag.beginVertex(selected);
                                vertexDragging = true;
                            }
                        }
                        case EDGE -> {
                            activeEdgeA = meshPick.edgeA();
                            activeEdgeB = meshPick.edgeB();
                            if (hasShiftDown() || hasAltDown()) return true;
                            var mesh = selected.ensureMeshGeometry();
                            if (mesh != null) {
                                edgeDragOldMesh = mesh.copy();
                                meshComponentDrag.beginEdge(selected, meshPick.edgeA(), meshPick.edgeB());
                                edgeDragging = true;
                            }
                        }
                        case FACE -> {
                            hoveredMeshFace = meshPick.index();
                            hoveredFace = GeometryFace.NONE;
                        }
                        case NONE -> { }
                    }
                    return true;
                }
                    GeometryFace clickedFace = gizmo.faceHit(selected, projector, mouseX, mouseY, cx, cy);
                    if (clickedFace != GeometryFace.NONE) {
                        viewport.geometryFaceSelection().select(selected, clickedFace);
                        hoveredFace = clickedFace;
                        cubeFaceEditor.begin(selected, clickedFace);
                        return true;
                    }
                    viewport.geometryFaceSelection().clear();
                }
                gizmoDragging = activeAxis != ViewportGizmo.Axis.NONE;
                hoveredAxis = activeAxis;
                if (gizmoDragging) {
                    dragOldGeometry = selected.geometry();
                    return true;
                }
            } else if (selected != null && viewport.transform().mode() != TransformMode.SELECT) {
                int cx=width/2, cy=height/2;
                activeAxis = gizmo.hit(selected, viewport.transform().mode(),
                        new ViewportProjector(viewport.viewport().camera()), mouseX, mouseY, cx, cy);
                gizmoDragging = activeAxis != ViewportGizmo.Axis.NONE;
                hoveredAxis = activeAxis;
                if (gizmoDragging) {
                    var t=selected.transform();
                    dragOldX=t.x(); dragOldY=t.y(); dragOldZ=t.z();
                    dragOldRx=t.rotationX(); dragOldRy=t.rotationY(); dragOldRz=t.rotationZ();
                    dragOldSx=t.scaleX(); dragOldSy=t.scaleY(); dragOldSz=t.scaleZ();
                    return true;
                }
            }
            // Empty-space drag in geometry mode starts component box selection.
            if (selected != null && viewport.transform().mode() == TransformMode.GEOMETRY) {
                componentBoxSelecting = true;
                boxStartX = boxCurrentX = mouseX;
                boxStartY = boxCurrentY = mouseY;
                return true;
            }

            ModelNode hit = new ViewportPicker().pick(core.editorContext().model(), core.editorContext().viewport(), mouseX, mouseY, width, height);
            if (hit != null) {
                core.editorContext().viewport().selection().select(hit, SelectionMode.SINGLE);
                core.editorContext().viewport().geometryFaceSelection().clear();
            } else {
                core.editorContext().viewport().selection().clear();
                core.editorContext().viewport().geometryFaceSelection().clear();
            }
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (componentDragging && button == 0) {
            ModelNode node=core.editorContext().viewport().selection().first(core.editorContext().model());
            if(node!=null && componentDragOldMesh!=null){
                var current=node.ensureMeshGeometry();
                if(current!=null && !componentDragOldMesh.equals(current))
                    core.editorContext().history().recordExecuted(new SetMeshGeometryCommand(node,componentDragOldMesh,current.copy()));
            }
            componentDragging=false;
            componentKeyboardTransformArmed=false;
            componentConstraintAxis=ComponentTransformGizmo.Axis.NONE;
            componentPlaneConstraint=false;
            componentAxis=ComponentTransformGizmo.Axis.NONE;
            hoveredComponentAxis=ComponentTransformGizmo.Axis.NONE;
            componentDragOldMesh=null;
            componentDragPivot=null;
            componentDragStartX=0.0;
            componentDragStartY=0.0;
            componentDragLastX=0.0;
            componentDragLastY=0.0;
            return true;
        }
        if (componentBoxSelecting && button == 0) {
            boxCurrentX = mouseX;
            boxCurrentY = mouseY;
            selectComponentsInBox();
            componentBoxSelecting = false;
            return true;
        }
        if (vertexDragging && button == 0) {
            meshComponentDrag.finish(core, core.editorContext().viewport().selection().first(core.editorContext().model()));
            vertexDragging = false;
            activeVertex = -1;
            vertexDragOldMesh = null;
            return true;
        }
        if (edgeDragging && button == 0) {
            meshComponentDrag.finish(core, core.editorContext().viewport().selection().first(core.editorContext().model()));
            edgeDragging = false;
            activeEdgeA = -1;
            activeEdgeB = -1;
            edgeDragOldMesh = null;
            return true;
        }
        if (cubeFaceEditor.dragging() && button == 0) {
            cubeFaceEditor.finish(core);
            return true;
        }
        if (gizmoDragging && button == 0) {
            ModelNode node = core.editorContext().viewport().selection().first(core.editorContext().model());
            if (node != null) {
                if (core.editorContext().viewport().transform().mode() == TransformMode.GEOMETRY
                        && dragOldGeometry != null && node.geometry() != null) {
                    var t = node.transform();
                    boolean geometryChanged = !dragOldGeometry.equals(node.geometry());
                    boolean positionChanged = dragOldX != t.x() || dragOldY != t.y() || dragOldZ != t.z();
                    if (geometryChanged || positionChanged) {
                        core.editorContext().history().recordExecuted(
                                new ResizeCubeFaceCommand(node,
                                        dragOldGeometry, node.geometry(),
                                        dragOldX, dragOldY, dragOldZ,
                                        t.x(), t.y(), t.z()));
                    }
                    dragOldGeometry = null;
                    gizmoDragging=false;
                    activeAxis=ViewportGizmo.Axis.NONE;
                    return true;
                }

                var t=node.transform();
                boolean changed = dragOldX!=t.x() || dragOldY!=t.y() || dragOldZ!=t.z() ||
                        dragOldRx!=t.rotationX() || dragOldRy!=t.rotationY() || dragOldRz!=t.rotationZ() ||
                        dragOldSx!=t.scaleX() || dragOldSy!=t.scaleY() || dragOldSz!=t.scaleZ();
                if (changed) {
                    core.editorContext().history().recordExecuted(new SetTransformCommand(node,
                            dragOldX,dragOldY,dragOldZ,dragOldRx,dragOldRy,dragOldRz,dragOldSx,dragOldSy,dragOldSz,
                            t.x(),t.y(),t.z(),t.rotationX(),t.rotationY(),t.rotationZ(),t.scaleX(),t.scaleY(),t.scaleZ(),true));
                }
            }
            gizmoDragging=false;
            activeAxis=ViewportGizmo.Axis.NONE;
            return true;
        }
        if (viewportInput.mouseReleased(mouseX, mouseY, button)) return true;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (componentDragging && button == 0) {
            ModelNode node=core.editorContext().viewport().meshComponentSelection().node(core.editorContext().model());
            if(node!=null){
                var mesh=node.ensureMeshGeometry();
                if(mesh!=null){
                    var selection=core.editorContext().viewport().meshComponentSelection();
                    java.util.Set<Integer> ids=MeshComponentTransforms.affectedVertices(mesh,selection.mode(),
                            selection.vertexIndices(),selection.edgeIndices(),selection.faceIndices());
                    var projector=new ViewportProjector(core.editorContext().viewport().viewport().camera());
                    var updated=componentDragOldMesh.copy();
                    var pivot=componentDragPivot;
                    ComponentTransformGizmo.Axis constrainedAxis = componentConstraintAxis != ComponentTransformGizmo.Axis.NONE
                            ? componentConstraintAxis : componentAxis;
                    int axis=constrainedAxis==ComponentTransformGizmo.Axis.X?0
                            :constrainedAxis==ComponentTransformGizmo.Axis.Y?1:2;
                    double totalDx = mouseX - componentDragStartX;
                    double totalDy = mouseY - componentDragStartY;

                    if(componentOperation==ComponentTransformGizmo.Operation.MOVE){
                        if (componentPlaneConstraint && constrainedAxis != ComponentTransformGizmo.Axis.NONE) {
                            ComponentTransformGizmo.Axis a1 = constrainedAxis == ComponentTransformGizmo.Axis.X
                                    ? ComponentTransformGizmo.Axis.Y : ComponentTransformGizmo.Axis.X;
                            ComponentTransformGizmo.Axis a2 = constrainedAxis == ComponentTransformGizmo.Axis.Z
                                    ? ComponentTransformGizmo.Axis.Y : ComponentTransformGizmo.Axis.Z;
                            double amount1 = componentGizmo.amount(a1, projector, node, pivot,
                                    width / 2, height / 2, totalDx, totalDy);
                            double amount2 = componentGizmo.amount(a2, projector, node, pivot,
                                    width / 2, height / 2, totalDx, totalDy);
                            if (hasControlDown()) {
                                amount1 = snapScalar(amount1, MOVE_SNAP_INCREMENT);
                                amount2 = snapScalar(amount2, MOVE_SNAP_INCREMENT);
                            }
                            int excluded = constrainedAxis == ComponentTransformGizmo.Axis.X ? 0
                                    : constrainedAxis == ComponentTransformGizmo.Axis.Y ? 1 : 2;
                            double dx = excluded == 0 ? 0 : (a1 == ComponentTransformGizmo.Axis.X ? amount1 : amount2);
                            double dy = excluded == 1 ? 0 : (a1 == ComponentTransformGizmo.Axis.Y ? amount1 : amount2);
                            double dz = excluded == 2 ? 0 : (a1 == ComponentTransformGizmo.Axis.Z ? amount1 : amount2);
                            updated = proportionalEditing
                                ? MeshComponentTransforms.translateProportional(updated, ids, pivot, proportionalRadius, dx, dy, dz)
                                : MeshComponentTransforms.translate(updated, ids, dx, dy, dz);
                        } else {
                        double amount=componentGizmo.amount(constrainedAxis,projector,node,pivot,
                                width / 2,height / 2,totalDx,totalDy);
                        if (hasControlDown()) amount = snapScalar(amount, MOVE_SNAP_INCREMENT);
                        double dx=axis==0?amount:0, dy=axis==1?amount:0, dz=axis==2?amount:0;
                        updated=proportionalEditing
                                ? MeshComponentTransforms.translateProportional(updated, ids, pivot, proportionalRadius, dx, dy, dz)
                                : MeshComponentTransforms.translate(updated,ids,dx,dy,dz);
                        }
                    } else if(componentOperation==ComponentTransformGizmo.Operation.ROTATE){
                        double degrees=componentGizmo.rotationAmount(constrainedAxis, node, pivot,
                                componentDragStartX, componentDragStartY, mouseX, mouseY,
                                projector, width / 2, height / 2);
                        if (hasControlDown()) degrees = snapScalar(degrees, ROTATE_SNAP_INCREMENT);
                        updated=proportionalEditing
                            ? MeshComponentTransforms.rotateProportional(updated, ids, pivot, proportionalRadius, axis, degrees)
                            : MeshComponentTransforms.rotate(updated,ids,pivot,axis,degrees);
                    } else {
                        if (componentPlaneConstraint && constrainedAxis != ComponentTransformGizmo.Axis.NONE) {
                            ComponentTransformGizmo.Axis a1 = constrainedAxis == ComponentTransformGizmo.Axis.X
                                    ? ComponentTransformGizmo.Axis.Y : ComponentTransformGizmo.Axis.X;
                            ComponentTransformGizmo.Axis a2 = constrainedAxis == ComponentTransformGizmo.Axis.Z
                                    ? ComponentTransformGizmo.Axis.Y : ComponentTransformGizmo.Axis.Z;
                            double factor1 = componentGizmo.scaleFactor(a1, projector, node, pivot,
                                    width / 2, height / 2, totalDx, totalDy);
                            double factor2 = componentGizmo.scaleFactor(a2, projector, node, pivot,
                                    width / 2, height / 2, totalDx, totalDy);
                            if (hasControlDown()) {
                                factor1 = snapScaleFactor(factor1, SCALE_SNAP_INCREMENT);
                                factor2 = snapScaleFactor(factor2, SCALE_SNAP_INCREMENT);
                            }
                            updated = proportionalEditing
                                ? MeshComponentTransforms.scaleProportional(updated, ids, pivot, proportionalRadius,
                                    a1 == ComponentTransformGizmo.Axis.X ? 0 : a1 == ComponentTransformGizmo.Axis.Y ? 1 : 2, factor1)
                                : MeshComponentTransforms.scale(updated, ids, pivot,
                                    a1 == ComponentTransformGizmo.Axis.X ? 0 : a1 == ComponentTransformGizmo.Axis.Y ? 1 : 2, factor1);
                            updated = proportionalEditing
                                ? MeshComponentTransforms.scaleProportional(updated, ids, pivot, proportionalRadius,
                                    a2 == ComponentTransformGizmo.Axis.X ? 0 : a2 == ComponentTransformGizmo.Axis.Y ? 1 : 2, factor2)
                                : MeshComponentTransforms.scale(updated, ids, pivot,
                                    a2 == ComponentTransformGizmo.Axis.X ? 0 : a2 == ComponentTransformGizmo.Axis.Y ? 1 : 2, factor2);
                        } else {
                            double factor=componentGizmo.scaleFactor(constrainedAxis,projector,node,pivot,
                                    width / 2,height / 2,totalDx,totalDy);
                            if (hasControlDown()) factor = snapScaleFactor(factor, SCALE_SNAP_INCREMENT);
                            updated=proportionalEditing
                                ? MeshComponentTransforms.scaleProportional(updated, ids, pivot, proportionalRadius, axis, factor)
                                : MeshComponentTransforms.scale(updated,ids,pivot,axis,factor);
                        }
                    }
                    node.setMeshGeometry(updated);
                }
            }
            return true;
        }
        if (componentBoxSelecting && button == 0) {
            boxCurrentX = mouseX;
            boxCurrentY = mouseY;
            return true;
        }
        if (vertexDragging && button == 0) {
            ModelNode node = core.editorContext().viewport().selection().first(core.editorContext().model());
            if (node != null && activeVertex >= 0) {
                var mesh = node.ensureMeshGeometry();
                if (mesh != null && activeVertex < mesh.vertices().size()) {
                    var camera = core.editorContext().viewport().viewport().camera();
                    double yaw = Math.toRadians(camera.yaw());
                    double pitch = Math.toRadians(camera.pitch());
                    double cy = Math.cos(yaw);
                    double sy = Math.sin(yaw);
                    double cp = Math.cos(pitch);
                    double sp = Math.sin(pitch);
                    double worldPerPixel = Math.max(0.0005, camera.distance() / 300.0);
                    double rightX = cy;
                    double rightZ = -sy;
                    double upX = -sy * sp;
                    double upY = cp;
                    double upZ = -cy * sp;
                    double dx = (deltaX * rightX - deltaY * upX) * worldPerPixel;
                    double dy = (-deltaY * upY) * worldPerPixel;
                    double dz = (deltaX * rightZ - deltaY * upZ) * worldPerPixel;
                    node.setMeshGeometry(MeshOperations.moveVertex(mesh, activeVertex, dx, dy, dz));
                }
            }
            return true;
        }
        if (cubeFaceEditor.dragging()) {
            cubeFaceEditor.update(core, deltaX, deltaY);
            return true;
        }
        if (gizmoDragging && button == 0) {
            ModelNode node = core.editorContext().viewport().selection().first(core.editorContext().model());
            if (node != null) {
                if (core.editorContext().viewport().transform().mode() == TransformMode.GEOMETRY) {
                    var g = node.geometry();
                    if (g != null) {
                        double amount = gizmo.dragAmount(activeAxis,
                                new ViewportProjector(core.editorContext().viewport().viewport().camera()),
                                deltaX, deltaY);
                        double width = g.width();
                        double height = g.height();
                        double depth = g.depth();
                        double sign = (activeAxis == ViewportGizmo.Axis.NEG_X ||
                                activeAxis == ViewportGizmo.Axis.NEG_Y ||
                                activeAxis == ViewportGizmo.Axis.NEG_Z) ? -1.0 : 1.0;
                        double move = amount * 2.0 * sign;

                        if (activeAxis == ViewportGizmo.Axis.X || activeAxis == ViewportGizmo.Axis.NEG_X) {
                            width = Math.max(0.1, width + move);
                            node.transform().position(node.transform().x() + amount * sign,
                                    node.transform().y(), node.transform().z());
                        } else if (activeAxis == ViewportGizmo.Axis.Y || activeAxis == ViewportGizmo.Axis.NEG_Y) {
                            height = Math.max(0.1, height + move);
                            node.transform().position(node.transform().x(),
                                    node.transform().y() + amount * sign, node.transform().z());
                        } else if (activeAxis == ViewportGizmo.Axis.Z || activeAxis == ViewportGizmo.Axis.NEG_Z) {
                            depth = Math.max(0.1, depth + move);
                            node.transform().position(node.transform().x(),
                                    node.transform().y(), node.transform().z() + amount * sign);
                        }

                        node.setGeometry(new CubeGeometry(width, height, depth));
                    }
                    return true;
                } else if (core.editorContext().viewport().transform().mode() == TransformMode.MOVE) {
                    double dx=activeAxis==ViewportGizmo.Axis.X?amount:0;
                    double dy=activeAxis==ViewportGizmo.Axis.Y?amount:0;
                    double dz=activeAxis==ViewportGizmo.Axis.Z?amount:0;
                    core.editorContext().viewport().transform().translate(node,dx,dy,dz);
                } else if (core.editorContext().viewport().transform().mode() == TransformMode.ROTATE) {
                    double rx=activeAxis==ViewportGizmo.Axis.X?amount*10:0;
                    double ry=activeAxis==ViewportGizmo.Axis.Y?amount*10:0;
                    double rz=activeAxis==ViewportGizmo.Axis.Z?amount*10:0;
                    core.editorContext().viewport().transform().rotateBy(node,rx,ry,rz);
                } else if (core.editorContext().viewport().transform().mode() == TransformMode.SCALE) {
                    double s=amount*0.1;
                    core.editorContext().viewport().transform().scaleBy(node,
                            activeAxis==ViewportGizmo.Axis.X?s:0,
                            activeAxis==ViewportGizmo.Axis.Y?s:0,
                            activeAxis==ViewportGizmo.Axis.Z?s:0);
                }
            }
            return true;
        }
        if (viewportInput.mouseDragged(mouseX, mouseY, button, hasShiftDown())) return true;
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (viewportInput.mouseScrolled(verticalAmount)) return true;
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override public void mouseMoved(double mouseX, double mouseY) {
        if (!gizmoDragging && !componentDragging) {
            ViewportContext viewport=core.editorContext().viewport();
            ModelNode selected=viewport.selection().first(core.editorContext().model());
            if(selected!=null && viewport.transform().mode()==TransformMode.GEOMETRY) {
                hoveredAxis=gizmo.geometryHit(selected,new ViewportProjector(viewport.viewport().camera()),
                        mouseX,mouseY,width/2,height/2);
                if (viewport.meshComponentSelection().matches(selected) && viewport.meshComponentSelection().size()>0) {
                    var projector=new ViewportProjector(viewport.viewport().camera());
                    hoveredComponentAxis=componentGizmo.hover(selected,
                            viewport.meshComponentSelection().mode(),
                            viewport.meshComponentSelection().vertexIndices(),
                            viewport.meshComponentSelection().edgeIndices(),
                            viewport.meshComponentSelection().faceIndices(),
                            projector,mouseX,mouseY,width/2,height/2,componentOperation,componentPivotMode,
                            viewport.meshComponentSelection());
                } else hoveredComponentAxis=ComponentTransformGizmo.Axis.NONE;
                MeshEditorHoverController.HoverResult meshHover = meshEditorHover.resolve(
                        selected, viewport, mouseX, mouseY, width / 2, height / 2,
                        hoveredAxis, hoveredComponentAxis);
                hoveredMeshFace = meshHover.face();
                hoveredMeshVertex = meshHover.vertex();
                hoveredMeshEdgeA = meshHover.edgeA();
                hoveredMeshEdgeB = meshHover.edgeB();
                hoveredFace = meshEditorHover.primitiveFace(
                        selected, viewport, mouseX, mouseY, width / 2, height / 2,
                        hoveredAxis, hoveredComponentAxis);
            } else if(selected!=null && viewport.transform().mode()!=TransformMode.SELECT) {
                hoveredAxis=gizmo.hoveredAxis(selected,viewport.transform().mode(),
                        new ViewportProjector(viewport.viewport().camera()),mouseX,mouseY,width/2,height/2);
            } else hoveredAxis=ViewportGizmo.Axis.NONE;
        }
        super.mouseMoved(mouseX, mouseY);
    }

    @Override public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context, mouseX, mouseY, delta);
        GeometryFace selectedFace = core.editorContext().viewport().geometryFaceSelection().face();
        viewportRenderer.render(context, width, height, core.editorContext().viewport(),
                core.editorContext().model(), hoveredAxis, hoveredFace, selectedFace, hoveredMeshFace,
                hoveredMeshVertex, hoveredMeshEdgeA, hoveredMeshEdgeB,
                hoveredComponentAxis, componentOperation, componentPivotMode, selectThrough);

        ViewportContext activeViewport = core.editorContext().viewport();
        if (activeViewport.transform().mode() == TransformMode.GEOMETRY
                && activeViewport.meshComponentSelection().size() > 0) {
            var textRenderer = MinecraftClient.getInstance().textRenderer;
            String mode = activeViewport.meshComponentSelection().mode().name();
            String operation = componentOperation.name();
            String axis = componentAxis == ComponentTransformGizmo.Axis.NONE
                    ? (hoveredComponentAxis == ComponentTransformGizmo.Axis.NONE
                    ? "" : " " + hoveredComponentAxis.name())
                    : " " + componentAxis.name();
            String pivot = componentPivotMode.name().replace('_', ' ');
            String active = switch (activeComponentLabel(activeViewport.meshComponentSelection())) {
                case null -> "";
                case String value -> " • Active " + value;
            };
            String constraint = componentConstraintAxis == ComponentTransformGizmo.Axis.NONE ? "" : " • " + (componentPlaneConstraint ? "PLANE " : "") + componentConstraintAxis.name();
            String snap = hasControlDown() ? " • SNAP" : "";
            String numeric = componentNumericEntry ? " • Value " + (componentNumericNegative ? "-" : "") + componentNumericBuffer : "";
            String proportional = proportionalEditing ? " • PROP " + String.format(java.util.Locale.ROOT, "%.1f", proportionalRadius) : "";
            String hover = hoveredMeshVertex >= 0 ? " • Hover V" + hoveredMeshVertex
                    : hoveredMeshEdgeA >= 0 ? " • Hover E" + hoveredMeshEdgeA + "-" + hoveredMeshEdgeB
                    : hoveredMeshFace >= 0 ? " • Hover F" + hoveredMeshFace : "";
            String xray = selectThrough ? " • X-RAY" : "";
            String topology = topologyPathPickArmed ? " • PATH: " + (topologyPathHasStart ? "pick target" : "pick start") : "";
            context.drawTextWithShadow(textRenderer,
                    operation + axis + constraint + " • " + mode + " • Pivot " + pivot + active + snap + numeric + proportional + hover + xray + topology,
                    26, height - 30, 0xFFE8E8E8);
        }
        if (componentBoxSelecting) {
            int left = (int) Math.round(Math.min(boxStartX, boxCurrentX));
            int top = (int) Math.round(Math.min(boxStartY, boxCurrentY));
            int right = (int) Math.round(Math.max(boxStartX, boxCurrentX));
            int bottom = (int) Math.round(Math.max(boxStartY, boxCurrentY));
            context.fill(left, top, right, top + 1, 0xFFFFFFFF);
            context.fill(left, bottom, right, bottom + 1, 0xFFFFFFFF);
            context.fill(left, top, left + 1, bottom, 0xFFFFFFFF);
            context.fill(right, top, right + 1, bottom, 0xFFFFFFFF);
        }
        super.render(context, mouseX, mouseY, delta);
    }

    private void applyNumericComponentTransform() {
        ViewportContext viewport = core.editorContext().viewport();
        ModelNode node = viewport.selection().first(core.editorContext().model());
        if (node == null || componentConstraintAxis == ComponentTransformGizmo.Axis.NONE
                || componentNumericBuffer.length() == 0) return;
        double value;
        try {
            value = Double.parseDouble((componentNumericNegative ? "-" : "") + componentNumericBuffer);
        } catch (NumberFormatException ignored) {
            return;
        }
        var selection = viewport.meshComponentSelection();
        var mesh = node.ensureMeshGeometry();
        if (mesh == null) return;
        var ids = MeshComponentTransforms.affectedVertices(mesh, selection.mode(),
                selection.vertexIndices(), selection.edgeIndices(), selection.faceIndices());
        var pivot = componentGizmo.localPivot(node, selection.mode(),
                selection.vertexIndices(), selection.edgeIndices(), selection.faceIndices(),
                componentPivotMode, selection);
        MeshGeometry before = mesh.copy();
        MeshGeometry updated = mesh.copy();
        int axis = componentConstraintAxis == ComponentTransformGizmo.Axis.X ? 0
                : componentConstraintAxis == ComponentTransformGizmo.Axis.Y ? 1 : 2;
        if (componentOperation == ComponentTransformGizmo.Operation.MOVE) {
            if (componentPlaneConstraint) {
                ComponentTransformGizmo.Axis a1 = axis == 0 ? ComponentTransformGizmo.Axis.Y : ComponentTransformGizmo.Axis.X;
                ComponentTransformGizmo.Axis a2 = axis == 2 ? ComponentTransformGizmo.Axis.Y : ComponentTransformGizmo.Axis.Z;
                updated = MeshComponentTransforms.translate(updated, ids,
                        a1 == ComponentTransformGizmo.Axis.X ? value : 0,
                        a1 == ComponentTransformGizmo.Axis.Y ? value : 0,
                        a1 == ComponentTransformGizmo.Axis.Z ? value : 0);
                updated = MeshComponentTransforms.translate(updated, ids,
                        a2 == ComponentTransformGizmo.Axis.X ? value : 0,
                        a2 == ComponentTransformGizmo.Axis.Y ? value : 0,
                        a2 == ComponentTransformGizmo.Axis.Z ? value : 0);
            } else {
                updated = proportionalEditing
                        ? MeshComponentTransforms.translateProportional(updated, ids, pivot, proportionalRadius,
                            axis == 0 ? value : 0, axis == 1 ? value : 0, axis == 2 ? value : 0)
                        : MeshComponentTransforms.translate(updated, ids,
                            axis == 0 ? value : 0, axis == 1 ? value : 0, axis == 2 ? value : 0);
            }
        } else if (componentOperation == ComponentTransformGizmo.Operation.ROTATE) {
            updated = proportionalEditing
                    ? MeshComponentTransforms.rotateProportional(updated, ids, pivot, proportionalRadius, axis, value)
                    : MeshComponentTransforms.rotate(updated, ids, pivot, axis, value);
        } else {
            double factor = Math.max(0.01, value);
            if (componentPlaneConstraint) {
                int a1 = axis == 0 ? 1 : 0;
                int a2 = axis == 2 ? 1 : 2;
                updated = MeshComponentTransforms.scale(updated, ids, pivot, a1, factor);
                updated = MeshComponentTransforms.scale(updated, ids, pivot, a2, factor);
            } else {
                updated = proportionalEditing
                        ? MeshComponentTransforms.scaleProportional(updated, ids, pivot, proportionalRadius, axis, factor)
                        : MeshComponentTransforms.scale(updated, ids, pivot, axis, factor);
            }
        }
        if (!before.equals(updated)) {
            node.setMeshGeometry(updated);
            core.editorContext().history().recordExecuted(
                    new SetMeshGeometryCommand(node, before, updated.copy()));
        }
        componentNumericEntry = false;
        componentNumericBuffer.setLength(0);
        componentNumericNegative = false;
        componentKeyboardTransformArmed = false;
        componentConstraintAxis = ComponentTransformGizmo.Axis.NONE;
        componentPlaneConstraint = false;
    }

    private static String activeComponentLabel(whitevoid.create.editor.geometry.MeshComponentSelection selection) {
        return switch (selection.mode()) {
            case VERTEX -> selection.activeVertex() >= 0 ? "V" + selection.activeVertex() : null;
            case EDGE -> selection.activeEdgeA() >= 0 ? "E" + selection.activeEdgeA() + "-" + selection.activeEdgeB() : null;
            case FACE -> selection.activeFace() >= 0 ? "F" + selection.activeFace() : null;
        };
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

    private void selectComponentsInBox() {
        ViewportContext viewport = core.editorContext().viewport();
        ModelNode node = viewport.selection().first(core.editorContext().model());
        if (node == null || viewport.transform().mode() != TransformMode.GEOMETRY) return;
        var mesh = node.ensureMeshGeometry();
        if (mesh == null) return;

        int left=(int)Math.round(Math.min(boxStartX,boxCurrentX));
        int right=(int)Math.round(Math.max(boxStartX,boxCurrentX));
        int top=(int)Math.round(Math.min(boxStartY,boxCurrentY));
        int bottom=(int)Math.round(Math.max(boxStartY,boxCurrentY));
        if (right-left < 3 && bottom-top < 3) return;

        ViewportProjector projector=new ViewportProjector(viewport.viewport().camera());
        int cx=width/2, cy=height/2;
        var selection=viewport.meshComponentSelection();

        if (selection.mode() == MeshSelectionMode.VERTEX) {
            java.util.Set<Integer> hits = new java.util.LinkedHashSet<>();
            for (int i=0;i<mesh.vertices().size();i++) {
                var v=mesh.vertices().get(i);
                var w=whitevoid.create.model.TransformMath.applyHierarchy(
                        new whitevoid.create.model.TransformMath.Point(v.x(),v.y(),v.z()),node);
                var p=projector.project(w.x(),w.y(),w.z(),cx,cy,300);
                if(p!=null && p.x()>=left && p.x()<=right && p.y()>=top && p.y()<=bottom) hits.add(i);
            }
            if (hasAltDown()) {
                for (int i : hits) selection.removeVertex(node, i);
            } else if (hasShiftDown()) {
                for (int i : hits) selection.addVertex(node, i);
            } else {
                selection.clear();
                for (int i : hits) selection.addVertex(node, i);
            }
        } else if (selection.mode() == MeshSelectionMode.EDGE) {
            java.util.List<int[]> hits = new java.util.ArrayList<>();
            for (int[] edge : whitevoid.create.model.ModelRenderer.meshEdges(mesh)) {
                var a=mesh.vertices().get(edge[0]); var b=mesh.vertices().get(edge[1]);
                var wa=whitevoid.create.model.TransformMath.applyHierarchy(
                        new whitevoid.create.model.TransformMath.Point(a.x(),a.y(),a.z()),node);
                var wb=whitevoid.create.model.TransformMath.applyHierarchy(
                        new whitevoid.create.model.TransformMath.Point(b.x(),b.y(),b.z()),node);
                var pa=projector.project(wa.x(),wa.y(),wa.z(),cx,cy,300);
                var pb=projector.project(wb.x(),wb.y(),wb.z(),cx,cy,300);
                if(pa!=null && pb!=null && pointInsideBox(pa.x(),pa.y(),left,top,right,bottom)) {
                    hits.add(new int[]{edge[0], edge[1]});
                }
            }
            if (hasAltDown()) {
                for (int[] edge : hits) selection.removeEdge(node, edge[0], edge[1]);
            } else {
                if (!hasShiftDown()) selection.clear();
                for (int[] edge : hits) selection.addEdge(node, edge[0], edge[1]);
            }
        } else {
            java.util.Set<Integer> hits = new java.util.LinkedHashSet<>();
            for (int i=0;i<mesh.faces().size();i++) {
                int[] ids=mesh.faces().get(i).vertices();
                double sx=0,sy=0; int count=0;
                for(int id:ids) {
                    var v=mesh.vertices().get(id);
                    var w=whitevoid.create.model.TransformMath.applyHierarchy(
                            new whitevoid.create.model.TransformMath.Point(v.x(),v.y(),v.z()),node);
                    var p=projector.project(w.x(),w.y(),w.z(),cx,cy,300);
                    if(p!=null){sx+=p.x();sy+=p.y();count++;}
                }
                if(count>0 && pointInsideBox(sx/count,sy/count,left,top,right,bottom)) hits.add(i);
            }
            if (hasAltDown()) {
                for (int i : hits) selection.removeFace(node, i);
            } else {
                if (!hasShiftDown()) selection.clear();
                for (int i : hits) selection.addFace(node, i);
            }
        }
    }
    private boolean pointInsideBox(double x,double y,int left,int top,int right,int bottom) {
        return x>=left && x<=right && y>=top && y<=bottom;
    }


    private int hitTestMeshVertex(ModelNode node, MeshGeometry mesh, ViewportContext viewport,
                                   double mouseX, double mouseY) {
        ViewportProjector projector = new ViewportProjector(viewport.viewport().camera());
        int cx = width / 2, cy = height / 2;
        int best = -1;
        double bestDistance = 10.0;
        for (int i = 0; i < mesh.vertices().size(); i++) {
            var v = mesh.vertices().get(i);
            var world = whitevoid.create.model.TransformMath.applyHierarchy(
                    new whitevoid.create.model.TransformMath.Point(v.x(), v.y(), v.z()), node);
            var p = projector.project(world.x(), world.y(), world.z(), cx, cy, 300);
            if (p == null) continue;
            double distance = Math.hypot(p.x() - mouseX, p.y() - mouseY);
            if (distance < bestDistance) {
                bestDistance = distance;
                best = i;
            }
        }
        return best;
    }

    @Override public boolean shouldPause() { return false; }
}
