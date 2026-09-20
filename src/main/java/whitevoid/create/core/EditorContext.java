package whitevoid.create.core;

import whitevoid.create.core.history.CommandHistory;

/** Runtime state shared by CREATE editor systems. */
public final class EditorContext {
    private final CommandHistory history = new CommandHistory();
    private boolean editing;

    public void reset() {
        editing = false;
        history.clear();
    }

    public boolean isEditing() { return editing; }
    public void setEditing(boolean editing) { this.editing = editing; }
    public CommandHistory history() { return history; }
}
