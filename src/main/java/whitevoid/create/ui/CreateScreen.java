package whitevoid.create.ui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;
import whitevoid.create.core.CreateCore;
import whitevoid.create.core.history.commands.AddCubeCommand;
import whitevoid.create.core.history.commands.DeleteNodeCommand;
import whitevoid.create.core.history.commands.DuplicateNodeCommand;
import whitevoid.create.core.history.commands.SetTransformCommand;
import whitevoid.create.core.history.commands.SetCubeGeometryCommand;
import whitevoid.create.core.history.commands.ResizeCubeFaceCommand;
import whitevoid.create.core.history.commands.SetMeshGeometryCommand;
import whitevoid.create.editor.geometry.MeshOperations;
import whitevoid.create.editor.selection.SelectionMode;
import whitevoid.create.editor.geometry.GeometryFace;
import whitevoid.create.editor.transform.TransformMode;
import whitevoid.create.editor.viewport.ViewportContext;
import whitevoid.create.model.CubeGeometry;
import whitevoid.create.model.ModelNode;

public final class CreateScreen extends Screen {
    private final CreateCore core;
    private final ViewportRenderer viewportRenderer = new ViewportRenderer();
    private final CreateViewportInput viewportInput;
    private final ViewportGizmo gizmo = new ViewportGizmo();
    private ViewportGizmo.Axis activeAxis = ViewportGizmo.Axis.NONE;
    private boolean gizmoDragging;
    private ViewportGizmo.Axis hoveredAxis = ViewportGizmo.Axis.NONE;
    private double dragOldX,dragOldY,dragOldZ,dragOldRx,dragOldRy,dragOldRz,dragOldSx,dragOldSy,dragOldSz;
    private CubeGeometry dragOldGeometry;
    private GeometryFace hoveredFace = GeometryFace.NONE;
    private boolean faceDragging;
    private GeometryFace activeFace = GeometryFace.NONE;
    private int hoveredMeshFace = -1;

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
        if (keyCode == GLFW.GLFW_KEY_B) {
            viewport.transform().setMode(TransformMode.GEOMETRY);
            viewport.geometryFaceSelection().clear();
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_E && viewport.transform().mode() == TransformMode.GEOMETRY) {
            extrudeSelectedFace(hasShiftDown() ? 1.0 : 0.25);
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_I && viewport.transform().mode() == TransformMode.GEOMETRY) {
            insetSelectedFace(hasShiftDown() ? 0.5 : 0.25);
            return true;
        }

        if (keyCode == 90 && hasControlDown()) {
            if (hasShiftDown()) viewportContextHistoryRedo();
            else viewportContextHistoryUndo();
            return true;
        }
        if (keyCode == 89 && hasControlDown()) {
            viewportContextHistoryRedo();
            return true;
        }

        if (keyCode == GLFW.GLFW_KEY_N) {
            addCube();
            return true;
        }

        if (keyCode == GLFW.GLFW_KEY_DELETE || keyCode == GLFW.GLFW_KEY_BACKSPACE) {
            deleteSelectedNode();
            return true;
        }

        if (keyCode == GLFW.GLFW_KEY_D && hasControlDown()) {
            duplicateSelectedNode();
            return true;
        }

