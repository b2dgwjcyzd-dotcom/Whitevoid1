package whitevoid.create.model;

/**
 * Immutable sRGB color used by CREATE materials.
 *
 * <p>The value is stored as ARGB, matching Minecraft's GUI color convention.
 * Alpha is kept explicitly so later material and texture systems can share
 * the same representation.</p>
 */
public record MaterialColor(int argb) {
    public static final MaterialColor WHITE = new MaterialColor(0xFFFFFFFF);
    public static final MaterialColor BLACK = new MaterialColor(0xFF000000);

    public int alpha() { return (argb >>> 24) & 0xFF; }
    public int red() { return (argb >>> 16) & 0xFF; }
    public int green() { return (argb >>> 8) & 0xFF; }
    public int blue() { return argb & 0xFF; }

    public MaterialColor withAlpha(int alpha) {
        requireChannel(alpha, "alpha");
        return new MaterialColor((alpha << 24) | (argb & 0x00FFFFFF));
    }

    public static MaterialColor rgba(int red, int green, int blue, int alpha) {
        requireChannel(red, "red");
        requireChannel(green, "green");
        requireChannel(blue, "blue");
        requireChannel(alpha, "alpha");
        return new MaterialColor(
                (alpha << 24) | (red << 16) | (green << 8) | blue);
    }

    private static void requireChannel(int value, String name) {
        if (value < 0 || value > 255) {
            throw new IllegalArgumentException(name + " must be between 0 and 255");
        }
    }
}
