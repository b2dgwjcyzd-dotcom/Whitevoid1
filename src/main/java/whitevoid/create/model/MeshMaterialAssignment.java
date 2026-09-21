package whitevoid.create.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Material assignments for a mesh.
 *
 * <p>Geometry stays purely geometric: this object owns the palette and the
 * per-face palette indices. Topology code can therefore replace a mesh
 * without carrying rendering concerns into MeshGeometry.</p>
 */
public final class MeshMaterialAssignment {
    private final MaterialPalette palette;
    private final List<Integer> faceMaterials;

    public MeshMaterialAssignment(MaterialPalette palette, List<Integer> faceMaterials) {
        this.palette = Objects.requireNonNull(palette, "palette").copy();
        Objects.requireNonNull(faceMaterials, "faceMaterials");

        List<Integer> copy = new ArrayList<>(faceMaterials.size());
        for (Integer index : faceMaterials) {
            if (index == null) {
                throw new IllegalArgumentException("Face material index cannot be null");
            }
            validateIndex(this.palette, index);
            copy.add(index);
        }
        this.faceMaterials = List.copyOf(copy);
    }

    public static MeshMaterialAssignment uniform(int faceCount, MaterialColor color) {
        if (faceCount < 0) {
            throw new IllegalArgumentException("faceCount cannot be negative");
        }
        MaterialPalette palette = new MaterialPalette();
        palette.set(0, Objects.requireNonNull(color, "color"));
        return new MeshMaterialAssignment(
                palette,
                java.util.Collections.nCopies(faceCount, 0));
    }

    public MaterialPalette palette() {
        return palette.copy();
    }

    public List<Integer> faceMaterials() {
        return faceMaterials;
    }

    public int materialIndex(int faceIndex) {
        return faceMaterials.get(faceIndex);
    }

    public MaterialColor materialColor(int faceIndex) {
        return palette.color(materialIndex(faceIndex));
    }

    public MeshMaterialAssignment copy() {
        return new MeshMaterialAssignment(palette, faceMaterials);
    }

    public MeshMaterialAssignment withFaceMaterial(int faceIndex, int paletteIndex) {
        if (faceIndex < 0 || faceIndex >= faceMaterials.size()) {
            throw new IndexOutOfBoundsException("faceIndex=" + faceIndex);
        }
        validateIndex(palette, paletteIndex);

        List<Integer> updated = new ArrayList<>(faceMaterials);
        updated.set(faceIndex, paletteIndex);
        return new MeshMaterialAssignment(palette, updated);
    }

    public MeshMaterialAssignment withFaceMaterials(List<Integer> newFaceMaterials) {
        return new MeshMaterialAssignment(palette, newFaceMaterials);
    }

    public MeshMaterialAssignment withPalette(MaterialPalette newPalette) {
        return new MeshMaterialAssignment(newPalette, faceMaterials);
    }

    private static void validateIndex(MaterialPalette palette, int index) {
        if (index < 0 || index >= palette.colors().size()) {
            throw new IllegalArgumentException(
                    "Material index " + index + " is outside the palette");
        }
    }
}
