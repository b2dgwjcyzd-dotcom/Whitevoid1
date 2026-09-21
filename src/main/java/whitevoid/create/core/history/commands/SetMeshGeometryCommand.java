package whitevoid.create.core.history.commands;

import java.util.Objects;
import whitevoid.create.core.history.Command;
import whitevoid.create.model.MeshGeometry;
import whitevoid.create.model.MeshMaterialAssignment;
import whitevoid.create.model.ModelNode;

public final class SetMeshGeometryCommand implements Command {
    private final ModelNode node;
    private final MeshGeometry oldMesh;
    private final MeshGeometry newMesh;
    private final MeshMaterialAssignment oldMaterials;
    private final MeshMaterialAssignment newMaterials;

    public SetMeshGeometryCommand(ModelNode node, MeshGeometry oldMesh, MeshGeometry newMesh) {
        this(node, oldMesh, newMesh,
                node.materialAssignment() != null
                        && node.materialAssignment().faceMaterials().size() == newMesh.faces().size()
                        ? node.materialAssignment()
                        : null);
    }

    public SetMeshGeometryCommand(ModelNode node, MeshGeometry oldMesh, MeshGeometry newMesh,
                                  MeshMaterialAssignment newMaterials) {
        this.node = Objects.requireNonNull(node, "node");
        this.oldMesh = Objects.requireNonNull(oldMesh, "oldMesh").copy();
        this.newMesh = Objects.requireNonNull(newMesh, "newMesh").copy();
        this.oldMaterials = node.materialAssignment();
        if (newMaterials != null && newMaterials.faceMaterials().size() != this.newMesh.faces().size()) {
            throw new IllegalArgumentException("New material assignment must match new mesh face count");
        }
        this.newMaterials = newMaterials == null ? null : newMaterials.copy();
    }

    @Override public void execute() {
        node.setMeshGeometry(newMesh);
        node.setMaterialAssignment(newMaterials);
    }

    @Override public void undo() {
        node.setMeshGeometry(oldMesh);
        node.setMaterialAssignment(oldMaterials);
    }

    @Override public String name() {
        return "Edit Mesh";
    }
}