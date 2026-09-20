package whitevoid.create.editor.selection;

import java.util.UUID;

public final class SelectionController {
    private final SelectionSet selection = new SelectionSet();

    public void select(UUID id, SelectionMode mode) {
        if (mode == SelectionMode.SINGLE) selection.select(id);
        else if (mode == SelectionMode.ADDITIVE) selection.add(id);
        else selection.toggle(id);
    }

    public void clear() { selection.clear(); }
    public SelectionSet selection() { return selection; }
}
