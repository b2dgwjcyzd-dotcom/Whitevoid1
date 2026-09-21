package whitevoid.create.editor.geometry;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.LinkedHashSet;
import whitevoid.create.model.MeshGeometry;

public final class MeshOperations {
    private MeshOperations() {}

    public record OperationResult(
            MeshGeometry mesh,
            Set<Integer> createdVertices,
            Set<Integer> createdFaces,
            Set<Long> createdEdges,
            Map<Integer, Integer> vertexMapping,
            Map<Integer, Integer> faceMapping,
            SelectionHint selectionHint) {
        public OperationResult(
                MeshGeometry mesh,
                Set<Integer> createdVertices,
                Set<Integer> createdFaces,
                Set<Long> createdEdges,
                Map<Integer, Integer> vertexMapping,
                Map<Integer, Integer> faceMapping) {
            this(mesh, createdVertices, createdFaces, createdEdges,
                    vertexMapping, faceMapping, SelectionHint.preserve());
        }
        public OperationResult {
            if (mesh == null) throw new IllegalArgumentException("Mesh cannot be null");
            createdVertices = Collections.unmodifiableSet(new LinkedHashSet<>(createdVertices));
            createdFaces = Collections.unmodifiableSet(new LinkedHashSet<>(createdFaces));
            createdEdges = Collections.unmodifiableSet(new LinkedHashSet<>(createdEdges));
            vertexMapping = Collections.unmodifiableMap(new java.util.LinkedHashMap<>(vertexMapping));
            faceMapping = Collections.unmodifiableMap(new java.util.LinkedHashMap<>(faceMapping));
            if (selectionHint == null) selectionHint = SelectionHint.preserve();
        }
    }

    public record SelectionHint(
            Set<Integer> vertices,
            Set<Long> edges,
            Set<Integer> faces,
            int activeVertex,
            long activeEdge,
            int activeFace) {
        public SelectionHint {
            vertices = Collections.unmodifiableSet(new LinkedHashSet<>(vertices));
            edges = Collections.unmodifiableSet(new LinkedHashSet<>(edges));
            faces = Collections.unmodifiableSet(new LinkedHashSet<>(faces));
        }

        public static SelectionHint preserve() {
            return new SelectionHint(Set.of(), Set.of(), Set.of(), -1, -1L, -1);
        }

        public static SelectionHint vertices(Set<Integer> values, int active) {
            return new SelectionHint(values, Set.of(), Set.of(), active, -1L, -1);
        }

        public static SelectionHint edges(Set<Long> values, long active) {
            return new SelectionHint(Set.of(), values, Set.of(), -1, active, -1);
        }

        public static SelectionHint faces(Set<Integer> values, int active) {
            return new SelectionHint(Set.of(), Set.of(), values, -1, -1L, active);
        }
    }

        public static OperationResult extrudeEdgesResult(
            MeshGeometry mesh, java.util.Set<Long> selectedEdges, double amount) {
        return MeshEdgeOperations.extrudeEdgesResult(mesh, selectedEdges, amount);
    }


    public static MeshGeometry moveVertex(MeshGeometry mesh, int vertexIndex,
                                             double dx, double dy, double dz) {
        if (mesh == null) throw new IllegalArgumentException("Mesh cannot be null");
        if (vertexIndex < 0 || vertexIndex >= mesh.vertices().size()) {
            throw new IllegalArgumentException("Invalid vertex index: " + vertexIndex);
        }

        List<MeshGeometry.Vertex> vertices = new ArrayList<>(mesh.vertices());
        var v = vertices.get(vertexIndex);
        vertices.set(vertexIndex, new MeshGeometry.Vertex(
                v.x() + dx, v.y() + dy, v.z() + dz));
        return new MeshGeometry(vertices, mesh.faces());
    }

        public static MeshGeometry extrudeEdge(MeshGeometry mesh, int a, int b, double amount) {
        return MeshEdgeOperations.extrudeEdge(mesh, a, b, amount);
    }

        public static MeshGeometry bevelEdge(MeshGeometry mesh, int a, int b, double amount) {
        return MeshEdgeOperations.bevelEdge(mesh, a, b, amount);
    }

    /**
     * Extrudes multiple selected edges as one connected strip.
     * Shared vertices are duplicated once and every selected edge receives
     * a connecting quad, so edge loops/rings can be extruded together.
     */
        public static MeshGeometry extrudeEdges(MeshGeometry mesh, java.util.Set<Long> selectedEdges, double amount) {
        return MeshEdgeOperations.extrudeEdges(mesh, selectedEdges, amount);
    }

    /**
     * Bevels a selected manifold edge region in one operation.
     * Each selected edge must have exactly two adjacent faces.
     */
        public static OperationResult bevelEdgesResult(
            MeshGeometry mesh, java.util.Set<Long> selectedEdges, double amount) {
        return MeshEdgeOperations.bevelEdgesResult(mesh, selectedEdges, amount);
    }

        public static MeshGeometry bevelEdges(MeshGeometry mesh, java.util.Set<Long> selectedEdges, double amount) {
        return MeshEdgeOperations.bevelEdges(mesh, selectedEdges, amount);
    }

        public static OperationResult extrudeFacesResult(MeshGeometry mesh, java.util.Set<Integer> selectedFaces, double amount) {
        return MeshFaceOperations.extrudeFacesResult(mesh, selectedFaces, amount);
    }

        public static OperationResult insetFacesResult(MeshGeometry mesh, java.util.Set<Integer> selectedFaces, double amount) {
        return MeshFaceOperations.insetFacesResult(mesh, selectedFaces, amount);
    }

        public static OperationResult extrudeFaceResult(MeshGeometry mesh, int faceIndex, double amount) {
        return MeshFaceOperations.extrudeFaceResult(mesh, faceIndex, amount);
    }

        public static MeshGeometry extrudeFace(MeshGeometry mesh, int faceIndex, double amount) {
        return MeshFaceOperations.extrudeFace(mesh, faceIndex, amount);
    }

    /** Extrudes a selected face region as one connected operation. */
        public static MeshGeometry extrudeFaces(MeshGeometry mesh, java.util.Set<Integer> selectedFaces, double amount) {
        return MeshFaceOperations.extrudeFaces(mesh, selectedFaces, amount);
    }


    /**
     * Insets a connected face region as one operation. Shared vertices are
     * duplicated once, internal selected edges stay internal, and only the
     * outer boundary receives the inset rim.
     */
        public static MeshGeometry insetFaces(MeshGeometry mesh, java.util.Set<Integer> selectedFaces, double amount) {
        return MeshFaceOperations.insetFaces(mesh, selectedFaces, amount);
    }

        public static OperationResult insetFaceResult(MeshGeometry mesh, int faceIndex, double amount) {
        return MeshFaceOperations.insetFaceResult(mesh, faceIndex, amount);
    }

        public static MeshGeometry insetFace(MeshGeometry mesh, int faceIndex, double amount) {
        return MeshFaceOperations.insetFace(mesh, faceIndex, amount);
    }
}
