package whitevoid.create.ui;

import whitevoid.create.core.CreateCore;
import whitevoid.create.model.ModelNode;

final class CreateMeshComponentDragFinishController {
    boolean finish(
            CreateCore core,
            CreateViewportInteractionState interaction,
            ModelNode node,
            MeshComponentDragController drag
    ) {
        if (interaction.vertexDragging) {
            drag.finish(core, node);
            interaction.vertexDragging = false;
            interaction.activeVertex = -1;
            interaction.vertexDragOldMesh = null;
            return true;
        }
        if (interaction.edgeDragging) {
            drag.finish(core, node);
            interaction.edgeDragging = false;
            interaction.activeEdgeA = -1;
            interaction.activeEdgeB = -1;
            interaction.edgeDragOldMesh = null;
            return true;
        }
        return false;
    }
}
