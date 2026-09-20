package whitevoid.create.core;

import whitevoid.create.core.history.CommandHistory;
import whitevoid.create.editor.viewport.ViewportContext;

/** Runtime state shared by CREATE editor systems. */
public final class EditorContext {
    private final CommandHistory history = new CommandHistory();
    private final ViewportContext viewport = new ViewportContext();
    private boolean editing;

    public void reset() {
        editing = false;
        history.clear();
        viewport.reset();
    }

    public boolean isEditing() { return editing; }
    public void setEditing(boolean editing) { this.editing = editing; }
    public CommandHistory history() { return history; }
    public ViewportContext viewport() { return viewport; }
}
