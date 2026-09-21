package whitevoid.create.model;

import java.util.Objects;

public final class ModelNodeCloner {
    private ModelNodeCloner() {}

    /**
     * Creates a fully independent copy of a node subtree.
     * Runtime identity (UUID and parent) is never shared with the source.
     */
    public static ModelNode deepCopy(ModelNode source) {
        Objects.requireNonNull(source, "source");

        ModelNode copy = new ModelNode(source.name());
        copy.setGeometry(source.geometry());

        // setGeometry creates a fresh canonical mesh; replace it with an
        // independent copy when the source carries custom editable topology.
        if (source.meshGeometry() != null) {
            copy.setMeshGeometry(source.meshGeometry().copy());
        }
        if (source.materialAssignment() != null) {
            copy.setMaterialAssignment(source.materialAssignment().copy());
        }

        copy.transform().position(
                source.transform().x(),
                source.transform().y(),
                source.transform().z());
        copy.transform().rotation(
                source.transform().rotationX(),
                source.transform().rotationY(),
                source.transform().rotationZ());
        copy.transform().scale(
                source.transform().scaleX(),
                source.transform().scaleY(),
                source.transform().scaleZ());

        for (ModelNode child : source.children()) {
            copy.addChild(deepCopy(child));
        }

        return copy;
    }
}
