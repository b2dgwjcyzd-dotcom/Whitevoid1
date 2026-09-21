package whitevoid.create.ui;

import whitevoid.create.core.CreateCore;
import whitevoid.create.editor.geometry.MeshSelectionMode;
import whitevoid.create.editor.transform.TransformMode;
import whitevoid.create.editor.viewport.ViewportContext;
import whitevoid.create.model.MeshGeometry;
import whitevoid.create.model.ModelNode;

final class CreateTopologyPathInteractionController {
    private final CreateViewportSelectionController selectionController;

    CreateTopologyPathInteractionController(CreateViewportSelectionController selectionController) {
        this.selectionController = selectionController;
    }

    boolean handleLeftClick(CreateCore core, CreateViewportInteractionState interaction,
                            double mouseX, double mouseY, int width, int height) {
        if (!interaction.topologyPathPickArmed || !interaction.topologyPathSecondPick) return false;

        ViewportContext viewport = core.editorContext().viewport();
        ModelNode node = viewport.selection().first(core.editorContext().model());
        if (node == null || viewport.transform().mode() != TransformMode.GEOMETRY
                || viewport.meshComponentSelection().mode() != MeshSelectionMode.VERTEX) return false;

        MeshGeometry mesh = node.ensureMeshGeometry();
        if (mesh == null) return false;

        int hit = selectionController.hitTestMeshVertex(node, mesh, viewport, mouseX, mouseY, width, height);
        if (hit < 0) return false;

        if (!interaction.topologyPathHasStart) {
            viewport.meshComponentSelection().selectVertex(node, hit);
            interaction.topologyPathHasStart = true;
            interaction.topologyPathStartIndex = hit;
            return true;
        }

        viewport.meshComponentSelection().selectShortestVertexPath(
                node, interaction.topologyPathStartIndex, hit);
        interaction.topologyPathPickArmed = false;
        interaction.topologyPathSecondPick = false;
        interaction.topologyPathHasStart = false;
        interaction.topologyPathStartIndex = -1;
        return true;
    }
}
