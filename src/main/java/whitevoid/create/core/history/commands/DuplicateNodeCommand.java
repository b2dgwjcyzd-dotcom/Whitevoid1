package whitevoid.create.core.history.commands;

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
        this.model = model;
        this.source = source;
    }

    @Override public void execute() {
        if (parent == null) {
            parent = source.parent();
            if (parent == null) throw new IllegalStateException("Cannot duplicate the model root");
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
        }

        if (index < 0) index = parent.indexOfChild(source) + 1;
        parent.addChild(Math.min(index, parent.children().size()), duplicate);
    }

    @Override public void undo() {
        if (parent == null || duplicate == null)
            throw new IllegalStateException("Duplicate command has not been executed");
        parent.removeChild(duplicate);
    }

    @Override public String name() { return "Duplicate Node"; }

    @Override public UUID selectionAfterUndo() { return source.id(); }
    @Override public UUID selectionAfterRedo() { return duplicate.id(); }

    public ModelNode duplicatedNode() { return duplicate; }
}
