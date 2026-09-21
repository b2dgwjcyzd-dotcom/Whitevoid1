package whitevoid.create.ui;

import whitevoid.create.model.CubeGeometry;
import whitevoid.create.model.MeshGeometry;

final class CreateViewportInteractionState {
    ViewportGizmo.Axis activeAxis = ViewportGizmo.Axis.NONE;
    boolean gizmoDragging;
    ViewportGizmo.Axis hoveredAxis = ViewportGizmo.Axis.NONE;
    double dragOldX, dragOldY, dragOldZ;
    double dragOldRx, dragOldRy, dragOldRz;
    double dragOldSx, dragOldSy, dragOldSz;
    CubeGeometry dragOldGeometry;
    whitevoid.create.editor.geometry.GeometryFace hoveredFace =
            whitevoid.create.editor.geometry.GeometryFace.NONE;

    int hoveredMeshFace = -1;
    int hoveredMeshVertex = -1;
    int hoveredMeshEdgeA = -1;
    int hoveredMeshEdgeB = -1;

    boolean vertexDragging;
    int activeVertex = -1;
    MeshGeometry vertexDragOldMesh;
    boolean edgeDragging;
    int activeEdgeA = -1;
    int activeEdgeB = -1;
    MeshGeometry edgeDragOldMesh;

    boolean componentBoxSelecting;
    double boxStartX, boxStartY, boxCurrentX, boxCurrentY;
    boolean componentDragging;
    ComponentTransformGizmo.Axis hoveredComponentAxis = ComponentTransformGizmo.Axis.NONE;

    ComponentTransformGizmo.Axis mirrorAxis = ComponentTransformGizmo.Axis.X;

    boolean topologyPathPickArmed;
    boolean topologyPathSecondPick;
    boolean topologyPathHasStart;
    int topologyPathStartIndex = -1;

    boolean proportionalEditing;
    double proportionalRadius = 3.0;
    boolean mirrorArmed;
    boolean selectThrough;
    MeshSelectionMode throughLastMode;
    double throughLastX = Double.NaN;
    double throughLastY = Double.NaN;
    int throughLastIndex;
}
