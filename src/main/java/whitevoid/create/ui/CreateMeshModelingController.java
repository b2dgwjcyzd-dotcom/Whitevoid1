package whitevoid.create.ui;

import whitevoid.create.core.CreateCore;
import whitevoid.create.core.history.commands.SetMeshGeometryCommand;
import whitevoid.create.editor.geometry.MeshOperations;
import whitevoid.create.editor.geometry.MeshMaterialOperations;
import whitevoid.create.editor.geometry.MeshComponentTransforms;
import whitevoid.create.editor.geometry.MeshSelectionMode;
import whitevoid.create.editor.geometry.MeshTopologySelection;
import whitevoid.create.editor.geometry.MeshComponentSelection;
import whitevoid.create.model.MeshGeometry;
import whitevoid.create.model.ModelNode;

import java.util.LinkedHashSet;

public final class CreateMeshModelingController {
    private final CreateCore core;
    private final ComponentTransformGizmo componentGizmo;
    private final ComponentTransformController componentTransform;

    public CreateMeshModelingController(
            CreateCore core,
            ComponentTransformGizmo componentGizmo,
            ComponentTransformController componentTransform
    ) {
        this.core = core;
        this.componentGizmo = componentGizmo;
        this.componentTransform = componentTransform;
    }

    public void mirrorSelectedComponents(int axis) {
        var viewport = core.editorContext().viewport();
        var node = viewport.meshComponentSelection().node(core.editorContext().model());
        if (node == null) return;
        var oldMesh = node.ensureMeshGeometry();
        if (oldMesh == null) return;

        var selection = viewport.meshComponentSelection();
        var ids = MeshComponentTransforms.affectedVertices(oldMesh, selection.mode(),
                selection.vertexIndices(), selection.edgeIndices(), selection.faceIndices());
        if (ids.isEmpty()) return;

        var pivot = componentGizmo.localPivot(node, selection.mode(),
                selection.vertexIndices(), selection.edgeIndices(), selection.faceIndices(), componentTransform.pivotMode());
        var newMesh = MeshComponentTransforms.mirror(oldMesh, ids, pivot, axis);
        if (newMesh.equals(oldMesh)) return;

        core.editorContext().history().execute(
                new SetMeshGeometryCommand(node, oldMesh.copy(), newMesh));
    }

    public void moveSelectedComponents(double dx, double dy, double dz) {
        var viewport = core.editorContext().viewport();
        var node = viewport.meshComponentSelection().node(core.editorContext().model());
        if (node == null) return;
        var oldMesh = node.ensureMeshGeometry();
        if (oldMesh == null) return;

        var selection = viewport.meshComponentSelection();
        LinkedHashSet<Integer> indices = new LinkedHashSet<>(
                MeshComponentTransforms.affectedVertices(
                        oldMesh,
                        selection.mode(),
                        selection.vertexIndices(),
                        selection.edgeIndices(),
                        selection.faceIndices()));
        if (indices.isEmpty()) return;

        var newMesh = oldMesh.copy();
        for (int index : indices) {
            newMesh = MeshOperations.moveVertex(newMesh, index, dx, dy, dz);
        }
        core.editorContext().history().execute(
                new SetMeshGeometryCommand(node, oldMesh.copy(), newMesh));
    }

    public void moveSelectedVertex(double dx, double dy, double dz) {
        var viewport = core.editorContext().viewport();
        var node = viewport.meshComponentSelection().node(core.editorContext().model());
        if (node == null || viewport.meshComponentSelection().mode() != MeshSelectionMode.VERTEX) return;
        int index = viewport.meshComponentSelection().indexA();
        var oldMesh = node.ensureMeshGeometry();
        if (oldMesh == null) return;
        var newMesh = MeshOperations.moveVertex(oldMesh, index, dx, dy, dz);
        core.editorContext().history().execute(
                new SetMeshGeometryCommand(node, oldMesh.copy(), newMesh));
        viewport.meshComponentSelection().selectVertex(node, index);
    }

    public void extrudeSelectedEdge(double amount) {
        var viewport = core.editorContext().viewport();
        var model = core.editorContext().model();
        MeshComponentSelection selection = viewport.meshComponentSelection();
        var node = selection.node(model);
        if (node == null || selection.size() == 0) return;

        var oldMesh = node.ensureMeshGeometry();
        if (oldMesh == null) return;

        if (selection.size() > 1) {
            var selected = new LinkedHashSet<Long>();
            for (int[] edge : selection.edgeIndices()) {
                selected.add(MeshTopologySelection.edgeKey(edge[0], edge[1]));
            }

            MeshOperations.OperationResult result =
                    MeshOperations.extrudeEdgesResult(oldMesh, selected, amount);
            core.editorContext().history().execute(
                    new SetMeshGeometryCommand(node, oldMesh.copy(), result.mesh(),
                        MeshMaterialOperations.apply(node.materialAssignment(), result)));
            selection.applySelectionHint(node, result);
            return;
        }

        int a = selection.indexA();
        int b = selection.indexB();
        if (a < 0 || b < 0) return;

        var newMesh = MeshOperations.extrudeEdge(oldMesh, a, b, amount);
        core.editorContext().history().execute(
                new SetMeshGeometryCommand(node, oldMesh.copy(), newMesh));
        selection.selectEdge(node, newMesh.vertices().size() - 2, newMesh.vertices().size() - 1);
    }

