package whitevoid.create.editor.geometry;

public enum GeometryFace {
    NONE,
    POS_X,
    NEG_X,
    POS_Y,
    NEG_Y,
    POS_Z,
    NEG_Z;

    public GeometryFace opposite() {
        return switch (this) {
            case POS_X -> NEG_X;
            case NEG_X -> POS_X;
            case POS_Y -> NEG_Y;
            case NEG_Y -> POS_Y;
            case POS_Z -> NEG_Z;
            case NEG_Z -> POS_Z;
            case NONE -> NONE;
        };
    }
}
