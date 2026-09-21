package whitevoid.create.ui;

import whitevoid.create.editor.geometry.MeshOperations;
import whitevoid.create.model.ModelNode;
import whitevoid.create.model.MeshGeometry;
import whitevoid.create.editor.viewport.ViewportContext;

final class CreateVertexDragController {
    boolean update(
            CreateViewportInteractionState interaction,
            ModelNode node,
            ViewportContext viewport,
            double deltaX,
            double deltaY
    ) {
        if (!interaction.vertexDragging || node == null || interaction.activeVertex < 0) return false;

        MeshGeometry mesh = node.ensureMeshGeometry();
        if (mesh == null || interaction.activeVertex >= mesh.vertices().size()) return true;

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

        node.setMeshGeometry(MeshOperations.moveVertex(
                mesh, interaction.activeVertex, dx, dy, dz
        ));
        return true;
    }
}
