package whitevoid.create.editor.transform;

import whitevoid.create.model.ModelNode;

public final class TransformController {
    private TransformMode mode = TransformMode.SELECT;

    public TransformMode mode() { return mode; }

    public void setMode(TransformMode mode) {
        this.mode = mode;
    }

    public void translate(ModelNode node, double dx, double dy, double dz) {
        node.transform().position(
                node.transform().x() + dx,
                node.transform().y() + dy,
                node.transform().z() + dz
        );
    }

    public void rotateBy(ModelNode node, double dx, double dy, double dz) {
        node.transform().rotation(
                node.transform().rotationX() + dx,
                node.transform().rotationY() + dy,
                node.transform().rotationZ() + dz
        );
    }

    public void scaleBy(ModelNode node, double dx, double dy, double dz) {
        node.transform().scale(
                Math.max(0.01, node.transform().scaleX() + dx),
                Math.max(0.01, node.transform().scaleY() + dy),
                Math.max(0.01, node.transform().scaleZ() + dz)
        );
    }
}
