package whitevoid.create.editor.selection;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import whitevoid.create.model.Model;
import whitevoid.create.model.ModelNode;

public final class SelectionController {
    private final SelectionSet selection = new SelectionSet();

    public void select(UUID id, SelectionMode mode) {
        if (mode == null) throw new NullPointerException("mode");
        if (mode == SelectionMode.SINGLE) selection.select(id);
        else if (mode == SelectionMode.ADDITIVE) selection.add(id);
        else selection.toggle(id);
    }

    public void select(ModelNode node, SelectionMode mode) {
        if (node == null) throw new NullPointerException("node");
        select(node.id(), mode);
    }

    public ModelNode first(Model model) {
        if (model == null) return null;

        Map<UUID, ModelNode> liveNodes = new HashMap<>();
        for (ModelNode node : model.allNodes()) {
            liveNodes.put(node.id(), node);
        }

        selection.ids().stream()
                .filter(id -> !liveNodes.containsKey(id))
                .toList()
                .forEach(selection::remove);

        for (UUID id : selection.ids()) {
            ModelNode node = liveNodes.get(id);
            if (node != null) return node;
        }

        return null;
    }

    public void clear() { selection.clear(); }
    public SelectionSet selection() { return selection; }
}
