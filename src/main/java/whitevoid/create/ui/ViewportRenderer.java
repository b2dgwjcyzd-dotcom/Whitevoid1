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
                drawGeometryHandles(context, projector, selected,
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



    private void drawGizmo(DrawContext context, ViewportProjector projector, ModelNode node,
                           TransformMode mode, int cx, int cy, int left, int top, int right, int bottom,
                           ViewportGizmo.Axis hoveredAxis) {
        gizmoRenderer.render(context, projector, node, mode, cx, cy, left, top, right, bottom, hoveredAxis);
    }

    private void drawComponentGizmo(DrawContext context, ViewportProjector projector, ModelNode node,
                                    ViewportContext viewport, int cx, int cy, int left, int top, int right, int bottom,
                                    ComponentTransformGizmo.Axis hoveredAxis, Operation operation,
                                    ComponentTransformGizmo.PivotMode pivotMode) {
        componentGizmoRenderer.render(context, projector, node, viewport,
                cx, cy, left, top, right, bottom, hoveredAxis, operation, pivotMode);
    }


    private void drawLine(DrawContext context, Point a, Point b, int left, int top,
                          int right, int bottom, int color) {
        int x0=(int)Math.round(a.x()), y0=(int)Math.round(a.y());
        int x1=(int)Math.round(b.x()), y1=(int)Math.round(b.y());
        int dx=Math.abs(x1-x0), dy=Math.abs(y1-y0);
        int sx=x0<x1?1:-1, sy=y0<y1?1:-1, err=dx-dy;
        while(true) {
            if(x0>=left&&x0<right&&y0>=top&&y0<bottom) context.fill(x0,y0,x0+1,y0+1,color);
            if(x0==x1&&y0==y1) break;
            int e2=2*err;
            if(e2>-dy){err-=dy;x0+=sx;}
            if(e2<dx){err+=dx;y0+=sy;}
        }
    }
}
