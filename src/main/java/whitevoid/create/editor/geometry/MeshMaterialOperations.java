package whitevoid.create.editor.geometry;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import whitevoid.create.model.MeshMaterialAssignment;

/**
 * Transfers per-face materials across topology operations.
 * Geometry operations remain unaware of palette data; they only describe
 * where newly-created faces came from.
 */
public final class MeshMaterialOperations {
    private MeshMaterialOperations() {}

    public static MeshMaterialAssignment apply(
            MeshMaterialAssignment source,
            MeshOperations.OperationResult result) {
        if (source == null) return null;
        Objects.requireNonNull(result, "result");

        if (source.faceMaterials().size() != result.faceMapping().size()) {
            throw new IllegalArgumentException("Source material assignment does not match operation input");
        }

        List<Integer> materials = new ArrayList<>(result.mesh().faces().size());
        for (int i = 0; i < result.mesh().faces().size(); i++) materials.add(0);

        for (Map.Entry<Integer, Integer> mapping : result.faceMapping().entrySet()) {
            int oldFace = mapping.getKey();
            int newFace = mapping.getValue();
            materials.set(newFace, source.materialIndex(oldFace));
        }

        for (int newFace : result.createdFaces()) {
            Integer sourceFace = result.faceMaterialSources().get(newFace);
            if (sourceFace == null) {
                throw new IllegalStateException("No material source for created face " + newFace);
            }
            materials.set(newFace, source.materialIndex(sourceFace));
        }

        return source.withFaceMaterials(materials);
    }
}