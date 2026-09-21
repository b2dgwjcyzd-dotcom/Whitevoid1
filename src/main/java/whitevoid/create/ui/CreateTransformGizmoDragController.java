package whitevoid.create.ui;

import whitevoid.create.core.CreateCore;
import whitevoid.create.editor.transform.TransformMode;
import whitevoid.create.editor.viewport.ViewportContext;
import whitevoid.create.model.CubeGeometry;
import whitevoid.create.model.ModelNode;

final class CreateTransformGizmoDragController {
    private final ViewportGizmo gizmo;

    CreateTransformGizmoDragController(ViewportGizmo gizmo) {
        this.gizmo = gizmo;
    }

    boolean update(
            CreateCore core,
            CreateViewportInteractionState interaction,
            ModelNode node,
            ViewportContext viewport,
            double deltaX,
            double deltaY
    ) {
        if (!interaction.gizmoDragging || node == null) return false;

        double amount = gizmo.dragAmount(
                interaction.activeAxis,
                new ViewportProjector(viewport.viewport().camera()),
                deltaX,
                deltaY
        );

        TransformMode mode = viewport.transform().mode();
        if (mode == TransformMode.GEOMETRY) {
            var geometry = node.geometry();
            if (geometry == null) return true;

            double width = geometry.width();
            double height = geometry.height();
            double depth = geometry.depth();
            double sign = (interaction.activeAxis == ViewportGizmo.Axis.NEG_X
                    || interaction.activeAxis == ViewportGizmo.Axis.NEG_Y
                    || interaction.activeAxis == ViewportGizmo.Axis.NEG_Z) ? -1.0 : 1.0;
            double move = amount * 2.0 * sign;

            if (interaction.activeAxis == ViewportGizmo.Axis.X
                    || interaction.activeAxis == ViewportGizmo.Axis.NEG_X) {
                width = Math.max(0.1, width + move);
                node.transform().position(
                        node.transform().x() + amount * sign,
                        node.transform().y(),
                        node.transform().z()
                );
            } else if (interaction.activeAxis == ViewportGizmo.Axis.Y
                    || interaction.activeAxis == ViewportGizmo.Axis.NEG_Y) {
                height = Math.max(0.1, height + move);
                node.transform().position(
                        node.transform().x(),
                        node.transform().y() + amount * sign,
                        node.transform().z()
                );
            } else if (interaction.activeAxis == ViewportGizmo.Axis.Z
                    || interaction.activeAxis == ViewportGizmo.Axis.NEG_Z) {
                depth = Math.max(0.1, depth + move);
                node.transform().position(
                        node.transform().x(),
                        node.transform().y(),
                        node.transform().z() + amount * sign
                );
            }

            node.setGeometry(new CubeGeometry(width, height, depth));
            return true;
        }

        if (mode == TransformMode.MOVE) {
            viewport.transform().translate(
                    node,
                    interaction.activeAxis == ViewportGizmo.Axis.X ? amount : 0,
                    interaction.activeAxis == ViewportGizmo.Axis.Y ? amount : 0,
                    interaction.activeAxis == ViewportGizmo.Axis.Z ? amount : 0
            );
        } else if (mode == TransformMode.ROTATE) {
            viewport.transform().rotateBy(
                    node,
                    interaction.activeAxis == ViewportGizmo.Axis.X ? amount * 10 : 0,
                    interaction.activeAxis == ViewportGizmo.Axis.Y ? amount * 10 : 0,
                    interaction.activeAxis == ViewportGizmo.Axis.Z ? amount * 10 : 0
            );
        } else if (mode == TransformMode.SCALE) {
            double scale = amount * 0.1;
            viewport.transform().scaleBy(
                    node,
                    interaction.activeAxis == ViewportGizmo.Axis.X ? scale : 0,
                    interaction.activeAxis == ViewportGizmo.Axis.Y ? scale : 0,
                    interaction.activeAxis == ViewportGizmo.Axis.Z ? scale : 0
            );
        }
        return true;
    }
}
