package whitevoid.create.ui;

import java.util.UUID;
import org.lwjgl.glfw.GLFW;
import whitevoid.create.core.CreateCore;
import whitevoid.create.core.history.Command;
import whitevoid.create.core.history.SelectionHistoryCommand;
import whitevoid.create.core.history.commands.SetTransformCommand;
import whitevoid.create.editor.geometry.MeshSelectionMode;
import whitevoid.create.editor.transform.TransformMode;
import whitevoid.create.editor.viewport.ViewportContext;
import whitevoid.create.editor.selection.SelectionMode;
import whitevoid.create.model.ModelNode;

public final class CreateTransformHotkeyController {
    private final CreateCore core;
    private final CreateMeshModelingController meshModeling;

    public CreateTransformHotkeyController(CreateCore core, CreateMeshModelingController meshModeling) {
        this.core = core;
        this.meshModeling = meshModeling;
    }

    public boolean handle(CreateViewportInteractionState interaction, ViewportContext viewport,
                          ModelNode node, int keyCode, boolean shiftDown, boolean controlDown) {
        if (controlDown && keyCode == GLFW.GLFW_KEY_Z) {
            if (shiftDown) redoWithSelection(viewport);
            else undoWithSelection(viewport);
            return true;
        }
        if (controlDown && keyCode == GLFW.GLFW_KEY_Y) {
            redoWithSelection(viewport);
            return true;
        }

        if (node == null) return false;

        if (viewport.transform().mode() == TransformMode.GEOMETRY
                && viewport.meshComponentSelection().matches(node)
                && (viewport.meshComponentSelection().mode() == MeshSelectionMode.VERTEX
                    || viewport.meshComponentSelection().mode() == MeshSelectionMode.EDGE)) {
            double step = shiftDown ? 0.1 : 0.25;
            if (keyCode == GLFW.GLFW_KEY_LEFT) return moveComponents(node, -step, 0, 0);
            if (keyCode == GLFW.GLFW_KEY_RIGHT) return moveComponents(node, step, 0, 0);
            if (keyCode == GLFW.GLFW_KEY_DOWN) return moveComponents(node, 0, 0, step);
            if (keyCode == GLFW.GLFW_KEY_UP) return moveComponents(node, 0, 0, -step);
            if (keyCode == GLFW.GLFW_KEY_SPACE) return moveComponents(node, 0, step, 0);
        }

        double step = shiftDown ? 0.1 : 1.0;
        if (viewport.transform().mode() == TransformMode.MOVE) {
            if (keyCode == GLFW.GLFW_KEY_LEFT) return transformMove(node, -step, 0, 0);
            if (keyCode == GLFW.GLFW_KEY_RIGHT) return transformMove(node, step, 0, 0);
            if (keyCode == GLFW.GLFW_KEY_DOWN) return transformMove(node, 0, 0, step);
            if (keyCode == GLFW.GLFW_KEY_UP) return transformMove(node, 0, 0, -step);
            if (keyCode == GLFW.GLFW_KEY_SPACE) return transformMove(node, 0, step, 0);
        }

        if (viewport.transform().mode() == TransformMode.ROTATE) {
            if (keyCode == GLFW.GLFW_KEY_LEFT) return transformRotate(node, 0, -5, 0);
            if (keyCode == GLFW.GLFW_KEY_RIGHT) return transformRotate(node, 0, 5, 0);
            if (keyCode == GLFW.GLFW_KEY_DOWN) return transformRotate(node, -5, 0, 0);
            if (keyCode == GLFW.GLFW_KEY_UP) return transformRotate(node, 5, 0, 0);
        }

        if (viewport.transform().mode() == TransformMode.SCALE) {
            if (keyCode == GLFW.GLFW_KEY_LEFT || keyCode == GLFW.GLFW_KEY_DOWN) return transformScale(node, -0.1);
            if (keyCode == GLFW.GLFW_KEY_RIGHT || keyCode == GLFW.GLFW_KEY_UP) return transformScale(node, 0.1);
        }

        return false;
    }

    private void undoWithSelection(ViewportContext viewport) {
        var history = core.editorContext().history();
        if (!history.undo()) return;
        applySelection(viewport, history.lastUndone(), true);
    }

    private void redoWithSelection(ViewportContext viewport) {
        var history = core.editorContext().history();
        if (!history.redo()) return;
        applySelection(viewport, history.lastRedone(), false);
    }

    private void applySelection(ViewportContext viewport, Command command, boolean undo) {
        if (!(command instanceof SelectionHistoryCommand selectionCommand)) return;

        UUID id = undo ? selectionCommand.selectionAfterUndo() : selectionCommand.selectionAfterRedo();
        if (id == null) {
            viewport.selection().clear();
            return;
        }

        var model = core.editorContext().model();
        for (var candidate : model.allNodes()) {
            if (candidate.id().equals(id)) {
                viewport.selection().select(candidate, SelectionMode.SINGLE);
                return;
            }
        }
        viewport.selection().clear();
    }

    private boolean moveComponents(ModelNode node, double dx, double dy, double dz) {
        meshModeling.moveSelectedComponents(dx, dy, dz);
        return true;
    }

    private boolean transformMove(ModelNode node, double dx, double dy, double dz) {
        var t = node.transform();
        core.editorContext().history().execute(new SetTransformCommand(node,
                t.x() + dx, t.y() + dy, t.z() + dz,
                t.rotationX(), t.rotationY(), t.rotationZ(),
                t.scaleX(), t.scaleY(), t.scaleZ()));
        return true;
    }

    private boolean transformRotate(ModelNode node, double dx, double dy, double dz) {
        var t = node.transform();
        core.editorContext().history().execute(new SetTransformCommand(node,
                t.x(), t.y(), t.z(),
                t.rotationX(), t.rotationY(), t.rotationZ(),
                t.scaleX(), t.scaleY(), t.scaleZ()));
        return true;
    }

    private boolean transformScale(ModelNode node, double delta) {
        var t = node.transform();
        double scale = Math.max(0.01, t.scaleX() + delta);
        core.editorContext().history().execute(new SetTransformCommand(node,
                t.x(), t.y(), t.z(),
                t.rotationX(), t.rotationY(), t.rotationZ(),
                t.scaleX(), t.scaleY(), t.scaleZ()));
        return true;
    }
}
