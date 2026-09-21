package whitevoid.create.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public final class ModelNode {
    private final UUID id;
    private String name;
    private ModelNode parent;
    private final List<ModelNode> children = new ArrayList<>();
    private final Transform transform = new Transform();
    private CubeGeometry geometry;
    private MeshGeometry meshGeometry;
    private MeshMaterialAssignment materialAssignment;

    public ModelNode(String name) {
        this(name, UUID.randomUUID());
    }

    public ModelNode(String name, UUID id) {
        this.name = Objects.requireNonNull(name, "name");
        this.id = Objects.requireNonNull(id, "id");
    }

    public UUID id() { return id; }
    public String name() { return name; }
    public void setName(String name) { this.name = name; }
    public ModelNode parent() { return parent; }
    public List<ModelNode> children() { return Collections.unmodifiableList(children); }
    public Transform transform() { return transform; }
    public CubeGeometry geometry() { return geometry; }

    public MeshGeometry meshGeometry() { return meshGeometry; }

    public MeshMaterialAssignment materialAssignment() { return materialAssignment; }

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
        if (meshGeometry == null) {
            this.meshGeometry = null;
            this.materialAssignment = null;
            return;
        }
        if (materialAssignment != null
                && materialAssignment.faceMaterials().size() != meshGeometry.faces().size()) {
            this.materialAssignment = null;
        }
        this.meshGeometry = meshGeometry;
    }

    public void setMaterialAssignment(MeshMaterialAssignment materialAssignment) {
        if (materialAssignment != null
                && meshGeometry != null
                && materialAssignment.faceMaterials().size() != meshGeometry.faces().size()) {
            throw new IllegalArgumentException("Material assignment must match mesh face count");
        }
        this.materialAssignment = materialAssignment == null ? null : materialAssignment.copy();
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
        for (ModelNode cursor = this; cursor != null; cursor = cursor.parent) {
            if (cursor == child) {
                throw new IllegalArgumentException("Cannot add a node to its own subtree");
            }
        }
        if (index < 0 || index > children.size()) {
            throw new IndexOutOfBoundsException("index=" + index);
        }

        ModelNode oldParent = child.parent;
        if (oldParent == this) {
            int oldIndex = children.indexOf(child);
            if (oldIndex < 0) {
                throw new IllegalStateException("Child parent link is inconsistent");
            }
            children.remove(oldIndex);
            if (oldIndex < index) {
                index--;
            }
        } else if (oldParent != null) {
            if (!oldParent.children.remove(child)) {
                throw new IllegalStateException("Child parent link is inconsistent");
            }
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
