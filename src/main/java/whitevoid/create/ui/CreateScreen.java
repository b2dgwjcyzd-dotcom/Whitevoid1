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
        this.cubeFaceEditor = new CubeFaceEditorController(gizmo);
    }

    @Override protected void init() {
        core.editorContext().setEditing(true);
    }

    @Override public void close() {
        viewportInput.cancelDrag();
        cubeFaceEditor.cancel();
        meshComponentDrag.cancel();
        core.editorContext().setEditing(false);
        super.close();
    }
