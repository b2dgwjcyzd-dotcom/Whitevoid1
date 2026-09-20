package whitevoid.create.editor.selection;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

public final class SelectionSet {
    private final Set<UUID> selected = new LinkedHashSet<>();

    public void select(UUID id) {
        selected.clear();
        selected.add(id);
    }

    public void add(UUID id) { selected.add(id); }
    public void remove(UUID id) { selected.remove(id); }
    public void toggle(UUID id) { if (!selected.remove(id)) selected.add(id); }
    public void clear() { selected.clear(); }
    public boolean contains(UUID id) { return selected.contains(id); }
    public boolean isEmpty() { return selected.isEmpty(); }
    public int size() { return selected.size(); }
    public Set<UUID> ids() { return Collections.unmodifiableSet(selected); }
}
