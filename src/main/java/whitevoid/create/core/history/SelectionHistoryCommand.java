package whitevoid.create.core.history;

import java.util.UUID;

/** Optional command contract for restoring object selection around undo/redo. */
public interface SelectionHistoryCommand {
    UUID selectionAfterUndo();
    UUID selectionAfterRedo();
}
