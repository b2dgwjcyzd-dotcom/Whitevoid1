package whitevoid.create.editor.transform;

import whitevoid.create.model.ModelNode;
import whitevoid.create.model.Transform;

public final class TransformController {
    private TransformMode mode = TransformMode.SELECT;

    public TransformMode mode() { return mode; }

    public void setMode(TransformMode mode) {
        this.mode = mode;
    }

    public void move(ModelNode node, double x, double y, double z) {
        Transform t = node.transform();
        t.position(x, y, z);
    }

    public void rotate(ModelNode node, double x, double y, double z) {
        Transform t = node.transform();
        t.rotation(x, y, z);
    }

    public void scale(ModelNode node, double x, double y, double z) {
        Transform t = node.transform();
        t.scale(x, y, z);
    }
}
