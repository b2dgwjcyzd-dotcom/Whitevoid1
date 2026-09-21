package whitevoid.create.editor.geometry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import whitevoid.create.model.MeshGeometry;

/**
 * Edge-focused mesh modeling operations for CREATE.
 */
public final class MeshEdgeOperations {
    private MeshEdgeOperations() {}

public static MeshOperations.OperationResult extrudeEdgesResult(
            MeshGeometry mesh, java.util.Set<Long> selectedEdges, double amount) {
        if (mesh == null) throw new IllegalArgumentException("Mesh cannot be null");
        if (selectedEdges == null || selectedEdges.isEmpty() || amount == 0.0) {
            return new MeshOperations.OperationResult(mesh.copy(), Set.of(), Set.of(), Set.of(), Map.of(), Map.of());
        }

        java.util.Set<Long> valid = new java.util.LinkedHashSet<>();
        java.util.Set<Integer> affected = new java.util.LinkedHashSet<>();
        java.util.Map<Integer, double[]> normals = new java.util.LinkedHashMap<>();

        for (long key : selectedEdges) {
            int a = (int) (key >>> 32);
            int b = (int) key;
            MeshOperationGeometry.validateEdge(mesh, a, b);
            if (MeshOperationGeometry.adjacentFaces(mesh, a, b).isEmpty()) {
                throw new IllegalArgumentException("Selected edge is not connected to a face");
            }
            valid.add(key);
            affected.add(a);
            affected.add(b);
            for (int faceIndex : MeshOperationGeometry.adjacentFaces(mesh, a, b)) {
                double[] n = MeshOperationGeometry.faceNormal(mesh, mesh.faces().get(faceIndex));
                for (int vertex : new int[]{a, b}) {
                    double[] sum = normals.computeIfAbsent(vertex, ignored -> new double[3]);
                    sum[0] += n[0]; sum[1] += n[1]; sum[2] += n[2];
                }
            }
        }

        java.util.List<Integer> ordered = new java.util.ArrayList<>(affected);
        java.util.Collections.sort(ordered);
        java.util.List<MeshGeometry.Vertex> vertices = new java.util.ArrayList<>(mesh.vertices());
        java.util.Map<Integer, Integer> duplicate = new java.util.LinkedHashMap<>();
        java.util.Set<Integer> createdVertices = new java.util.LinkedHashSet<>();

        for (int id : ordered) {
            double[] n = normals.get(id);
            double len = Math.sqrt(n[0] * n[0] + n[1] * n[1] + n[2] * n[2]);
            if (len < 1e-9) { n[0] = 0; n[1] = 1; n[2] = 0; }
            else { n[0] /= len; n[1] /= len; n[2] /= len; }

            MeshGeometry.Vertex v = mesh.vertices().get(id);
            int copy = vertices.size();
            vertices.add(new MeshGeometry.Vertex(
                    v.x() + n[0] * amount,
                    v.y() + n[1] * amount,
                    v.z() + n[2] * amount));
            duplicate.put(id, copy);
            createdVertices.add(copy);
        }

        java.util.List<MeshGeometry.Face> faces = new java.util.ArrayList<>(mesh.faces());
        java.util.Set<Integer> createdFaces = new java.util.LinkedHashSet<>();
        java.util.Set<Long> createdEdges = new java.util.LinkedHashSet<>();

        for (long key : valid) {
            int a = (int) (key >>> 32);
            int b = (int) key;
            int da = duplicate.get(a);
            int db = duplicate.get(b);
            int faceIndex = faces.size();
            faces.add(new MeshGeometry.Face(a, b, db, da));
            createdFaces.add(faceIndex);
            createdEdges.add(MeshTopology.MeshOperationGeometry.edgeKey(da, db));
            createdEdges.add(MeshTopology.MeshOperationGeometry.edgeKey(a, da));
            createdEdges.add(MeshTopology.MeshOperationGeometry.edgeKey(b, db));
        }

        java.util.Map<Integer, Integer> vertexMapping = new java.util.LinkedHashMap<>();
        for (int i = 0; i < mesh.vertices().size(); i++) vertexMapping.put(i, i);
        java.util.Map<Integer, Integer> faceMapping = new java.util.LinkedHashMap<>();
        for (int i = 0; i < mesh.faces().size(); i++) faceMapping.put(i, i);

        java.util.Set<Long> focusEdges = new java.util.LinkedHashSet<>();
        for (long edge : createdEdges) {
            int a = MeshTopology.edgeA(edge);
            int b = MeshTopology.edgeB(edge);
            if (a >= mesh.vertices().size() && b >= mesh.vertices().size()) {
                focusEdges.add(edge);
            }
        }
        return new MeshOperations.OperationResult(new MeshGeometry(vertices, faces),
                createdVertices, createdFaces, createdEdges, vertexMapping, faceMapping,
                MeshOperations.SelectionHint.edges(focusEdges, focusEdges.isEmpty() ? -1L : focusEdges.iterator().next()));
    }

public static MeshGeometry extrudeEdge(MeshGeometry mesh, int a, int b, double amount) {
        MeshOperationGeometry.validateEdge(mesh, a, b);
        if (amount == 0.0) return mesh.copy();

        List<Integer> adjacent = MeshOperationGeometry.adjacentFaces(mesh, a, b);
        if (adjacent.isEmpty()) throw new IllegalArgumentException("Edge is not connected to a face");

        double nx = 0.0, ny = 0.0, nz = 0.0;
        for (int faceIndex : adjacent) {
            double[] n = MeshOperationGeometry.faceNormal(mesh, mesh.faces().get(faceIndex));
            nx += n[0]; ny += n[1]; nz += n[2];
        }
        double len = Math.sqrt(nx * nx + ny * ny + nz * nz);
        if (len < 1e-9) throw new IllegalArgumentException("Cannot extrude degenerate edge");
        nx /= len; ny /= len; nz /= len;

        List<MeshGeometry.Vertex> vertices = new ArrayList<>(mesh.vertices());
        int na = vertices.size();
        var va = mesh.vertices().get(a);
        vertices.add(new MeshGeometry.Vertex(va.x() + nx * amount, va.y() + ny * amount, va.z() + nz * amount));
        int nb = vertices.size();
        var vb = mesh.vertices().get(b);
        vertices.add(new MeshGeometry.Vertex(vb.x() + nx * amount, vb.y() + ny * amount, vb.z() + nz * amount));

        List<MeshGeometry.Face> faces = new ArrayList<>(mesh.faces());
        faces.add(new MeshGeometry.Face(a, b, nb, na));
        return new MeshGeometry(vertices, faces);
    }

public static MeshGeometry bevelEdge(MeshGeometry mesh, int a, int b, double amount) {
        MeshOperationGeometry.validateEdge(mesh, a, b);
        if (amount <= 0.0) return mesh.copy();

        List<Integer> adjacent = MeshOperationGeometry.adjacentFaces(mesh, a, b);
        if (adjacent.size() != 2) {
            throw new IllegalArgumentException("Bevel currently requires exactly two adjacent faces");
        }

        double edgeLength = MeshOperationGeometry.distance(mesh.vertices().get(a), mesh.vertices().get(b));
        double offset = Math.min(amount, edgeLength * 0.49);

        List<MeshGeometry.Vertex> vertices = new ArrayList<>(mesh.vertices());
        int[] newA = new int[2];
        int[] newB = new int[2];

        for (int i = 0; i < 2; i++) {
            var face = mesh.faces().get(adjacent.get(i));
            double[] ca = MeshOperationGeometry.centroid(mesh, face);
            var va = mesh.vertices().get(a);
            var vb = mesh.vertices().get(b);

            newA[i] = vertices.size();
            vertices.add(MeshOperationGeometry.toward(va, ca, offset));
            newB[i] = vertices.size();
            vertices.add(MeshOperationGeometry.toward(vb, ca, offset));
        }

        List<MeshGeometry.Face> faces = new ArrayList<>();
        for (int i = 0; i < mesh.faces().size(); i++) {
            if (i == adjacent.get(0) || i == adjacent.get(1)) continue;
            faces.add(mesh.faces().get(i));
        }

        for (int side = 0; side < 2; side++) {
            int faceIndex = adjacent.get(side);
            int[] original = mesh.faces().get(faceIndex).vertices();
            int[] replaced = original.clone();
            for (int i = 0; i < replaced.length; i++) {
                int current = replaced[i];
                int next = replaced[(i + 1) % replaced.length];
                if (current == a && next == b) {
                    replaced[i] = newA[side];
                    replaced[(i + 1) % replaced.length] = newB[side];
                } else if (current == b && next == a) {
                    replaced[i] = newB[side];
                    replaced[(i + 1) % replaced.length] = newA[side];
                }
            }
            faces.add(new MeshGeometry.Face(replaced));
        }

        faces.add(new MeshGeometry.Face(newA[0], newB[0], newB[1], newA[1]));
        return new MeshGeometry(vertices, faces);
    }

public static MeshGeometry extrudeEdges(MeshGeometry mesh, java.util.Set<Long> selectedEdges, double amount) {
        return extrudeEdgesResult(mesh, selectedEdges, amount).mesh();
    }

public static MeshOperations.OperationResult bevelEdgesResult(
            MeshGeometry mesh, java.util.Set<Long> selectedEdges, double amount) {
        if (mesh == null) throw new IllegalArgumentException("Mesh cannot be null");
        if (selectedEdges == null || selectedEdges.isEmpty() || amount <= 0.0) {
            return new MeshOperations.OperationResult(mesh.copy(), Set.of(), Set.of(), Set.of());
        }

        MeshGeometry result = bevelEdges(mesh, selectedEdges, amount);
        java.util.Map<Integer, Integer> vertexMapping = new java.util.LinkedHashMap<>();
        for (int i = 0; i < mesh.vertices().size(); i++) vertexMapping.put(i, i);
        java.util.Map<Integer, Integer> faceMapping = new java.util.LinkedHashMap<>();
        for (int i = 0; i < mesh.faces().size(); i++) faceMapping.put(i, i);

        java.util.Set<Integer> createdVertices = new java.util.LinkedHashSet<>();
        for (int i = mesh.vertices().size(); i < result.vertices().size(); i++) {
            createdVertices.add(i);
        }

        java.util.Set<Integer> createdFaces = new java.util.LinkedHashSet<>();
        for (int i = mesh.faces().size(); i < result.faces().size(); i++) {
            createdFaces.add(i);
        }

        java.util.Set<Long> createdEdges = new java.util.LinkedHashSet<>();
        for (int faceIndex : createdFaces) {
            int[] ids = result.faces().get(faceIndex).vertices();
            for (int i = 0; i < ids.length; i++) {
                createdEdges.add(MeshTopology.MeshOperationGeometry.edgeKey(ids[i], ids[(i + 1) % ids.length]));
            }
        }

        java.util.Set<Long> focusEdges = new java.util.LinkedHashSet<>(createdEdges);
        long activeEdge = focusEdges.isEmpty() ? -1L : focusEdges.iterator().next();
        return new MeshOperations.OperationResult(result, createdVertices, createdFaces, createdEdges, vertexMapping, faceMapping,
                MeshOperations.SelectionHint.edges(focusEdges, activeEdge));
    }

public static MeshGeometry bevelEdges(MeshGeometry mesh, java.util.Set<Long> selectedEdges, double amount) {
        if (mesh == null) throw new IllegalArgumentException("Mesh cannot be null");
        if (selectedEdges == null || selectedEdges.isEmpty() || amount <= 0.0) return mesh.copy();

        java.util.Set<Long> valid = new java.util.LinkedHashSet<>();
        for (long key : selectedEdges) {
            int a = (int) (key >>> 32);
            int b = (int) key;
            MeshOperationGeometry.validateEdge(mesh, a, b);
            if (MeshOperationGeometry.adjacentFaces(mesh, a, b).size() != 2) {
                throw new IllegalArgumentException("Bevel requires exactly two adjacent faces for every selected edge");
            }
            valid.add(key);
        }
        if (valid.isEmpty()) return mesh.copy();

        List<MeshGeometry.Vertex> vertices = new ArrayList<>(mesh.vertices());
        java.util.Map<Long, int[]> faceCopies = new java.util.LinkedHashMap<>();

        // Create one offset pair for each edge on each adjacent face.
        for (long key : valid) {
            int a = (int) (key >>> 32);
            int b = (int) key;
            List<Integer> adjacent = MeshOperationGeometry.adjacentFaces(mesh, a, b);
            int[] copies = new int[4];

            for (int side = 0; side < 2; side++) {
                MeshGeometry.Face face = mesh.faces().get(adjacent.get(side));
                double[] center = MeshOperationGeometry.centroid(mesh, face);
                double edgeLength = MeshOperationGeometry.distance(mesh.vertices().get(a), mesh.vertices().get(b));
                double offset = Math.min(amount, edgeLength * 0.49);

                copies[side * 2] = vertices.size();
                vertices.add(MeshOperationGeometry.toward(mesh.vertices().get(a), center, offset));
                copies[side * 2 + 1] = vertices.size();
                vertices.add(MeshOperationGeometry.toward(mesh.vertices().get(b), center, offset));
            }
            faceCopies.put(key, copies);
        }

        List<MeshGeometry.Face> faces = new ArrayList<>();
        for (int faceIndex = 0; faceIndex < mesh.faces().size(); faceIndex++) {
            int[] original = mesh.faces().get(faceIndex).vertices();
            int[] replaced = original.clone();

            for (int i = 0; i < original.length; i++) {
                int a = original[i];
                int b = original[(i + 1) % original.length];
                long key = MeshOperationGeometry.edgeKey(a, b);
                int[] copies = faceCopies.get(key);
                if (copies == null) continue;

                List<Integer> adjacent = MeshOperationGeometry.adjacentFaces(mesh, a, b);
                int side = adjacent.indexOf(faceIndex);
                if (side < 0) continue;

                // Preserve the winding of the face edge.
                int na = copies[side * 2];
                int nb = copies[side * 2 + 1];
                if (a > b) {
                    int tmp = na; na = nb; nb = tmp;
                }
                replaced[i] = na;
                replaced[(i + 1) % original.length] = nb;
            }

            faces.add(new MeshGeometry.Face(replaced));
        }

        // The new bevel surface bridges the two offset copies of each edge.
        for (long key : valid) {
            int[] copies = faceCopies.get(key);
            faces.add(new MeshGeometry.Face(copies[0], copies[1], copies[3], copies[2]));
        }

        return new MeshGeometry(vertices, faces);
    }
}
