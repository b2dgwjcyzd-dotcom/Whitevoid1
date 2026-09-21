package whitevoid.create.ui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import whitevoid.create.core.CreateCore;
import whitevoid.create.editor.geometry.MeshComponentSelection;
import whitevoid.create.editor.geometry.MeshSelectionMode;
import whitevoid.create.editor.transform.TransformMode;
import whitevoid.create.editor.viewport.ViewportContext;
import whitevoid.create.model.ModelNode;

final class CreateViewportOverlayRenderer {
    void renderStatus(
            DrawContext context,
            int width,
            int height,
            CreateCore core,
            CreateViewportInteractionState interaction,
            ComponentTransformController componentTransform,
            ComponentTransformInputController componentTransformInput,
            boolean controlDown
    ) {
        ViewportContext viewport = core.editorContext().viewport();
        if (viewport.transform().mode() != TransformMode.GEOMETRY
                || viewport.meshComponentSelection().size() <= 0) {
            return;
        }

        var textRenderer = MinecraftClient.getInstance().textRenderer;
        String mode = viewport.meshComponentSelection().mode().name();
        String operation = componentTransform.operation().name();
        String axis = componentTransform.axis() == ComponentTransformGizmo.Axis.NONE
                ? (interaction.hoveredComponentAxis == ComponentTransformGizmo.Axis.NONE
                ? "" : " " + interaction.hoveredComponentAxis.name())
                : " " + componentTransform.axis().name();
        String pivot = componentTransform.pivotMode().name().replace('_', ' ');
        String active = switch (activeComponentLabel(viewport.meshComponentSelection())) {
            case null -> "";
            case String value -> " • Active " + value;
        };
        String constraint = componentTransform.constraintAxis() == ComponentTransformGizmo.Axis.NONE
                ? ""
                : " • " + (componentTransform.planeConstraint() ? "PLANE " : "")
                + componentTransform.constraintAxis().name();
        String snap = controlDown ? " • SNAP" : "";
        String numeric = componentTransformInput.numericEntry()
                ? " • Value " + (componentTransformInput.numericNegative() ? "-" : "")
                + componentTransformInput.numericBuffer()
                : "";
        String proportional = interaction.proportionalEditing
                ? " • PROP " + String.format(java.util.Locale.ROOT, "%.1f", interaction.proportionalRadius)
                : "";
        String hover = interaction.hoveredMeshVertex >= 0
                ? " • Hover V" + interaction.hoveredMeshVertex
                : interaction.hoveredMeshEdgeA >= 0
                ? " • Hover E" + interaction.hoveredMeshEdgeA + "-" + interaction.hoveredMeshEdgeB
                : interaction.hoveredMeshFace >= 0
                ? " • Hover F" + interaction.hoveredMeshFace
                : "";
        String xray = interaction.selectThrough ? " • X-RAY" : "";
        String topology = interaction.topologyPathPickArmed
                ? " • PATH: " + (interaction.topologyPathHasStart ? "pick target" : "pick start")
                : "";

        context.drawTextWithShadow(
                textRenderer,
                operation + axis + constraint + " • " + mode + " • Pivot " + pivot
                        + active + snap + numeric + proportional + hover + xray + topology,
                26, height - 30, 0xFFE8E8E8
        );
    }

    void renderComponentSelectionBox(
            DrawContext context,
            CreateViewportInteractionState interaction
    ) {
        if (!interaction.componentBoxSelecting) {
            return;
        }

        int left = (int) Math.round(Math.min(interaction.boxStartX, interaction.boxCurrentX));
        int top = (int) Math.round(Math.min(interaction.boxStartY, interaction.boxCurrentY));
        int right = (int) Math.round(Math.max(interaction.boxStartX, interaction.boxCurrentX));
        int bottom = (int) Math.round(Math.max(interaction.boxStartY, interaction.boxCurrentY));

        context.fill(left, top, right, top + 1, 0xFFFFFFFF);
        context.fill(left, bottom, right, bottom + 1, 0xFFFFFFFF);
        context.fill(left, top, left + 1, bottom, 0xFFFFFFFF);
        context.fill(right, top, right + 1, bottom, 0xFFFFFFFF);
    }

    private static String activeComponentLabel(MeshComponentSelection selection) {
        return switch (selection.mode()) {
            case VERTEX -> selection.activeVertex() >= 0 ? "V" + selection.activeVertex() : null;
            case EDGE -> selection.activeEdgeA() >= 0
                    ? "E" + selection.activeEdgeA() + "-" + selection.activeEdgeB()
                    : null;
            case FACE -> selection.activeFace() >= 0 ? "F" + selection.activeFace() : null;
        };
    }
}
