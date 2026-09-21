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
        if (model == null) return null;

        for (UUID id : selection.ids()) {
            for (ModelNode node : model.allNodes()) {
                if (node.id().equals(id)) return node;
            }
        }

        // Undoing a structural command can detach the selected node while
        // keeping its UUID in the selection set. Purge such stale IDs.
        var liveIds = model.allNodes().stream()
                .map(ModelNode::id)
                .collect(java.util.stream.Collectors.toSet());
        selection.ids().stream()
                .filter(id -> !liveIds.contains(id))
                .toList()
                .forEach(selection::remove);

        return null;
    }
    public void clear() { selection.clear(); }
    public SelectionSet selection() { return selection; }
}
