package whitevoid.create.project;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import whitevoid.create.model.CubeGeometry;
import whitevoid.create.model.MeshGeometry;
import whitevoid.create.model.Model;
import whitevoid.create.model.ModelNode;
import whitevoid.create.model.Transform;

/**
 * Serializes the editable CREATE model without exposing persistence concerns
 * to the editor and model classes.
 */
public final class ModelSerializer {
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public String serialize(Model model) {
        if (model == null) throw new IllegalArgumentException("Model cannot be null");
        return gson.toJson(toData(model.root()));
    }

    public Model deserialize(String json) {
        if (json == null || json.isBlank()) {
            throw new IllegalArgumentException("Model JSON cannot be blank");
        }

        NodeData root = gson.fromJson(json, NodeData.class);
        if (root == null || root.id == null) {
            throw new IllegalArgumentException("Invalid CREATE model");
        }

        Model model = new Model(root.id);
        restoreNodeData(model.root(), root);
        return model;
        return model;
    }

    private static NodeData toData(ModelNode node) {
        NodeData data = new NodeData();
        data.id = node.id();
        data.name = node.name();

        Transform transform = node.transform();
        data.transform = new TransformData(
                transform.x(), transform.y(), transform.z(),
                transform.rotationX(), transform.rotationY(), transform.rotationZ(),
                transform.scaleX(), transform.scaleY(), transform.scaleZ());

        CubeGeometry cube = node.geometry();
        if (cube != null) {
            data.cube = new CubeData(cube.width(), cube.height(), cube.depth());
        }

        MeshGeometry mesh = node.meshGeometry();
        if (mesh != null) {
            List<MeshVertexData> vertices = new ArrayList<>();
            for (MeshGeometry.Vertex vertex : mesh.vertices()) {
                vertices.add(new MeshVertexData(vertex.x(), vertex.y(), vertex.z()));
            }

            List<MeshFaceData> faces = new ArrayList<>();
            for (MeshGeometry.Face face : mesh.faces()) {
                faces.add(new MeshFaceData(face.vertices()));
            }
            data.mesh = new MeshData(vertices, faces);
        }

        data.children = new ArrayList<>();
        for (ModelNode child : node.children()) {
            data.children.add(toData(child));
        }
        return data;
    }

    private static void restoreChildren(ModelNode parent, List<NodeData> children) {
        if (children == null) return;
        for (NodeData data : children) {
            if (data == null || data.id == null) {
                throw new IllegalArgumentException("Invalid CREATE model node");
            }

            ModelNode node = new ModelNode(data.name == null ? "Node" : data.name, data.id);
            restoreNodeData(node, data);
            parent.addChild(node);
        }
    }

    private static void restoreNodeData(ModelNode node, NodeData data) {
        if (data.name != null) node.setName(data.name);
        restoreTransform(node.transform(), data.transform);

        if (data.cube != null) {
            node.setGeometry(new CubeGeometry(
                    data.cube.width, data.cube.height, data.cube.depth));
        }

        if (data.mesh != null) {
            List<MeshGeometry.Vertex> vertices = new ArrayList<>();
            if (data.mesh.vertices != null) {
                for (MeshVertexData vertex : data.mesh.vertices) {
                    vertices.add(new MeshGeometry.Vertex(vertex.x, vertex.y, vertex.z));
                }
            }

            List<MeshGeometry.Face> faces = new ArrayList<>();
            if (data.mesh.faces != null) {
                for (MeshFaceData face : data.mesh.faces) {
                    faces.add(new MeshGeometry.Face(face.vertices));
                }
            }
            node.setMeshGeometry(new MeshGeometry(vertices, faces));
        }

        restoreChildren(node, data.children);
    }

    private static void restoreTransform(Transform transform, TransformData data) {
        if (data == null) return;
        transform.position(data.x, data.y, data.z);
        transform.rotation(data.rotationX, data.rotationY, data.rotationZ);
        transform.scale(data.scaleX, data.scaleY, data.scaleZ);
    }

    private static final class NodeData {
        UUID id;
        String name;
        TransformData transform;
        CubeData cube;
        MeshData mesh;
        List<NodeData> children;
    }

    private record TransformData(
            double x, double y, double z,
            double rotationX, double rotationY, double rotationZ,
            double scaleX, double scaleY, double scaleZ) {}

    private record CubeData(double width, double height, double depth) {}

    private record MeshData(List<MeshVertexData> vertices, List<MeshFaceData> faces) {}

    private record MeshVertexData(double x, double y, double z) {}

    private record MeshFaceData(int[] vertices) {}
}