        if (keyCode == GLFW.GLFW_KEY_LEFT_BRACKET || keyCode == GLFW.GLFW_KEY_RIGHT_BRACKET) {
            resizeSelectedCube(keyCode == GLFW.GLFW_KEY_RIGHT_BRACKET);
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

    private void insetSelectedFace(double amount) {
        var viewport = core.editorContext().viewport();
        var model = core.editorContext().model();
        var node = viewport.meshFaceSelection().node(model);
        int faceIndex = viewport.meshFaceSelection().faceIndex();
        if (node == null || faceIndex < 0) return;

        var oldMesh = node.ensureMeshGeometry();
        if (oldMesh == null || faceIndex >= oldMesh.faces().size()) return;

        var newMesh = MeshOperations.insetFace(oldMesh, faceIndex, amount);
        core.editorContext().history().execute(
                new SetMeshGeometryCommand(node, oldMesh.copy(), newMesh)
        );

        viewport.meshFaceSelection().select(node, newMesh.faces().size() - 1);
        viewport.geometryFaceSelection().clear();
    }

    private void extrudeSelectedFace(double amount) {
        var viewport = core.editorContext().viewport();
        var model = core.editorContext().model();
        var node = viewport.geometryFaceSelection().node(model);
        var face = viewport.geometryFaceSelection().face();
        if (node == null || node.geometry() == null || face == GeometryFace.NONE) return;

        var oldGeometry = node.geometry();
        var t = node.transform();
        double x = t.x(), y = t.y(), z = t.z();
        double width = oldGeometry.width();
        double height = oldGeometry.height();
        double depth = oldGeometry.depth();

        switch (face) {
            case POS_X -> {
                width += amount;
                x += amount * 0.5;
            }
            case NEG_X -> {
                width += amount;
                x -= amount * 0.5;
            }
            case POS_Y -> {
                height += amount;
                y += amount * 0.5;
            }
            case NEG_Y -> {
                height += amount;
                y -= amount * 0.5;
            }
            case POS_Z -> {
                depth += amount;
                z += amount * 0.5;
            }
            case NEG_Z -> {
                depth += amount;
                z -= amount * 0.5;
            }
            case NONE -> {
                return;
            }
        }

        var newGeometry = new CubeGeometry(width, height, depth);
        core.editorContext().history().execute(
                new ResizeCubeFaceCommand(node, oldGeometry, newGeometry,
                        t.x(), t.y(), t.z(), x, y, z)
        );
    }

    private void addCube() {
        var model = core.editorContext().model();
        var selected = core.editorContext().viewport().selection().first(model);

        double x = 0.0;
        double y = 0.0;
        double z = 0.0;
        if (selected != null) {
            x = selected.transform().x() + 3.0;
            y = selected.transform().y();
            z = selected.transform().z();
        }

        var command = new AddCubeCommand(
                model,
                model.root(),
                "Cube",
                new CubeGeometry(2.0, 2.0, 2.0),
                x, y, z
        );
        core.editorContext().history().execute(command);
        core.editorContext().viewport().selection().select(
                command.createdNode(),
                SelectionMode.SINGLE
        );
        core.editorContext().viewport().transform().setMode(TransformMode.SELECT);
    }

    private void deleteSelectedNode() {
        var model = core.editorContext().model();
        var node = core.editorContext().viewport().selection().first(model);
        if (node == null || node == model.root()) return;

        core.editorContext().history().execute(new DeleteNodeCommand(model, node));
        core.editorContext().viewport().selection().clear();
        core.editorContext().viewport().transform().setMode(TransformMode.SELECT);
    }

    private void duplicateSelectedNode() {
        var model = core.editorContext().model();
        var node = core.editorContext().viewport().selection().first(model);
        if (node == null || node == model.root()) return;

        var command = new DuplicateNodeCommand(model, node);
        core.editorContext().history().execute(command);
        core.editorContext().viewport().selection().select(
                command.duplicatedNode(),
                SelectionMode.SINGLE
        );
    }

    private void resizeSelectedCube(boolean grow) {
        var node = core.editorContext().viewport().selection().first(core.editorContext().model());
        if (node == null || node.geometry() == null) return;

        var g = node.geometry();
        double step = hasShiftDown() ? 0.25 : 1.0;
        double factor = grow ? step : -step;

        double width = Math.max(0.1, g.width() + factor);
        double height = Math.max(0.1, g.height() + factor);
        double depth = Math.max(0.1, g.depth() + factor);

        core.editorContext().history().execute(
                new SetCubeGeometryCommand(node, new CubeGeometry(width, height, depth))
        );
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
            if (selected != null && viewport.transform().mode() == TransformMode.GEOMETRY) {
                int cx=width/2, cy=height/2;
                ViewportProjector projector = new ViewportProjector(viewport.viewport().camera());
                activeAxis = gizmo.geometryHit(selected, projector, mouseX, mouseY, cx, cy);
                if (activeAxis == ViewportGizmo.Axis.NONE) {
                    int clickedMeshFace = gizmo.meshFaceHit(selected, projector, mouseX, mouseY, cx, cy);
                    if (clickedMeshFace >= 0) {
                        viewport.meshFaceSelection().select(selected, clickedMeshFace);
                        viewport.geometryFaceSelection().clear();
                        hoveredMeshFace = clickedMeshFace;
                        hoveredFace = GeometryFace.NONE;
                        return true;
                    }
                    viewport.meshFaceSelection().clear();
                    GeometryFace clickedFace = gizmo.faceHit(selected, projector, mouseX, mouseY, cx, cy);
                    if (clickedFace != GeometryFace.NONE) {
                        viewport.geometryFaceSelection().select(selected, clickedFace);
                        hoveredFace = clickedFace;
                        activeFace = clickedFace;
                        faceDragging = true;
                        dragOldGeometry = selected.geometry();
                        var t = selected.transform();
                        dragOldX = t.x();
                        dragOldY = t.y();
                        dragOldZ = t.z();
                        return true;
                    }
                    viewport.geometryFaceSelection().clear();
                }
                gizmoDragging = activeAxis != ViewportGizmo.Axis.NONE;
                hoveredAxis = activeAxis;
                if (gizmoDragging) {
                    dragOldGeometry = selected.geometry();
                    return true;
                }
            } else if (selected != null && viewport.transform().mode() != TransformMode.SELECT) {
                int cx=width/2, cy=height/2;
                activeAxis = gizmo.hit(selected, viewport.transform().mode(),
                        new ViewportProjector(viewport.viewport().camera()), mouseX, mouseY, cx, cy);
                gizmoDragging = activeAxis != ViewportGizmo.Axis.NONE;
                hoveredAxis = activeAxis;
                if (gizmoDragging) {
                    var t=selected.transform();
                    dragOldX=t.x(); dragOldY=t.y(); dragOldZ=t.z();
                    dragOldRx=t.rotationX(); dragOldRy=t.rotationY(); dragOldRz=t.rotationZ();
                    dragOldSx=t.scaleX(); dragOldSy=t.scaleY(); dragOldSz=t.scaleZ();
                    return true;
                }
            }
            ModelNode hit = new ViewportPicker().pick(core.editorContext().model(), core.editorContext().viewport(), mouseX, mouseY, width, height);
            if (hit != null) {
                core.editorContext().viewport().selection().select(hit, SelectionMode.SINGLE);
                core.editorContext().viewport().geometryFaceSelection().clear();
                core.editorContext().viewport().meshFaceSelection().clear();
            } else {
                core.editorContext().viewport().selection().clear();
                core.editorContext().viewport().geometryFaceSelection().clear();
            }
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (faceDragging && button == 0) {
            ModelNode node = core.editorContext().viewport().geometryFaceSelection().node(core.editorContext().model());
            if (node != null && dragOldGeometry != null && node.geometry() != null) {
                var t = node.transform();
                boolean changed = !dragOldGeometry.equals(node.geometry())
                        || dragOldX != t.x() || dragOldY != t.y() || dragOldZ != t.z();
                if (changed) {
                    core.editorContext().history().recordExecuted(
                            new ResizeCubeFaceCommand(node,
                                    dragOldGeometry, node.geometry(),
                                    dragOldX, dragOldY, dragOldZ,
                                    t.x(), t.y(), t.z()));
                }
            }
            faceDragging = false;
            activeFace = GeometryFace.NONE;
            dragOldGeometry = null;
            return true;
        }
        if (gizmoDragging && button == 0) {
            ModelNode node = core.editorContext().viewport().selection().first(core.editorContext().model());
            if (node != null) {
                if (core.editorContext().viewport().transform().mode() == TransformMode.GEOMETRY
                        && dragOldGeometry != null && node.geometry() != null) {
                    var t = node.transform();
                    boolean geometryChanged = !dragOldGeometry.equals(node.geometry());
                    boolean positionChanged = dragOldX != t.x() || dragOldY != t.y() || dragOldZ != t.z();
                    if (geometryChanged || positionChanged) {
                        core.editorContext().history().recordExecuted(
                                new ResizeCubeFaceCommand(node,
                                        dragOldGeometry, node.geometry(),
                                        dragOldX, dragOldY, dragOldZ,
                                        t.x(), t.y(), t.z()));
                    }
                    dragOldGeometry = null;
                    gizmoDragging=false;
                    activeAxis=ViewportGizmo.Axis.NONE;
                    return true;
                }

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
        if (faceDragging && button == 0) {
            ModelNode node = core.editorContext().viewport().geometryFaceSelection().node(core.editorContext().model());
            if (node != null && node.geometry() != null && activeFace != GeometryFace.NONE) {
                ViewportProjector projector = new ViewportProjector(core.editorContext().viewport().viewport().camera());
                double amount = gizmo.faceDragAmount(activeFace, projector, deltaX, deltaY);
                var g = node.geometry();
                double width = g.width();
                double height = g.height();
                double depth = g.depth();
                double sign = switch (activeFace) {
                    case POS_X, POS_Y, POS_Z -> 1.0;
                    case NEG_X, NEG_Y, NEG_Z -> -1.0;
                    case NONE -> 0.0;
                };
                double move = amount * 2.0 * sign;
                switch (activeFace) {
                    case POS_X, NEG_X -> width = Math.max(0.1, width + move);
                    case POS_Y, NEG_Y -> height = Math.max(0.1, height + move);
                    case POS_Z, NEG_Z -> depth = Math.max(0.1, depth + move);
                    case NONE -> { return true; }
                }
                node.setGeometry(new CubeGeometry(width, height, depth));
                node.transform().position(
                        node.transform().x() + amount * sign * (activeFace == GeometryFace.POS_X || activeFace == GeometryFace.NEG_X ? 1.0 : 0.0),
                        node.transform().y() + amount * sign * (activeFace == GeometryFace.POS_Y || activeFace == GeometryFace.NEG_Y ? 1.0 : 0.0),
                        node.transform().z() + amount * sign * (activeFace == GeometryFace.POS_Z || activeFace == GeometryFace.NEG_Z ? 1.0 : 0.0)
                );
            }
            return true;
        }
        if (gizmoDragging && button == 0) {
            ModelNode node = core.editorContext().viewport().selection().first(core.editorContext().model());
            if (node != null) {
                if (core.editorContext().viewport().transform().mode() == TransformMode.GEOMETRY) {
                    var g = node.geometry();
                    if (g != null) {
                        double amount = gizmo.dragAmount(activeAxis,
                                new ViewportProjector(core.editorContext().viewport().viewport().camera()),
                                deltaX, deltaY);
                        double width = g.width();
                        double height = g.height();
                        double depth = g.depth();
                        double sign = (activeAxis == ViewportGizmo.Axis.NEG_X ||
                                activeAxis == ViewportGizmo.Axis.NEG_Y ||
                                activeAxis == ViewportGizmo.Axis.NEG_Z) ? -1.0 : 1.0;
                        double move = amount * 2.0 * sign;

                        if (activeAxis == ViewportGizmo.Axis.X || activeAxis == ViewportGizmo.Axis.NEG_X) {
                            width = Math.max(0.1, width + move);
                            node.transform().position(node.transform().x() + amount * sign,
                                    node.transform().y(), node.transform().z());
                        } else if (activeAxis == ViewportGizmo.Axis.Y || activeAxis == ViewportGizmo.Axis.NEG_Y) {
                            height = Math.max(0.1, height + move);
                            node.transform().position(node.transform().x(),
                                    node.transform().y() + amount * sign, node.transform().z());
                        } else if (activeAxis == ViewportGizmo.Axis.Z || activeAxis == ViewportGizmo.Axis.NEG_Z) {
                            depth = Math.max(0.1, depth + move);
                            node.transform().position(node.transform().x(),
                                    node.transform().y(), node.transform().z() + amount * sign);
                        }

                        node.setGeometry(new CubeGeometry(width, height, depth));
                    }
                    return true;
                } else if (core.editorContext().viewport().transform().mode() == TransformMode.MOVE) {
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

    @Override public void mouseMoved(double mouseX, double mouseY) {
        if (!gizmoDragging) {
            ViewportContext viewport=core.editorContext().viewport();
            ModelNode selected=viewport.selection().first(core.editorContext().model());
            if(selected!=null && viewport.transform().mode()==TransformMode.GEOMETRY) {
                hoveredAxis=gizmo.geometryHit(selected,new ViewportProjector(viewport.viewport().camera()),
                        mouseX,mouseY,width/2,height/2);
                hoveredMeshFace = hoveredAxis == ViewportGizmo.Axis.NONE
                        ? gizmo.meshFaceHit(selected,new ViewportProjector(viewport.viewport().camera()),
                        mouseX,mouseY,width/2,height/2)
                        : -1;
                hoveredFace = GeometryFace.NONE;
            } else if(selected!=null && viewport.transform().mode()!=TransformMode.SELECT) {
                hoveredAxis=gizmo.hoveredAxis(selected,viewport.transform().mode(),
                        new ViewportProjector(viewport.viewport().camera()),mouseX,mouseY,width/2,height/2);
            } else hoveredAxis=ViewportGizmo.Axis.NONE;
        }
        super.mouseMoved(mouseX, mouseY);
    }

    @Override public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context, mouseX, mouseY, delta);
        GeometryFace selectedFace = core.editorContext().viewport().geometryFaceSelection().face();
        viewportRenderer.render(context, width, height, core.editorContext().viewport(), core.editorContext().model(), hoveredAxis, hoveredFace, selectedFace);
        super.render(context, mouseX, mouseY, delta);
    }

    @Override public boolean shouldPause() { return false; }
}
