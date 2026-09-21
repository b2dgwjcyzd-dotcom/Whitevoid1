package whitevoid.create.core.history.commands;

import java.util.Objects;
import java.util.UUID;
import whitevoid.create.core.history.Command;
import whitevoid.create.core.history.SelectionHistoryCommand;
import whitevoid.create.model.Model;
import whitevoid.create.model.ModelNode;
import whitevoid.create.model.ModelNodeCloner;

public final class DuplicateNodeCommand implements Command, SelectionHistoryCommand {
    private final Model model;
    private final ModelNode source;
    private ModelNode duplicate;
    private ModelNode parent;
    private int index = -1;

    public DuplicateNodeCommand(Model model, ModelNode source) {
        this.model = Objects.requireNonNull(model, "model");
        this.source = Objects.requireNonNull(source, "source");
        if (!model.allNodes().contains(source)) {
            throw new IllegalArgumentException("Source node does not belong to the model");
        }
    }

    @Override public void execute() {
        if (parent == null) {
            parent = source.parent();
            if (parent == null) throw new IllegalStateException("Cannot duplicate the model root");
            if (parent.indexOfChild(source) < 0) throw new IllegalStateException("Source node is not attached to its parent");
        } else if (source.parent() != parent) {
            throw new IllegalStateException("Duplicate source changed parent");
        }

        if (duplicate == null) {
            duplicate = ModelNodeCloner.deepCopy(source);
            duplicate.setName(source.name() + " Copy");
            duplicate.transform().position(
                    source.transform().x() + 1.0,
                    source.transform().y(),
                    source.transform().z());
        } else if (duplicate.parent() != null && duplicate.parent() != parent) {
            throw new IllegalStateException("Duplicate node was reparented outside this command");
        }

        if (index < 0) {
            int sourceIndex = parent.indexOfChild(source);
            if (sourceIndex < 0) throw new IllegalStateException("Source node is not attached to its parent");
            index = sourceIndex + 1;
        }
        if (duplicate.parent() != null) throw new IllegalStateException("Duplicate node is already attached");
        parent.addChild(Math.min(index, parent.children().size()), duplicate);
    }

    @Override public void undo() {
        if (parent == null || duplicate == null)
            throw new IllegalStateException("Duplicate command has not been executed");
        if (duplicate.parent() != parent)
            throw new IllegalStateException("Duplicate node changed parent outside this command");
        parent.removeChild(duplicate);
    }

    @Override public String name() { return "Duplicate Node"; }

    @Override public UUID selectionAfterUndo() { return source.id(); }
    @Override public UUID selectionAfterRedo() { return duplicate.id(); }

    public ModelNode duplicatedNode() { return duplicate; }
}
