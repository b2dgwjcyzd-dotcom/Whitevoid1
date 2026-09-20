package whitevoid.create.core.history.commands;

import whitevoid.create.core.history.Command;
import whitevoid.create.model.CubeGeometry;
import whitevoid.create.model.ModelNode;

public final class SetCubeGeometryCommand implements Command {
    private final ModelNode node;
    private final CubeGeometry oldGeometry;
    private final CubeGeometry newGeometry;

    public SetCubeGeometryCommand(ModelNode node, CubeGeometry newGeometry) {
        this.node = node;
        if (node.geometry() == null) throw new IllegalArgumentException("Node has no cube geometry");
        this.oldGeometry = node.geometry();
        this.newGeometry = newGeometry;
    }

    public SetCubeGeometryCommand(ModelNode node, CubeGeometry oldGeometry, CubeGeometry newGeometry) {
        this.node = node;
        this.oldGeometry = oldGeometry;
        this.newGeometry = newGeometry;
    }

    @Override public void execute() { node.setGeometry(newGeometry); }
    @Override public void undo() { node.setGeometry(oldGeometry); }
    @Override public String name() { return "Set Cube Geometry"; }
}
