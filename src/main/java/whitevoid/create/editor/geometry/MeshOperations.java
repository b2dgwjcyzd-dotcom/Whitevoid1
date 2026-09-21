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
        return MeshVertexOperations.moveVertex(mesh, vertexIndex, dx, dy, dz);
    }
