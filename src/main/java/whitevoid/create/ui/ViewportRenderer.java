package whitevoid.create.ui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import whitevoid.create.editor.viewport.ViewportContext;
import whitevoid.create.model.Model;
import whitevoid.create.model.ModelNode;
import whitevoid.create.editor.transform.TransformMode;
import whitevoid.create.editor.geometry.GeometryFace;
import whitevoid.create.ui.ComponentTransformGizmo.Operation;

public final class ViewportRenderer {
    private final CreateViewportModelRenderer modelRenderer = new CreateViewportModelRenderer();
    private final CreateViewportGridRenderer gridRenderer = new CreateViewportGridRenderer();
    private final CreateViewportSelectionOverlayRenderer selectionOverlayRenderer = new CreateViewportSelectionOverlayRenderer();
    private final CreateViewportGizmoRenderer gizmoRenderer = new CreateViewportGizmoRenderer();
    private final CreateViewportComponentGizmoRenderer componentGizmoRenderer = new CreateViewportComponentGizmoRenderer();
    private final CreateViewportGeometryHandlesRenderer geometryHandlesRenderer = new CreateViewportGeometryHandlesRenderer();

    public void render(DrawContext context, int width, int height, ViewportContext viewport, Model model,
                       ViewportGizmo.Axis hoveredAxis, GeometryFace hoveredFace, GeometryFace selectedFace,
                       int hoveredMeshFace, int hoveredMeshVertex, int hoveredMeshEdgeA, int hoveredMeshEdgeB,
                       ComponentTransformGizmo.Axis hoveredComponentAxis,
                       Operation componentOperation, ComponentTransformGizmo.PivotMode componentPivotMode,
                       boolean xrayMode) {
        int left = 16, top = 16, right = width - 16, bottom = height - 16;
        int centerX = (left + right) / 2, centerY = (top + bottom) / 2;
        context.fill(left, top, right, bottom, 0xFF111216);

        ViewportProjector projector = new ViewportProjector(viewport.viewport().camera());
        if (viewport.viewport().gridVisible()) {
            gridRenderer.renderGrid(context, projector, centerX, centerY, left, top, right, bottom);
        }
        gridRenderer.renderAxes(context, projector, centerX, centerY, left, top, right, bottom);

        modelRenderer.render(context, projector, model, viewport,
                centerX, centerY, left, top, right, bottom);

        ModelNode selected = viewport.selection().first(model);
        if (selected != null && viewport.transform().mode() != whitevoid.create.editor.transform.TransformMode.SELECT) {
            drawGizmo(context, projector, selected, viewport.transform().mode(), centerX, centerY, left, top, right, bottom,
                    hoveredAxis);
        }
        if (selected != null && viewport.transform().mode() == TransformMode.GEOMETRY) {
            selectionOverlayRenderer.render(context, projector, selected, viewport,
                    hoveredFace, selectedFace,
                    hoveredMeshFace, hoveredMeshVertex, hoveredMeshEdgeA, hoveredMeshEdgeB,
                    xrayMode, centerX, centerY, left, top, right, bottom);
            drawComponentGizmo(context, projector, selected, viewport,
                    centerX, centerY, left, top, right, bottom,
                    hoveredComponentAxis, componentOperation, componentPivotMode);
            if (selected.meshGeometry() == null) {
                geometryHandlesRenderer.render(context, projector, selected,
                        centerX, centerY, left, top, right, bottom, hoveredAxis);
            }
        }
        var textRenderer = MinecraftClient.getInstance().textRenderer;
        context.drawTextWithShadow(textRenderer, "CREATE • Model Viewport", left + 10, top + 10, 0xFFE8E8E8);
        context.drawTextWithShadow(textRenderer,
                viewport.viewport().camera().mode().name() + " | Zoom " +
                        String.format("%.2f", viewport.viewport().camera().distance()),
                left + 10, top + 25, 0xFFAAAAAA);
    }



