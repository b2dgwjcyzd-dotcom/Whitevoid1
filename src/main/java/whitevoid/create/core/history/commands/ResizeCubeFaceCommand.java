package whitevoid.create.core.history.commands;

import whitevoid.create.core.history.Command;
import whitevoid.create.model.CubeGeometry;
import whitevoid.create.model.ModelNode;

public final class ResizeCubeFaceCommand implements Command {
    private final ModelNode node;
    private final CubeGeometry oldGeometry;
    private final CubeGeometry newGeometry;
    private final double oldX, oldY, oldZ;
    private final double newX, newY, newZ;

    public ResizeCubeFaceCommand(ModelNode node,
                                 CubeGeometry oldGeometry, CubeGeometry newGeometry,
                                 double oldX, double oldY, double oldZ,
                                 double newX, double newY, double newZ) {
        this.node = node;
        this.oldGeometry = oldGeometry;
        this.newGeometry = newGeometry;
        this.oldX = oldX;
        this.oldY = oldY;
        this.oldZ = oldZ;
        this.newX = newX;
        this.newY = newY;
        this.newZ = newZ;
    }

    @Override public void execute() {
        node.setGeometry(newGeometry);
        node.transform().position(newX, newY, newZ);
    }

    @Override public void undo() {
        node.setGeometry(oldGeometry);
        node.transform().position(oldX, oldY, oldZ);
    }

    @Override public String name() {
        return "Resize Cube Face";
    }
}
