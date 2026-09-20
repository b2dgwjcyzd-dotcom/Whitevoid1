package whitevoid.create.ui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import whitevoid.create.core.CreateCore;
import whitevoid.create.core.history.commands.SetTransformCommand;
import whitevoid.create.editor.selection.SelectionMode;
import whitevoid.create.editor.transform.TransformMode;
import whitevoid.create.editor.viewport.ViewportContext;
import whitevoid.create.model.ModelNode;

public final class CreateScreen extends Screen {
    private final CreateCore core;
    private final ViewportRenderer viewportRenderer = new ViewportRenderer();
    private final CreateViewportInput viewportInput;
    private final ViewportGizmo gizmo = new ViewportGizmo();
    private ViewportGizmo.Axis activeAxis = ViewportGizmo.Axis.NONE;
    private boolean gizmoDragging;
    private double dragOldX,dragOldY,dragOldZ,dragOldRx,dragOldRy,dragOldRz,dragOldSx,dragOldSy,dragOldSz;

    public CreateScreen(CreateCore core) {
        super(Text.literal("CREATE"));
        this.core = core;
        this.viewportInput = new CreateViewportInput(core.editorContext().viewport().viewport().camera());
    }

    @Override protected void init() {
        core.editorContext().setEditing(true);
    }

    @Override public void close() {
        viewportInput.cancelDrag();
        core.editorContext().setEditing(false);
        super.close();
    }

    @Override public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        ViewportContext viewport = core.editorContext().viewport();

        if (keyCode == 71) { viewport.transform().setMode(TransformMode.MOVE); return true; }
        if (keyCode == 82) { viewport.transform().setMode(TransformMode.ROTATE); return true; }
        if (keyCode == 83) { viewport.transform().setMode(TransformMode.SCALE); return true; }

        if (keyCode == 90 && hasControlDown()) {
            if (hasShiftDown()) viewportContextHistoryRedo();
            else viewportContextHistoryUndo();
            return true;
        }
        if (keyCode == 89 && hasControlDown()) {
            viewportContextHistoryRedo();
            return true;
        }

        ModelNode node = viewport.selection().first(core.editorContext().model());
        if (node != null) {
            double step = hasShiftDown() ? 0.1 : 1.0;
            if (viewport.transform().mode() == TransformMode.MOVE) {
                if (keyCode == 263) { transformMove(node, -step, 0, 0); return true; }
                if (keyCode == 262) { transformMove(node, step, 0, 0); return true; }
                if (keyCode == 264) { transformMove(node, 0, 0, step); return true; }
                if (keyCode == 265) { transformMove(node, 0, 0, -step); return true; }
                if (keyCode == 32) { transformMove(node, 0, step, 0); return true; }
            }
            if (viewport.transform().mode() == TransformMode.ROTATE) {
                if (keyCode == 263) { transformRotate(node, 0, -5, 0); return true; }
                if (keyCode == 262) { transformRotate(node, 0, 5, 0); return true; }
                if (keyCode == 264) { transformRotate(node, -5, 0, 0); return true; }
                if (keyCode == 265) { transformRotate(node, 5, 0, 0); return true; }
            }
            if (viewport.transform().mode() == TransformMode.SCALE) {
                if (keyCode == 263 || keyCode == 264) { transformScale(node, -0.1, -0.1, -0.1); return true; }
                if (keyCode == 262 || keyCode == 265) { transformScale(node, 0.1, 0.1, 0.1); return true; }
            }
        }

        if (keyCode == 27) viewport.transform().setMode(TransformMode.SELECT);
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private void transformMove(ModelNode node, double dx, double dy, double dz) {
        var t=node.transform();
        core.editorContext().history().execute(new SetTransformCommand(node,
                t.x()+dx,t.y()+dy,t.z()+dz,t.rotationX(),t.rotationY(),t.rotationZ(),t.scaleX(),t.scaleY(),t.scaleZ()));
    }

    private void transformRotate(ModelNode node, double dx, double dy, double dz) {
        var t=node.transform();
        core.editorContext().history().execute(new SetTransformCommand(node,
                t.x(),t.y(),t.z(),t.rotationX()+dx,t.rotationY()+dy,t.rotationZ()+dz,t.scaleX(),t.scaleY(),t.scaleZ()));
    }

    private void transformScale(ModelNode node, double dx, double dy, double dz) {
        var t=node.transform();
        core.editorContext().history().execute(new SetTransformCommand(node,
                t.x(),t.y(),t.z(),t.rotationX(),t.rotationY(),t.rotationZ(),
                Math.max(.01,t.scaleX()+dx),Math.max(.01,t.scaleY()+dy),Math.max(.01,t.scaleZ()+dz)));
    }

