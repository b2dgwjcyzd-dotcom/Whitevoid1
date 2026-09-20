package whitevoid.create.core;

import whitevoid.create.core.history.CommandHistory;
import whitevoid.create.editor.viewport.ViewportContext;
import whitevoid.create.model.Model;

/** Runtime state shared by CREATE editor systems. */
public final class EditorContext {
    private final CommandHistory history = new CommandHistory();
    private final ViewportContext viewport = new ViewportContext();
    private Model model = Model.withDefaultCube();
    private boolean editing;

    public void reset() {
        editing = false;
        history.clear();
        viewport.reset();
        model = Model.withDefaultCube();
    }

    public boolean isEditing() { return editing; }
    public void setEditing(boolean editing) { this.editing = editing; }
    public CommandHistory history() { return history; }
    public ViewportContext viewport() { return viewport; }
    public Model model() { return model; }
    public void setModel(Model model) { this.model = model; }
}
