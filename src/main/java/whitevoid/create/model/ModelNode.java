package whitevoid.create.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public final class ModelNode {
    private final UUID id = UUID.randomUUID();
    private String name;
    private ModelNode parent;
    private final List<ModelNode> children = new ArrayList<>();
    private final Transform transform = new Transform();
    private CubeGeometry geometry;

    public ModelNode(String name) {
        this.name = name;
    }

    public UUID id() { return id; }
    public String name() { return name; }
    public void setName(String name) { this.name = name; }
    public ModelNode parent() { return parent; }
    public List<ModelNode> children() { return Collections.unmodifiableList(children); }
    public Transform transform() { return transform; }
    public CubeGeometry geometry() { return geometry; }

    public void setGeometry(CubeGeometry geometry) {
        this.geometry = geometry;
    }

    public void addChild(ModelNode child) {
        if (child == this) {
            throw new IllegalArgumentException("A node cannot parent itself");
        }
        if (child.parent != null) {
            child.parent.children.remove(child);
        }
        child.parent = this;
        children.add(child);
    }

    public void removeChild(ModelNode child) {
        if (children.remove(child)) {
            child.parent = null;
        }
    }

    void collect(List<ModelNode> target) {
        target.add(this);
        for (ModelNode child : children) {
            child.collect(target);
        }
    }
}