    private void viewportContextHistoryUndo() { core.editorContext().history().undo(); }
    private void viewportContextHistoryRedo() { core.editorContext().history().redo(); }

    @Override public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (viewportInput.mouseClicked(mouseX, mouseY, button)) return true;
        if (button == 0) {
            ViewportContext viewport = core.editorContext().viewport();
            ModelNode selected = viewport.selection().first(core.editorContext().model());
            if (selected != null && viewport.transform().mode() != TransformMode.SELECT) {
                int cx=width/2, cy=height/2;
                activeAxis = gizmo.hit(selected, viewport.transform().mode(),
                        new ViewportProjector(viewport.viewport().camera()), mouseX, mouseY, cx, cy);
                gizmoDragging = activeAxis != ViewportGizmo.Axis.NONE;
                if (gizmoDragging) {
                    var t=selected.transform();
                    dragOldX=t.x(); dragOldY=t.y(); dragOldZ=t.z();
                    dragOldRx=t.rotationX(); dragOldRy=t.rotationY(); dragOldRz=t.rotationZ();
                    dragOldSx=t.scaleX(); dragOldSy=t.scaleY(); dragOldSz=t.scaleZ();
                    return true;
                }
            }
            ModelNode hit = new ViewportPicker().pick(core.editorContext().model(), core.editorContext().viewport(), mouseX, mouseY, width, height);
            if (hit != null) core.editorContext().viewport().selection().select(hit, SelectionMode.SINGLE);
            else core.editorContext().viewport().selection().clear();
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (gizmoDragging && button == 0) {
            ModelNode node = core.editorContext().viewport().selection().first(core.editorContext().model());
            if (node != null) {
                var t=node.transform();
                boolean changed = dragOldX!=t.x() || dragOldY!=t.y() || dragOldZ!=t.z() ||
                        dragOldRx!=t.rotationX() || dragOldRy!=t.rotationY() || dragOldRz!=t.rotationZ() ||
                        dragOldSx!=t.scaleX() || dragOldSy!=t.scaleY() || dragOldSz!=t.scaleZ();
                if (changed) {
                    core.editorContext().history().recordExecuted(new SetTransformCommand(node,
                            dragOldX,dragOldY,dragOldZ,dragOldRx,dragOldRy,dragOldRz,dragOldSx,dragOldSy,dragOldSz,
                            t.x(),t.y(),t.z(),t.rotationX(),t.rotationY(),t.rotationZ(),t.scaleX(),t.scaleY(),t.scaleZ(),true));
                }
            }
            gizmoDragging=false;
            activeAxis=ViewportGizmo.Axis.NONE;
            return true;
        }
        if (viewportInput.mouseReleased(mouseX, mouseY, button)) return true;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (gizmoDragging && button == 0) {
            ModelNode node = core.editorContext().viewport().selection().first(core.editorContext().model());
            if (node != null) {
                double amount = gizmo.dragAmount(activeAxis, new ViewportProjector(core.editorContext().viewport().viewport().camera()), deltaX, deltaY);
                var t=node.transform();
                if (core.editorContext().viewport().transform().mode() == TransformMode.MOVE) {
                    double dx=activeAxis==ViewportGizmo.Axis.X?amount:0;
                    double dy=activeAxis==ViewportGizmo.Axis.Y?amount:0;
                    double dz=activeAxis==ViewportGizmo.Axis.Z?amount:0;
                    core.editorContext().viewport().transform().translate(node,dx,dy,dz);
                } else if (core.editorContext().viewport().transform().mode() == TransformMode.ROTATE) {
                    double rx=activeAxis==ViewportGizmo.Axis.X?amount*10:0;
                    double ry=activeAxis==ViewportGizmo.Axis.Y?amount*10:0;
                    double rz=activeAxis==ViewportGizmo.Axis.Z?amount*10:0;
                    core.editorContext().viewport().transform().rotateBy(node,rx,ry,rz);
                } else if (core.editorContext().viewport().transform().mode() == TransformMode.SCALE) {
                    double s=amount*0.1;
                    core.editorContext().viewport().transform().scaleBy(node,
                            activeAxis==ViewportGizmo.Axis.X?s:0,
                            activeAxis==ViewportGizmo.Axis.Y?s:0,
                            activeAxis==ViewportGizmo.Axis.Z?s:0);
                }
            }
            return true;
        }
        if (viewportInput.mouseDragged(mouseX, mouseY, button, hasShiftDown())) return true;
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (viewportInput.mouseScrolled(verticalAmount)) return true;
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context, mouseX, mouseY, delta);
        viewportRenderer.render(context, width, height, core.editorContext().viewport(), core.editorContext().model());
        super.render(context, mouseX, mouseY, delta);
    }

    @Override public boolean shouldPause() { return false; }
}
