package whitevoid.create.core.history.commands;

import java.util.UUID;
import whitevoid.create.core.history.Command;
import whitevoid.create.core.history.SelectionHistoryCommand;
import whitevoid.create.model.CubeGeometry;
import whitevoid.create.model.Model;
import whitevoid.create.model.ModelNode;

public final class AddCubeCommand implements Command, SelectionHistoryCommand {
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
        if (created.parent() != null && created.parent() != parent) {
            throw new IllegalStateException("Created cube was reparented outside this command");
        }
        if (created.parent() == parent) {
            throw new IllegalStateException("Created cube is already attached");
        }
        parent.addChild(Math.min(index, parent.children().size()), created);
    }

    @Override public void undo() {
        if (created == null || created.parent() != parent) {
            throw new IllegalStateException("Created cube is not attached to its expected parent");
        }
        parent.removeChild(created);
    }

    @Override public String name() { return "Add Cube"; }

    @Override public UUID selectionAfterUndo() { return null; }

    @Override public UUID selectionAfterRedo() { return created.id(); }

    public ModelNode createdNode() { return created; }
}