    public void bevelSelectedEdge(double amount) {
        var viewport = core.editorContext().viewport();
        var model = core.editorContext().model();
        MeshComponentSelection selection = viewport.meshComponentSelection();
        var node = selection.node(model);
        if (node == null || selection.size() == 0) return;

        var oldMesh = node.ensureMeshGeometry();
        if (oldMesh == null) return;

        try {
            if (selection.size() > 1) {
                var selected = new LinkedHashSet<Long>();
                for (int[] edge : selection.edgeIndices()) {
                    selected.add(MeshTopologySelection.edgeKey(edge[0], edge[1]));
                }

                MeshOperations.OperationResult result =
                        MeshOperations.bevelEdgesResult(oldMesh, selected, amount);
                if (result.createdFaces().isEmpty()) return;

                core.editorContext().history().execute(
                        new SetMeshGeometryCommand(node, oldMesh.copy(), result.mesh()));
                selection.applySelectionHint(node, result);
            } else {
                int a = selection.indexA();
                int b = selection.indexB();
                if (a < 0 || b < 0) return;

                var newMesh = MeshOperations.bevelEdge(oldMesh, a, b, amount);
                core.editorContext().history().execute(
                        new SetMeshGeometryCommand(node, oldMesh.copy(), newMesh));
                selection.clear();
            }
        } catch (IllegalArgumentException ignored) {
            // Bevel requires manifold selected edges with exactly two adjacent faces.
        }
    }

    public void insetSelectedFaces(double amount) {
        var viewport = core.editorContext().viewport();
        var model = core.editorContext().model();
        MeshComponentSelection selection = viewport.meshComponentSelection();
        var node = selection.node(model);
        if (node == null || selection.faceIndices().isEmpty()) return;

        var selected = new LinkedHashSet<>(selection.faceIndices());
        var oldMesh = node.ensureMeshGeometry();
        if (oldMesh == null) return;

        MeshOperations.OperationResult result =
                MeshOperations.insetFacesResult(oldMesh, selected, amount);
        if (result.createdFaces().isEmpty()) return;

        core.editorContext().history().execute(
                new SetMeshGeometryCommand(node, oldMesh.copy(), result.mesh()));
        selection.applySelectionHint(node, result);
        viewport.geometryFaceSelection().clear();
    }

    public void extrudeSelectedFaces(double amount) {
        var viewport = core.editorContext().viewport();
        var model = core.editorContext().model();
        MeshComponentSelection selection = viewport.meshComponentSelection();
        var node = selection.node(model);
        if (node == null || selection.faceIndices().isEmpty()) return;

        var selected = new LinkedHashSet<>(selection.faceIndices());
        var oldMesh = node.ensureMeshGeometry();
        if (oldMesh == null) return;

        MeshOperations.OperationResult result =
                MeshOperations.extrudeFacesResult(oldMesh, selected, amount);
        if (result.createdFaces().isEmpty()) return;

        core.editorContext().history().execute(
                new SetMeshGeometryCommand(node, oldMesh.copy(), result.mesh()));
        selection.applySelectionHint(node, result);
        viewport.geometryFaceSelection().clear();
    }

    public void insetSelectedFace(double amount) {
        applySingleFaceOperation(amount, true);
    }

    public void extrudeSelectedFace(double amount) {
        applySingleFaceOperation(amount, false);
    }

    private void applySingleFaceOperation(double amount, boolean inset) {
        var viewport = core.editorContext().viewport();
        var model = core.editorContext().model();
        MeshComponentSelection selection = viewport.meshComponentSelection();
        var node = selection.node(model);
        int faceIndex = selection.activeFace();
        if (node == null || faceIndex < 0 || !selection.containsFace(faceIndex)) return;

        var oldMesh = node.ensureMeshGeometry();
        if (oldMesh == null || faceIndex >= oldMesh.faces().size()) return;

        MeshOperations.OperationResult result = inset
                ? MeshOperations.insetFaceResult(oldMesh, faceIndex, amount)
                : MeshOperations.extrudeFaceResult(oldMesh, faceIndex, amount);
        if (result.createdFaces().isEmpty()) return;

        core.editorContext().history().execute(
                new SetMeshGeometryCommand(node, oldMesh.copy(), result.mesh()));
        selection.applySelectionHint(node, result);
        viewport.geometryFaceSelection().clear();
    }
}
