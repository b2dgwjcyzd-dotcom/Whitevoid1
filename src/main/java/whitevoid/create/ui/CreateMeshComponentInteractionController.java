package whitevoid.create.ui;

import whitevoid.create.core.CreateCore;
import whitevoid.create.editor.geometry.GeometryFace;
import whitevoid.create.editor.viewport.ViewportContext;
import whitevoid.create.model.ModelNode;

final class CreateMeshComponentInteractionController {
    private final MeshEditorController meshEditor;
    private final MeshComponentDragController meshComponentDrag;

    CreateMeshComponentInteractionController(
            MeshEditorController meshEditor,
            MeshComponentDragController meshComponentDrag
    ) {
        this.meshEditor = meshEditor;
        this.meshComponentDrag = meshComponentDrag;
    }

    boolean handleClick(
            CreateCore core,
            CreateViewportInteractionState interaction,
            ModelNode selected,
            ViewportContext viewport,
            double mouseX,
            double mouseY,
            int centerX,
            int centerY,
            boolean selectThrough,
            boolean altDown,
            boolean shiftDown,
            boolean controlDown
    ) {
        MeshEditorController.PickResult pick = meshEditor.pickAndSelect(
                selected, viewport, mouseX, mouseY, centerX, centerY,
                selectThrough, altDown, shiftDown, controlDown
        );
        if (pick.type() == MeshEditorController.PickType.NONE) {
            return false;
        }

        viewport.geometryFaceSelection().clear();
        switch (pick.type()) {
            case VERTEX -> {
                interaction.activeVertex = pick.index();
                if (shiftDown || altDown || controlDown) return true;
                var mesh = selected.ensureMeshGeometry();
                if (mesh != null) {
                    interaction.vertexDragOldMesh = mesh.copy();
                    meshComponentDrag.beginVertex(selected);
                    interaction.vertexDragging = true;
                }
            }
            case EDGE -> {
                interaction.activeEdgeA = pick.edgeA();
                interaction.activeEdgeB = pick.edgeB();
                if (shiftDown || altDown) return true;
                var mesh = selected.ensureMeshGeometry();
                if (mesh != null) {
                    interaction.edgeDragOldMesh = mesh.copy();
                    meshComponentDrag.beginEdge(selected, pick.edgeA(), pick.edgeB());
                    interaction.edgeDragging = true;
                }
            }
            case FACE -> {
                interaction.hoveredMeshFace = pick.index();
                interaction.hoveredFace = GeometryFace.NONE;
            }
            case NONE -> { }
        }
        return true;
    }
}
