package whitevoid.create.ui;

import whitevoid.create.core.CreateCore;
import whitevoid.create.core.history.commands.ResizeCubeFaceCommand;
import whitevoid.create.core.history.commands.SetTransformCommand;
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
    /** Cancels a live gizmo drag and restores its captured state. */
    void abort(CreateViewportInteractionState interaction, ModelNode node) {
        if (!interaction.gizmoDragging) return;

        if (node != null) {
            var transform = node.transform();
            transform.position(interaction.dragOldX, interaction.dragOldY, interaction.dragOldZ);
            transform.rotation(interaction.dragOldRx, interaction.dragOldRy, interaction.dragOldRz);
            transform.scale(interaction.dragOldSx, interaction.dragOldSy, interaction.dragOldSz);

            if (interaction.dragOldGeometry != null) {
                node.setGeometry(interaction.dragOldGeometry);
            }
        }

        interaction.dragOldGeometry = null;
        interaction.gizmoDragging = false;
        interaction.activeAxis = ViewportGizmo.Axis.NONE;
    }

    boolean finish(CreateCore core, CreateViewportInteractionState interaction, ModelNode node, TransformMode mode) {
        if (!interaction.gizmoDragging) return false;

        if (node != null) {
            if (mode == TransformMode.GEOMETRY
                    && interaction.dragOldGeometry != null && node.geometry() != null) {
                var transform = node.transform();
                boolean geometryChanged = !interaction.dragOldGeometry.equals(node.geometry());
                boolean positionChanged = interaction.dragOldX != transform.x()
                        || interaction.dragOldY != transform.y()
                        || interaction.dragOldZ != transform.z();
                if (geometryChanged || positionChanged) {
                    core.editorContext().history().recordExecuted(
                            new ResizeCubeFaceCommand(
                                    node,
                                    interaction.dragOldGeometry,
                                    node.geometry(),
                                    interaction.dragOldX, interaction.dragOldY, interaction.dragOldZ,
                                    transform.x(), transform.y(), transform.z()
                            )
                    );
                }
                interaction.dragOldGeometry = null;
                interaction.gizmoDragging = false;
                interaction.activeAxis = ViewportGizmo.Axis.NONE;
                return true;
            }

            var transform = node.transform();
            boolean changed = interaction.dragOldX != transform.x()
                    || interaction.dragOldY != transform.y()
                    || interaction.dragOldZ != transform.z()
                    || interaction.dragOldRx != transform.rotationX()
                    || interaction.dragOldRy != transform.rotationY()
                    || interaction.dragOldRz != transform.rotationZ()
                    || interaction.dragOldSx != transform.scaleX()
                    || interaction.dragOldSy != transform.scaleY()
                    || interaction.dragOldSz != transform.scaleZ();
            if (changed) {
                core.editorContext().history().recordExecuted(new SetTransformCommand(
                        node,
                        interaction.dragOldX, interaction.dragOldY, interaction.dragOldZ,
                        interaction.dragOldRx, interaction.dragOldRy, interaction.dragOldRz,
                        interaction.dragOldSx, interaction.dragOldSy, interaction.dragOldSz,
                        transform.x(), transform.y(), transform.z(),
                        transform.rotationX(), transform.rotationY(), transform.rotationZ(),
                        transform.scaleX(), transform.scaleY(), transform.scaleZ(), true
                ));
            }
        }

        interaction.gizmoDragging = false;
        interaction.activeAxis = ViewportGizmo.Axis.NONE;
        return true;
    }

}
