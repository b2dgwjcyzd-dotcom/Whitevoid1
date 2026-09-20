package whitevoid.create.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public final class Model {
    private final ModelNode root = new ModelNode("Root");

    public static Model withDefaultCube() {
        Model model = new Model();
        ModelNode cube = model.addNode("Cube");
        cube.setGeometry(new CubeGeometry(2.0, 2.0, 2.0));
        return model;
    }

    public ModelNode root() {
        return root;
    }

    public List<ModelNode> allNodes() {
        List<ModelNode> nodes = new ArrayList<>();
        root.collect(nodes);
        return Collections.unmodifiableList(nodes);
    }

    public ModelNode addNode(String name) {
        ModelNode node = new ModelNode(Objects.requireNonNull(name, "name"));
        root.addChild(node);
        return node;
    }
}
