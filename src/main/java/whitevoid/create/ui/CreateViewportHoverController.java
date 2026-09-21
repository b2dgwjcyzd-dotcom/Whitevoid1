package whitevoid.create.ui;

import whitevoid.create.editor.viewport.ViewportContext;
import whitevoid.create.editor.geometry.GeometryFace;
import whitevoid.create.editor.transform.TransformMode;
import whitevoid.create.model.ModelNode;

public final class CreateViewportHoverController {
    private final ViewportGizmo gizmo;
    private final ComponentTransformGizmo componentGizmo;
    private final ComponentTransformController componentTransform;
    private final MeshEditorHoverController meshEditorHover;

    public CreateViewportHoverController(
            ViewportGizmo gizmo,
            ComponentTransformGizmo componentGizmo,
            ComponentTransformController componentTransform,
            MeshEditorHoverController meshEditorHover
    ) {
        this.gizmo = gizmo;
        this.componentGizmo = componentGizmo;
        this.componentTransform = componentTransform;
        this.meshEditorHover = meshEditorHover;
    }

    public void update(
            CreateViewportInteractionState interaction,
            ViewportContext viewport,
            ModelNode selected,
            double mouseX,
            double mouseY,
            int width,
            int height
    ) {
        if (selected == null) {
            interaction.hoveredAxis = ViewportGizmo.Axis.NONE;
            interaction.hoveredComponentAxis = ComponentTransformGizmo.Axis.NONE;
            clearMeshHover(interaction);
            return;
        }

        ViewportProjector projector = new ViewportProjector(viewport.viewport().camera());
        int centerX = width / 2;
        int centerY = height / 2;

        if (viewport.transform().mode() == TransformMode.GEOMETRY) {
            interaction.hoveredAxis = gizmo.geometryHit(
                    selected, projector, mouseX, mouseY, centerX, centerY);

            if (viewport.meshComponentSelection().matches(selected)
                    && viewport.meshComponentSelection().size() > 0) {
                interaction.hoveredComponentAxis = componentGizmo.hover(
                        selected,
                        viewport.meshComponentSelection().mode(),
                        viewport.meshComponentSelection().vertexIndices(),
                        viewport.meshComponentSelection().edgeIndices(),
                        viewport.meshComponentSelection().faceIndices(),
                        projector,
                        mouseX,
                        mouseY,
                        centerX,
                        centerY,
                        componentTransform.operation(),
                        componentTransform.pivotMode(),
                        viewport.meshComponentSelection()
                );
            } else {
                interaction.hoveredComponentAxis = ComponentTransformGizmo.Axis.NONE;
            }

            MeshEditorHoverController.HoverResult meshHover = meshEditorHover.resolve(
                    selected,
                    viewport,
                    mouseX,
                    mouseY,
                    centerX,
                    centerY,
                    interaction.hoveredAxis,
                    interaction.hoveredComponentAxis
            );
            interaction.hoveredMeshFace = meshHover.face();
            interaction.hoveredMeshVertex = meshHover.vertex();
            interaction.hoveredMeshEdgeA = meshHover.edgeA();
            interaction.hoveredMeshEdgeB = meshHover.edgeB();
            interaction.hoveredFace = meshEditorHover.primitiveFace(
                    selected,
                    viewport,
                    mouseX,
                    mouseY,
                    centerX,
                    centerY,
                    interaction.hoveredAxis,
                    interaction.hoveredComponentAxis
            );
            return;
        }

        if (viewport.transform().mode() != TransformMode.SELECT) {
            interaction.hoveredAxis = gizmo.hoveredAxis(
                    selected,
                    viewport.transform().mode(),
                    projector,
                    mouseX,
                    mouseY,
                    centerX,
                    centerY
            );
        } else {
            interaction.hoveredAxis = ViewportGizmo.Axis.NONE;
        }

        interaction.hoveredComponentAxis = ComponentTransformGizmo.Axis.NONE;
        clearMeshHover(interaction);
    }

    private static void clearMeshHover(CreateViewportInteractionState interaction) {
        interaction.hoveredMeshFace = -1;
        interaction.hoveredMeshVertex = -1;
        interaction.hoveredMeshEdgeA = -1;
        interaction.hoveredMeshEdgeB = -1;
        interaction.hoveredFace = GeometryFace.NONE;
    }
}
