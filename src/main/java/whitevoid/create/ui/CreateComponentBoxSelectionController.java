package whitevoid.create.ui;

import whitevoid.create.core.CreateCore;
import whitevoid.create.editor.viewport.ViewportContext;
import whitevoid.create.model.ModelNode;

final class CreateComponentBoxSelectionController {
    boolean begin(
            CreateCore core,
            CreateViewportInteractionState interaction,
            ModelNode selected,
            ViewportContext viewport,
            double mouseX,
            double mouseY
    ) {
        if (selected == null || viewport.transform().mode() != TransformMode.GEOMETRY) return false;
        interaction.componentBoxSelecting = true;
        interaction.boxStartX = interaction.boxCurrentX = mouseX;
        interaction.boxStartY = interaction.boxCurrentY = mouseY;
        return true;
    }

    boolean update(
            CreateViewportInteractionState interaction,
            double mouseX,
            double mouseY
    ) {
        if (!interaction.componentBoxSelecting) return false;
        interaction.boxCurrentX = mouseX;
        interaction.boxCurrentY = mouseY;
        return true;
    }

    boolean finish(
            CreateCore core,
            CreateViewportInteractionState interaction,
            ModelNode selected,
            ViewportContext viewport,
            double mouseX,
            double mouseY,
            int width,
            int height,
            boolean altDown,
            boolean shiftDown,
            CreateViewportSelectionController selectionController
    ) {
        if (!interaction.componentBoxSelecting) return false;
        interaction.boxCurrentX = mouseX;
        interaction.boxCurrentY = mouseY;
        selectionController.selectComponentsInBox(
                viewport, selected,
                interaction.boxStartX, interaction.boxStartY,
                interaction.boxCurrentX, interaction.boxCurrentY,
                width, height, altDown, shiftDown);
        interaction.componentBoxSelecting = false;
        return true;
    }
}
