package whitevoid.create.core.history.commands;

import java.util.Objects;
import whitevoid.create.core.history.Command;
import whitevoid.create.model.CubeGeometry;
import whitevoid.create.model.MeshGeometry;
import whitevoid.create.model.ModelNode;

public final class SetCubeGeometryCommand implements Command {
    private final ModelNode node;
    private final CubeGeometry oldGeometry;
    private final CubeGeometry newGeometry;
    private final MeshGeometry oldMesh;

    public SetCubeGeometryCommand(ModelNode node, CubeGeometry newGeometry) {
        this(node, Objects.requireNonNull(node, "node").geometry(), newGeometry);
        if (node.meshGeometry() != null && !node.meshGeometry().matchesCube(oldGeometry)) {
            throw new IllegalArgumentException("Cannot resize a node with custom mesh geometry");
        }
    }

    public SetCubeGeometryCommand(ModelNode node, CubeGeometry oldGeometry, CubeGeometry newGeometry) {
        this.node = Objects.requireNonNull(node, "node");
        this.oldGeometry = Objects.requireNonNull(oldGeometry, "oldGeometry");
        this.newGeometry = Objects.requireNonNull(newGeometry, "newGeometry");
        this.oldMesh = node.meshGeometry() == null ? null : node.meshGeometry().copy();
    }

    @Override public void execute() {
        node.setGeometry(newGeometry);
    }

    @Override public void undo() {
        node.setGeometry(oldGeometry);
        if (oldMesh != null) node.setMeshGeometry(oldMesh.copy());
    }

    @Override public String name() { return "Set Cube Geometry"; }
}
