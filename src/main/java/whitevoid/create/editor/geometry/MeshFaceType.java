package whitevoid.create.editor.geometry;

public enum MeshFaceType {
    NONE,
    POS_X,
    NEG_X,
    POS_Y,
    NEG_Y,
    POS_Z,
    NEG_Z;

    public static MeshFaceType fromIndex(int index) {
        return switch (index) {
            case 0 -> POS_X;
            case 1 -> NEG_X;
            case 2 -> POS_Y;
            case 3 -> NEG_Y;
            case 4 -> POS_Z;
            case 5 -> NEG_Z;
            default -> NONE;
        };
    }
}
