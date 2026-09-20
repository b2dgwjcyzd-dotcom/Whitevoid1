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
    private MeshGeometry meshGeometry;

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

    public MeshGeometry meshGeometry() { return meshGeometry; }

    /**
     * Returns the editable mesh, creating a mesh representation from the
     * legacy cube geometry on first access.
     */
    public MeshGeometry ensureMeshGeometry() {
        if (meshGeometry == null && geometry != null) {
            meshGeometry = MeshGeometry.fromCube(geometry);
        }
        return meshGeometry;
    }

    public void setMeshGeometry(MeshGeometry meshGeometry) {
        this.meshGeometry = meshGeometry;
    }

    public void setGeometry(CubeGeometry geometry) {
        this.geometry = geometry;
        this.meshGeometry = geometry == null ? null : MeshGeometry.fromCube(geometry);
    }

    public void addChild(ModelNode child) {
        addChild(children.size(), child);
    }

    public void addChild(int index, ModelNode child) {
        if (child == null) {
            throw new NullPointerException("child");
        }
        if (child == this) {
            throw new IllegalArgumentException("A node cannot parent itself");
        }
        if (index < 0 || index > children.size()) {
            throw new IndexOutOfBoundsException("index=" + index);
        }

        if (child.parent != null) {
            child.parent.children.remove(child);
        }

        child.parent = this;
        children.add(index, child);
    }

    public int indexOfChild(ModelNode child) {
        return children.indexOf(child);
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
