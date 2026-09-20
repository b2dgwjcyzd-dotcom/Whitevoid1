package whitevoid.create.ui;

import whitevoid.create.core.CreateCore;
import whitevoid.create.core.history.commands.SetMeshGeometryCommand;
import whitevoid.create.model.MeshGeometry;
import whitevoid.create.model.ModelNode;

public final class MeshComponentDragController {
    public enum Type { NONE, VERTEX, EDGE }

    private final ViewportGizmo gizmo;
    private Type type = Type.NONE;
    private MeshGeometry oldMesh;
    private int vertex = -1;
    private int edgeA = -1;
    private int edgeB = -1;

    public MeshComponentDragController(ViewportGizmo gizmo) {
        this.gizmo = gizmo;
    }

    public boolean dragging() { return type != Type.NONE; }
    public Type type() { return type; }

    public void beginVertex(ModelNode node) {
        if (node == null) { cancel(); return; }
        MeshGeometry mesh = node.ensureMeshGeometry();
        if (mesh == null) { cancel(); return; }
        type = Type.VERTEX;
        vertex = -1;
        edgeA = edgeB = -1;
        oldMesh = mesh.copy();
    }

    public void beginEdge(ModelNode node, int a, int b) {
        if (node == null) { cancel(); return; }
        MeshGeometry mesh = node.ensureMeshGeometry();
        if (mesh == null) { cancel(); return; }
        type = Type.EDGE;
        vertex = -1;
        edgeA = a;
        edgeB = b;
        oldMesh = mesh.copy();
    }

    public void finish(CreateCore core, ModelNode node) {
        if (!dragging()) return;
        if (node != null && oldMesh != null) {
            MeshGeometry current = node.ensureMeshGeometry();
            if (current != null && !oldMesh.equals(current)) {
                core.editorContext().history().recordExecuted(
                        new SetMeshGeometryCommand(node, oldMesh, current.copy()));
            }
        }
        cancel();
    }

    public void cancel() {
        type = Type.NONE;
        oldMesh = null;
        vertex = -1;
        edgeA = edgeB = -1;
    }

    public Type beginType() { return type; }
    public int vertex() { return vertex; }
    public int edgeA() { return edgeA; }
    public int edgeB() { return edgeB; }
}
