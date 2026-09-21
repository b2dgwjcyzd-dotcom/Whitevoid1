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

        var liveIds = model.allNodes().stream()
                .map(ModelNode::id)
                .collect(java.util.stream.Collectors.toSet());

        // Structural undo/redo can detach nodes while their UUIDs remain selected.
        // Always purge stale IDs before resolving the first live selection.
        selection.ids().stream()
                .filter(id -> !liveIds.contains(id))
                .toList()
                .forEach(selection::remove);

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
