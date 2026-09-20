package whitevoid.create.core;

/**
 * Runtime state shared by CREATE editor systems.
 *
 * This class intentionally contains only state that belongs to the editor
 * context. Rendering, GUI widgets, project serialization and model logic are
 * separate systems and must not be placed here.
 */
public final class EditorContext {
    private boolean editing;

    public void reset() {
        editing = false;
    }

    public boolean isEditing() {
        return editing;
    }

    public void setEditing(boolean editing) {
        this.editing = editing;
    }
}
