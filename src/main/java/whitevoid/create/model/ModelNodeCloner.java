package whitevoid.create.model;

public final class ModelNodeCloner {
    private ModelNodeCloner() {}

    public static ModelNode deepCopy(ModelNode source) {
        ModelNode copy = new ModelNode(source.name());
        copy.setGeometry(source.geometry());
        if (source.meshGeometry() != null) {
            copy.setMeshGeometry(source.meshGeometry().copy());
        }

        copy.transform().position(
                source.transform().x(), source.transform().y(), source.transform().z());
        copy.transform().rotation(
                source.transform().rotationX(), source.transform().rotationY(), source.transform().rotationZ());
        copy.transform().scale(
                source.transform().scaleX(), source.transform().scaleY(), source.transform().scaleZ());

        for (ModelNode child : source.children()) {
            copy.addChild(deepCopy(child));
        }
        return copy;
    }
}
