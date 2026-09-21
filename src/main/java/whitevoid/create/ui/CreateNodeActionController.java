package whitevoid.create.ui;

import whitevoid.create.core.CreateCore;
import whitevoid.create.core.history.commands.AddCubeCommand;
import whitevoid.create.core.history.commands.DeleteNodeCommand;
import whitevoid.create.core.history.commands.DuplicateNodeCommand;
import whitevoid.create.core.history.commands.SetCubeGeometryCommand;
import whitevoid.create.editor.selection.SelectionMode;
import whitevoid.create.editor.transform.TransformMode;
import whitevoid.create.model.CubeGeometry;

public final class CreateNodeActionController {
    private final CreateCore core;

    public CreateNodeActionController(CreateCore core) {
        this.core = core;
    }

    public void addCube() {
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
                model, model.root(), "Cube",
                new CubeGeometry(2.0, 2.0, 2.0),
                x, y, z
        );
        core.editorContext().history().execute(command);
        core.editorContext().viewport().selection().select(command.createdNode(), SelectionMode.SINGLE);
        core.editorContext().viewport().transform().setMode(TransformMode.SELECT);
    }

    public void deleteSelectedNode() {
        var model = core.editorContext().model();
        var node = core.editorContext().viewport().selection().first(model);
        if (node == null || node == model.root()) return;

        core.editorContext().history().execute(new DeleteNodeCommand(model, node));
        core.editorContext().viewport().selection().clear();
        core.editorContext().viewport().transform().setMode(TransformMode.SELECT);
    }

    public void duplicateSelectedNode() {
        var model = core.editorContext().model();
        var node = core.editorContext().viewport().selection().first(model);
        if (node == null || node == model.root()) return;

        var command = new DuplicateNodeCommand(model, node);
        core.editorContext().history().execute(command);
        core.editorContext().viewport().selection().select(command.duplicatedNode(), SelectionMode.SINGLE);
    }

    public void resizeSelectedCube(boolean grow, boolean fineStep) {
        var node = core.editorContext().viewport().selection().first(core.editorContext().model());
        if (node == null || node.geometry() == null) return;

        var g = node.geometry();
        double step = fineStep ? 0.25 : 1.0;
        double factor = grow ? step : -step;

        double width = Math.max(0.1, g.width() + factor);
        double height = Math.max(0.1, g.height() + factor);
        double depth = Math.max(0.1, g.depth() + factor);

        core.editorContext().history().execute(
                new SetCubeGeometryCommand(node, new CubeGeometry(width, height, depth))
        );
    }
}
