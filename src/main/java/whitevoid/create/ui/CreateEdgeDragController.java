package whitevoid.create.ui;

import whitevoid.create.editor.geometry.MeshOperations;
import whitevoid.create.editor.viewport.ViewportContext;
import whitevoid.create.model.MeshGeometry;
import whitevoid.create.model.ModelNode;

final class CreateEdgeDragController {
    boolean update(
            CreateViewportInteractionState interaction,
            ModelNode node,
            ViewportContext viewport,
            double deltaX,
            double deltaY
    ) {
        if (!interaction.edgeDragging || node == null
                || interaction.activeEdgeA < 0 || interaction.activeEdgeB < 0) {
            return false;
        }

        MeshGeometry mesh = node.ensureMeshGeometry();
        if (mesh == null
                || interaction.activeEdgeA >= mesh.vertices().size()
                || interaction.activeEdgeB >= mesh.vertices().size()) {
            return true;
        }

        var camera = viewport.viewport().camera();
        double yaw = Math.toRadians(camera.yaw());
        double pitch = Math.toRadians(camera.pitch());
        double cy = Math.cos(yaw);
        double sy = Math.sin(yaw);
        double cp = Math.cos(pitch);
        double sp = Math.sin(pitch);
        double worldPerPixel = Math.max(0.0005, camera.distance() / 300.0);

        double rightX = cy;
        double rightZ = -sy;
        double upX = -sy * sp;
        double upY = cp;
        double upZ = -cy * sp;

        double dx = (deltaX * rightX - deltaY * upX) * worldPerPixel;
        double dy = (-deltaY * upY) * worldPerPixel;
        double dz = (deltaX * rightZ - deltaY * upZ) * worldPerPixel;

        MeshGeometry moved = MeshOperations.moveVertex(
                mesh, interaction.activeEdgeA, dx, dy, dz);
        moved = MeshOperations.moveVertex(
                moved, interaction.activeEdgeB, dx, dy, dz);
        node.setMeshGeometry(moved);
        return true;
    }
}
