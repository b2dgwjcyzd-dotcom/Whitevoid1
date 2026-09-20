package whitevoid.create.editor.selection;

import java.util.UUID;
import whitevoid.create.model.Model;
import whitevoid.create.model.ModelNode;

public final class SelectionController {
    private final SelectionSet selection = new SelectionSet();

    public void select(UUID id, SelectionMode mode) {
        if (mode == SelectionMode.SINGLE) selection.select(id);
        else if (mode == SelectionMode.ADDITIVE) selection.add(id);
        else selection.toggle(id);
    }

    public void select(ModelNode node, SelectionMode mode) {
        select(node.id(), mode);
    }

    public ModelNode first(Model model) {
        for (UUID id : selection.ids()) {
            for (ModelNode node : model.allNodes()) {
                if (node.id().equals(id)) return node;
            }
        }
        return null;
    }

    public void clear() { selection.clear(); }
    public SelectionSet selection() { return selection; }
}
