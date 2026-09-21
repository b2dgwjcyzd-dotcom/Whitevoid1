package whitevoid.create.editor.geometry;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import whitevoid.create.model.MeshGeometry;

/**
 * Face-focused mesh modeling operations for CREATE.
 */
public final class MeshFaceOperations {
    private MeshFaceOperations() {}

public static MeshOperations.OperationResult extrudeFacesResult(
            MeshGeometry mesh, java.util.Set<Integer> selectedFaces, double amount) {
        if (mesh == null) throw new IllegalArgumentException("Mesh cannot be null");
        if (selectedFaces == null || selectedFaces.isEmpty() || amount == 0.0) {
            return new MeshOperations.OperationResult(mesh.copy(), Set.of(), Set.of(), Set.of());
        }
        java.util.Set<Integer> valid = new java.util.LinkedHashSet<>();
        for (int face : selectedFaces) {
            if (face >= 0 && face < mesh.faces().size()) valid.add(face);
        }
        if (valid.isEmpty()) return new MeshOperations.OperationResult(mesh.copy(), Set.of(), Set.of(), Set.of());

        MeshGeometry result = extrudeFaces(mesh, valid, amount);
        java.util.Map<Integer, Integer> vertexMapping = new java.util.LinkedHashMap<>();
        for (int i = 0; i < mesh.vertices().size(); i++) vertexMapping.put(i, i);
        java.util.Map<Integer, Integer> faceMapping = new java.util.LinkedHashMap<>();
        int newFace = 0;
        for (int i = 0; i < mesh.faces().size(); i++) {
            if (!valid.contains(i)) faceMapping.put(i, newFace++);
        }

        java.util.Set<Integer> createdVertices = new java.util.LinkedHashSet<>();
        for (int i = mesh.vertices().size(); i < result.vertices().size(); i++) createdVertices.add(i);

        int baseFaceCount = mesh.faces().size() - valid.size();
        java.util.Set<Integer> createdFaces = new java.util.LinkedHashSet<>();
        for (int i = baseFaceCount; i < result.faces().size(); i++) createdFaces.add(i);

        java.util.Set<Long> createdEdges = new java.util.LinkedHashSet<>();
        for (int faceIndex : createdFaces) {
            int[] ids = result.faces().get(faceIndex).vertices();
            for (int i = 0; i < ids.length; i++) {
                createdEdges.add(MeshOperationGeometry.edgeKey(ids[i], ids[(i + 1) % ids.length]));
            }
        }
        java.util.Set<Integer> focusFaces = new java.util.LinkedHashSet<>();
        int focusCount = valid.size();
        int focusSeen = 0;
        for (int faceIndex : createdFaces) {
            if (focusSeen++ >= focusCount) break;
            focusFaces.add(faceIndex);
        }
        int activeFace = focusFaces.isEmpty() ? -1 : focusFaces.iterator().next();
        java.util.Map<Integer, Integer> materialSources = new java.util.LinkedHashMap<>();
        int createdCursor = baseFaceCount;
        for (int sourceFace : valid) {
            materialSources.put(createdCursor++, sourceFace);
        }
        for (int sourceFace : valid) {
            int[] original = mesh.faces().get(sourceFace).vertices();
            for (int i = 0; i < original.length; i++) {
                int a = original[i];
                int b = original[(i + 1) % original.length];
                if (!MeshOperationGeometry.isSelectedEdge(mesh, a, b, valid)) {
                    materialSources.put(createdCursor++, sourceFace);
                }
            }
        }
        return new MeshOperations.OperationResult(result, createdVertices, createdFaces, createdEdges, vertexMapping, faceMapping,
                materialSources, MeshOperations.SelectionHint.faces(focusFaces, activeFace));
    }

public static MeshOperations.OperationResult insetFacesResult(
            MeshGeometry mesh, java.util.Set<Integer> selectedFaces, double amount) {
        if (mesh == null) throw new IllegalArgumentException("Mesh cannot be null");
        if (selectedFaces == null || selectedFaces.isEmpty() || amount <= 0.0) {
            return new MeshOperations.OperationResult(mesh.copy(), Set.of(), Set.of(), Set.of());
        }
        java.util.Set<Integer> valid = new java.util.LinkedHashSet<>();
        for (int face : selectedFaces) {
            if (face >= 0 && face < mesh.faces().size()) valid.add(face);
        }
        if (valid.isEmpty()) return new MeshOperations.OperationResult(mesh.copy(), Set.of(), Set.of(), Set.of());

        MeshGeometry result = insetFaces(mesh, valid, amount);
        java.util.Map<Integer, Integer> vertexMapping = new java.util.LinkedHashMap<>();
        for (int i = 0; i < mesh.vertices().size(); i++) vertexMapping.put(i, i);
        java.util.Map<Integer, Integer> faceMapping = new java.util.LinkedHashMap<>();
        int newFace = 0;
        for (int i = 0; i < mesh.faces().size(); i++) {
            if (!valid.contains(i)) faceMapping.put(i, newFace++);
        }

        java.util.Set<Integer> createdVertices = new java.util.LinkedHashSet<>();
        for (int i = mesh.vertices().size(); i < result.vertices().size(); i++) createdVertices.add(i);

        int baseFaceCount = mesh.faces().size() - valid.size();
        java.util.Set<Integer> createdFaces = new java.util.LinkedHashSet<>();
        for (int i = baseFaceCount; i < result.faces().size(); i++) createdFaces.add(i);

        java.util.Set<Long> createdEdges = new java.util.LinkedHashSet<>();
        for (int faceIndex : createdFaces) {
            int[] ids = result.faces().get(faceIndex).vertices();
            for (int i = 0; i < ids.length; i++) {
                createdEdges.add(MeshOperationGeometry.edgeKey(ids[i], ids[(i + 1) % ids.length]));
            }
        }
        java.util.Set<Integer> focusFaces = new java.util.LinkedHashSet<>();
        int focusCount = valid.size();
        int focusSeen = 0;
        for (int faceIndex : createdFaces) {
            if (focusSeen++ >= focusCount) break;
            focusFaces.add(faceIndex);
        }
        int activeFace = focusFaces.isEmpty() ? -1 : focusFaces.iterator().next();
        java.util.Map<Integer, Integer> materialSources = new java.util.LinkedHashMap<>();
        int createdCursor = baseFaceCount;
        for (int sourceFace : valid) {
            materialSources.put(createdCursor++, sourceFace);
        }
        for (int sourceFace : valid) {
            int[] original = mesh.faces().get(sourceFace).vertices();
            for (int i = 0; i < original.length; i++) {
                int a = original[i];
                int b = original[(i + 1) % original.length];
                if (!MeshOperationGeometry.isSelectedEdge(mesh, a, b, valid)) {
                    materialSources.put(createdCursor++, sourceFace);
                }
            }
        }
        return new MeshOperations.OperationResult(result, createdVertices, createdFaces, createdEdges, vertexMapping, faceMapping,
                materialSources, MeshOperations.SelectionHint.faces(focusFaces, activeFace));
    }

public static MeshOperations.OperationResult extrudeFaceResult(MeshGeometry mesh, int faceIndex, double amount) {
        if (mesh == null) throw new IllegalArgumentException("Mesh cannot be null");
        if (faceIndex < 0 || faceIndex >= mesh.faces().size()) {
            throw new IllegalArgumentException("Invalid face index: " + faceIndex);
        }

        MeshGeometry result = extrudeFace(mesh, faceIndex, amount);
        if (amount == 0.0) return new MeshOperations.OperationResult(result, Set.of(), Set.of(), Set.of());

        java.util.Map<Integer, Integer> vertexMapping = new java.util.LinkedHashMap<>();
        for (int i = 0; i < mesh.vertices().size(); i++) vertexMapping.put(i, i);

        java.util.Map<Integer, Integer> faceMapping = new java.util.LinkedHashMap<>();
        int mappedFace = 0;
        for (int i = 0; i < mesh.faces().size(); i++) {
            if (i != faceIndex) faceMapping.put(i, mappedFace++);
        }

        int baseFaceCount = mesh.faces().size() - 1;
        java.util.Set<Integer> createdVertices = new java.util.LinkedHashSet<>();
        for (int i = mesh.vertices().size(); i < result.vertices().size(); i++) createdVertices.add(i);

        java.util.Set<Integer> createdFaces = new java.util.LinkedHashSet<>();
        for (int i = baseFaceCount; i < result.faces().size(); i++) createdFaces.add(i);

        java.util.Set<Long> createdEdges = new java.util.LinkedHashSet<>();
        for (int created : createdFaces) {
            int[] ids = result.faces().get(created).vertices();
            for (int i = 0; i < ids.length; i++) {
                createdEdges.add(MeshOperationGeometry.edgeKey(ids[i], ids[(i + 1) % ids.length]));
            }
        }

        int activeFace = result.faces().isEmpty() ? -1 : result.faces().size() - 1;
        Set<Integer> focus = activeFace >= 0 ? Set.of(activeFace) : Set.of();
        java.util.Map<Integer, Integer> materialSources = new java.util.LinkedHashMap<>();
        for (int created : createdFaces) materialSources.put(created, faceIndex);
        return new MeshOperations.OperationResult(result, createdVertices, createdFaces, createdEdges,
                vertexMapping, faceMapping, materialSources, MeshOperations.SelectionHint.faces(focus, activeFace));
    }

public static MeshGeometry extrudeFace(MeshGeometry mesh, int faceIndex, double amount) {
        if (mesh == null) throw new IllegalArgumentException("Mesh cannot be null");
        if (faceIndex < 0 || faceIndex >= mesh.faces().size()) {
            throw new IllegalArgumentException("Invalid face index: " + faceIndex);
        }
        if (amount == 0.0) return mesh.copy();

        int[] indices = mesh.faces().get(faceIndex).vertices();
        if (indices.length < 3) {
            throw new IllegalArgumentException("Extrude requires a polygon face");
        }

        var a = mesh.vertices().get(indices[0]);
        var b = mesh.vertices().get(indices[1]);
        var d = mesh.vertices().get(indices[2]);

        double ux = b.x() - a.x();
        double uy = b.y() - a.y();
        double uz = b.z() - a.z();
        double vx = d.x() - a.x();
        double vy = d.y() - a.y();
        double vz = d.z() - a.z();

        double nx = uy * vz - uz * vy;
        double ny = uz * vx - ux * vz;
        double nz = ux * vy - uy * vx;
        double length = Math.sqrt(nx * nx + ny * ny + nz * nz);
        if (length < 1e-9) {
            throw new IllegalArgumentException("Cannot extrude a degenerate face");
        }
        nx /= length;
        ny /= length;
        nz /= length;

        List<MeshGeometry.Vertex> vertices = new ArrayList<>(mesh.vertices());
        int[] extruded = new int[indices.length];

        for (int i = 0; i < indices.length; i++) {
            var vtx = vertices.get(indices[i]);
            extruded[i] = vertices.size();
            vertices.add(new MeshGeometry.Vertex(
                    vtx.x() + nx * amount,
                    vtx.y() + ny * amount,
                    vtx.z() + nz * amount));
        }

        List<MeshGeometry.Face> faces = new ArrayList<>();
        for (int i = 0; i < mesh.faces().size(); i++) {
            if (i != faceIndex) faces.add(mesh.faces().get(i));
        }

        for (int i = 0; i < indices.length; i++) {
            int next = (i + 1) % indices.length;
            faces.add(new MeshGeometry.Face(
                    indices[i], indices[next], extruded[next], extruded[i]));
        }

        int[] cap = new int[extruded.length];
        for (int i = 0; i < extruded.length; i++) {
            cap[i] = extruded[extruded.length - 1 - i];
        }
        faces.add(new MeshGeometry.Face(cap));

        return new MeshGeometry(vertices, faces);
    }

public static MeshGeometry extrudeFaces(MeshGeometry mesh, java.util.Set<Integer> selectedFaces, double amount) {
        if (mesh == null) throw new IllegalArgumentException("Mesh cannot be null");
        if (selectedFaces == null || selectedFaces.isEmpty() || amount == 0.0) return mesh.copy();

        java.util.Set<Integer> valid = new java.util.LinkedHashSet<>();
        for (int face : selectedFaces) {
            if (face >= 0 && face < mesh.faces().size()) valid.add(face);
        }
        if (valid.isEmpty()) return mesh.copy();

        java.util.Map<Integer, double[]> normals = new java.util.LinkedHashMap<>();
        java.util.Set<Integer> affected = new java.util.LinkedHashSet<>();
        for (int faceIndex : valid) {
            MeshGeometry.Face face = mesh.faces().get(faceIndex);
            double[] n = MeshOperationGeometry.faceNormal(mesh, face);
            for (int id : face.vertices()) {
                affected.add(id);
                double[] sum = normals.computeIfAbsent(id, ignored -> new double[3]);
                sum[0] += n[0]; sum[1] += n[1]; sum[2] += n[2];
            }
        }

        List<MeshGeometry.Vertex> vertices = new ArrayList<>(mesh.vertices());
        java.util.Map<Integer, Integer> duplicate = new java.util.LinkedHashMap<>();
        for (int id : affected) {
            double[] n = normals.get(id);
            double len = Math.sqrt(n[0] * n[0] + n[1] * n[1] + n[2] * n[2]);
            if (len < 1e-9) { n[0] = 0; n[1] = 1; n[2] = 0; }
            else { n[0] /= len; n[1] /= len; n[2] /= len; }
            MeshGeometry.Vertex v = mesh.vertices().get(id);
            int copy = vertices.size();
            vertices.add(new MeshGeometry.Vertex(v.x() + n[0] * amount,
                    v.y() + n[1] * amount, v.z() + n[2] * amount));
            duplicate.put(id, copy);
        }

        List<MeshGeometry.Face> faces = new ArrayList<>();
        for (int i = 0; i < mesh.faces().size(); i++) {
            if (!valid.contains(i)) faces.add(mesh.faces().get(i));
        }

        for (int faceIndex : valid) {
            int[] original = mesh.faces().get(faceIndex).vertices();
            int[] moved = new int[original.length];
            for (int i = 0; i < original.length; i++) moved[i] = duplicate.get(original[i]);
            MeshOperationGeometry.reverse(moved);
            faces.add(new MeshGeometry.Face(moved));
        }

        for (int faceIndex : valid) {
            int[] original = mesh.faces().get(faceIndex).vertices();
            for (int i = 0; i < original.length; i++) {
                int a = original[i];
                int b = original[(i + 1) % original.length];
                if (!MeshOperationGeometry.isSelectedEdge(mesh, a, b, valid)) {
                    faces.add(new MeshGeometry.Face(a, b, duplicate.get(b), duplicate.get(a)));
                }
            }
        }
        return new MeshGeometry(vertices, faces);
    }

public static MeshGeometry insetFaces(MeshGeometry mesh, java.util.Set<Integer> selectedFaces, double amount) {
        if (mesh == null) throw new IllegalArgumentException("Mesh cannot be null");
        if (selectedFaces == null || selectedFaces.isEmpty() || amount <= 0.0) return mesh.copy();

        java.util.Set<Integer> valid = new java.util.LinkedHashSet<>();
        for (int face : selectedFaces) {
            if (face >= 0 && face < mesh.faces().size()) valid.add(face);
        }
        if (valid.isEmpty()) return mesh.copy();

        double factor = Math.max(0.0, Math.min(0.99, amount));
        java.util.Map<Integer, double[]> targets = new java.util.LinkedHashMap<>();
        java.util.Map<Integer, Integer> counts = new java.util.LinkedHashMap<>();

        // Each shared vertex gets the average center of the selected faces
        // touching it. This preserves the local shape of a multi-face region.
        for (int faceIndex : valid) {
            MeshGeometry.Face face = mesh.faces().get(faceIndex);
            double[] center = MeshOperationGeometry.centroid(mesh, face);
            for (int id : face.vertices()) {
                double[] target = targets.computeIfAbsent(id, ignored -> new double[3]);
                target[0] += center[0];
                target[1] += center[1];
                target[2] += center[2];
                counts.put(id, counts.getOrDefault(id, 0) + 1);
            }
        }

        List<MeshGeometry.Vertex> vertices = new ArrayList<>(mesh.vertices());
        java.util.Map<Integer, Integer> inner = new java.util.LinkedHashMap<>();
        for (int id : targets.keySet()) {
            double[] target = targets.get(id);
            int count = counts.get(id);
            target[0] /= count; target[1] /= count; target[2] /= count;

            MeshGeometry.Vertex v = mesh.vertices().get(id);
            int copy = vertices.size();
            vertices.add(new MeshGeometry.Vertex(
                    v.x() + (target[0] - v.x()) * factor,
                    v.y() + (target[1] - v.y()) * factor,
                    v.z() + (target[2] - v.z()) * factor));
            inner.put(id, copy);
        }

        List<MeshGeometry.Face> faces = new ArrayList<>();
        for (int i = 0; i < mesh.faces().size(); i++) {
            if (!valid.contains(i)) faces.add(mesh.faces().get(i));
        }

        // Add the inset faces first so the caller can select them as a region.
        for (int faceIndex : valid) {
            int[] original = mesh.faces().get(faceIndex).vertices();
            int[] inset = new int[original.length];
            for (int i = 0; i < original.length; i++) inset[i] = inner.get(original[i]);
            faces.add(new MeshGeometry.Face(inset));
        }

        // Build a rim only on the boundary of the selected region.
        for (int faceIndex : valid) {
            int[] original = mesh.faces().get(faceIndex).vertices();
            for (int i = 0; i < original.length; i++) {
                int a = original[i];
                int b = original[(i + 1) % original.length];
                if (!MeshOperationGeometry.isSelectedEdge(mesh, a, b, valid)) {
                    faces.add(new MeshGeometry.Face(a, b, inner.get(b), inner.get(a)));
                }
            }
        }

        return new MeshGeometry(vertices, faces);
    }

public static MeshOperations.OperationResult insetFaceResult(MeshGeometry mesh, int faceIndex, double amount) {
        if (mesh == null) throw new IllegalArgumentException("Mesh cannot be null");
        if (faceIndex < 0 || faceIndex >= mesh.faces().size()) {
            throw new IllegalArgumentException("Invalid face index: " + faceIndex);
        }

        MeshGeometry result = insetFace(mesh, faceIndex, amount);
        if (amount <= 0.0) return new MeshOperations.OperationResult(result, Set.of(), Set.of(), Set.of());

        java.util.Map<Integer, Integer> vertexMapping = new java.util.LinkedHashMap<>();
        for (int i = 0; i < mesh.vertices().size(); i++) vertexMapping.put(i, i);

        java.util.Map<Integer, Integer> faceMapping = new java.util.LinkedHashMap<>();
        int mappedFace = 0;
        for (int i = 0; i < mesh.faces().size(); i++) {
            if (i != faceIndex) faceMapping.put(i, mappedFace++);
        }

        int baseFaceCount = mesh.faces().size() - 1;
        java.util.Set<Integer> createdVertices = new java.util.LinkedHashSet<>();
        for (int i = mesh.vertices().size(); i < result.vertices().size(); i++) createdVertices.add(i);

        java.util.Set<Integer> createdFaces = new java.util.LinkedHashSet<>();
        for (int i = baseFaceCount; i < result.faces().size(); i++) createdFaces.add(i);

        java.util.Set<Long> createdEdges = new java.util.LinkedHashSet<>();
        for (int created : createdFaces) {
            int[] ids = result.faces().get(created).vertices();
            for (int i = 0; i < ids.length; i++) {
                createdEdges.add(MeshOperationGeometry.edgeKey(ids[i], ids[(i + 1) % ids.length]));
            }
        }

        int activeFace = result.faces().isEmpty() ? -1 : result.faces().size() - 1;
        Set<Integer> focus = activeFace >= 0 ? Set.of(activeFace) : Set.of();
        java.util.Map<Integer, Integer> materialSources = new java.util.LinkedHashMap<>();
        for (int created : createdFaces) materialSources.put(created, faceIndex);
        return new MeshOperations.OperationResult(result, createdVertices, createdFaces, createdEdges,
                vertexMapping, faceMapping, materialSources, MeshOperations.SelectionHint.faces(focus, activeFace));
    }

public static MeshGeometry insetFace(MeshGeometry mesh, int faceIndex, double amount) {
        if (mesh == null) throw new IllegalArgumentException("Mesh cannot be null");
        if (faceIndex < 0 || faceIndex >= mesh.faces().size()) {
            throw new IllegalArgumentException("Invalid face index: " + faceIndex);
        }
        if (amount <= 0.0) return mesh.copy();

        MeshGeometry.Face selected = mesh.faces().get(faceIndex);
        int[] indices = selected.vertices();
        if (indices.length < 3) {
            throw new IllegalArgumentException("Inset requires a polygon face");
        }

        List<MeshGeometry.Vertex> vertices = new ArrayList<>(mesh.vertices());
        List<MeshGeometry.Face> faces = new ArrayList<>();
        for (int i = 0; i < mesh.faces().size(); i++) {
            if (i != faceIndex) faces.add(mesh.faces().get(i));
        }

        int[] inner = new int[indices.length];
        double cx = 0.0, cy = 0.0, cz = 0.0;
        for (int index : indices) {
            var v = vertices.get(index);
            cx += v.x(); cy += v.y(); cz += v.z();
        }
        cx /= indices.length; cy /= indices.length; cz /= indices.length;

        double factor = Math.max(0.0, Math.min(0.99, amount));
        for (int i = 0; i < indices.length; i++) {
            var v = vertices.get(indices[i]);
            inner[i] = vertices.size();
            vertices.add(new MeshGeometry.Vertex(
                    v.x() + (cx - v.x()) * factor,
                    v.y() + (cy - v.y()) * factor,
                    v.z() + (cz - v.z()) * factor));
        }

        for (int i = 0; i < indices.length; i++) {
            int next = (i + 1) % indices.length;
            faces.add(new MeshGeometry.Face(
                    indices[i], indices[next], inner[next], inner[i]));
        }

        faces.add(new MeshGeometry.Face(inner));
        return new MeshGeometry(vertices, faces);
    }
}
