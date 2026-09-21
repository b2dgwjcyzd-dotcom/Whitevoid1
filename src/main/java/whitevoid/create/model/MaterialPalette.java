package whitevoid.create.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Small ordered material palette for CREATE assets.
 *
 * <p>This is intentionally independent from mesh topology. Face/voxel
 * assignments will be introduced separately so topology operations do not
 * become coupled to the material system.</p>
 */
public final class MaterialPalette {
    private final List<MaterialColor> colors = new ArrayList<>();

    public MaterialPalette() {
        colors.add(MaterialColor.WHITE);
    }

    public MaterialPalette(List<MaterialColor> colors) {
        Objects.requireNonNull(colors, "colors");
        if (colors.isEmpty()) throw new IllegalArgumentException("Palette cannot be empty");
        for (MaterialColor color : colors) {
            Objects.requireNonNull(color, "color");
        }
        this.colors.addAll(colors);
    }

    public List<MaterialColor> colors() {
        return List.copyOf(colors);
    }

    public MaterialColor color(int index) {
        return colors.get(index);
    }

    public int add(MaterialColor color) {
        colors.add(Objects.requireNonNull(color, "color"));
        return colors.size() - 1;
    }

    public void set(int index, MaterialColor color) {
        colors.set(index, Objects.requireNonNull(color, "color"));
    }

    public MaterialColor remove(int index) {
        if (colors.size() == 1) {
            throw new IllegalStateException("Palette must keep at least one material");
        }
        return colors.remove(index);
    }

    public MaterialPalette copy() {
        return new MaterialPalette(colors);
    }
}
