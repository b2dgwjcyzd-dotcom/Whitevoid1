package whitevoid.create.core.history.commands;

import whitevoid.create.core.history.Command;
import whitevoid.create.model.CubeGeometry;
import whitevoid.create.model.Model;
import whitevoid.create.model.ModelNode;

public final class AddCubeCommand implements Command {
    private final Model model;
    private final ModelNode parent;
    private final String name;
    private final CubeGeometry geometry;
    private final double x, y, z;
    private final int index;
    private ModelNode created;

    public AddCubeCommand(Model model, ModelNode parent, String name,
                           CubeGeometry geometry, double x, double y, double z) {
        this.model = model;
        this.parent = parent;
        this.name = name;
        this.geometry = geometry;
        this.x = x;
        this.y = y;
        this.z = z;
        this.index = parent.children().size();
    }

    @Override public void execute() {
        if (created == null) {
            created = new ModelNode(name);
            created.setGeometry(geometry);
            created.transform().position(x, y, z);
        }
        parent.addChild(Math.min(index, parent.children().size()), created);
    }

    @Override public void undo() {
        parent.removeChild(created);
    }

    @Override public String name() { return "Add Cube"; }

    public ModelNode createdNode() { return created; }
}
