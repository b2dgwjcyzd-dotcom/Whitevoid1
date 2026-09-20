package whitevoid.create.core.history.commands;

import java.util.Objects;
import whitevoid.create.core.history.Command;
import whitevoid.create.model.MeshGeometry;
import whitevoid.create.model.ModelNode;

public final class SetMeshGeometryCommand implements Command {
    private final ModelNode node;
    private final MeshGeometry oldMesh;
    private final MeshGeometry newMesh;

    public SetMeshGeometryCommand(ModelNode node, MeshGeometry oldMesh, MeshGeometry newMesh) {
        this.node = Objects.requireNonNull(node, "node");
        this.oldMesh = oldMesh;
        this.newMesh = Objects.requireNonNull(newMesh, "newMesh");
    }

    @Override public void execute() {
        node.setMeshGeometry(newMesh);
    }

    @Override public void undo() {
        node.setMeshGeometry(oldMesh);
    }

    @Override public String name() {
        return "Edit Mesh";
    }
}
