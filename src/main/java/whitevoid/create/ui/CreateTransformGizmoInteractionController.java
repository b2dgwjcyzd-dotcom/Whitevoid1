package whitevoid.create.ui;

import whitevoid.create.core.CreateCore;
import whitevoid.create.editor.transform.TransformMode;
import whitevoid.create.editor.viewport.ViewportContext;
import whitevoid.create.model.ModelNode;

final class CreateTransformGizmoInteractionController {
    private final ViewportGizmo gizmo;

    CreateTransformGizmoInteractionController(ViewportGizmo gizmo) {
        this.gizmo = gizmo;
    }

    boolean begin(
            CreateCore core,
            CreateViewportInteractionState interaction,
            ModelNode selected,
            ViewportContext viewport,
            TransformMode mode,
            double mouseX,
            double mouseY,
            int centerX,
            int centerY
    ) {
        if (selected == null || mode == TransformMode.SELECT) return false;

        interaction.activeAxis = gizmo.hit(
                selected,
                mode,
                new ViewportProjector(viewport.viewport().camera()),
                mouseX, mouseY, centerX, centerY
        );
        interaction.gizmoDragging = interaction.activeAxis != ViewportGizmo.Axis.NONE;
        interaction.hoveredAxis = interaction.activeAxis;

        if (!interaction.gizmoDragging) return false;

        var transform = selected.transform();
        interaction.dragOldX = transform.x();
        interaction.dragOldY = transform.y();
        interaction.dragOldZ = transform.z();
        interaction.dragOldRx = transform.rotationX();
        interaction.dragOldRy = transform.rotationY();
        interaction.dragOldRz = transform.rotationZ();
        interaction.dragOldSx = transform.scaleX();
        interaction.dragOldSy = transform.scaleY();
        interaction.dragOldSz = transform.scaleZ();
        return true;
    }
}
