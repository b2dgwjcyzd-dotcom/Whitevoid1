package whitevoid.create.core.history.commands;

import java.util.UUID;
import whitevoid.create.core.history.Command;
import whitevoid.create.core.history.SelectionHistoryCommand;
import whitevoid.create.model.Model;
import whitevoid.create.model.ModelNode;

public final class DeleteNodeCommand implements Command, SelectionHistoryCommand {
    private final Model model;
    private final ModelNode node;
    private ModelNode parent;
    private int index = -1;

    public DeleteNodeCommand(Model model, ModelNode node) {
        this.model = model;
        this.node = node;
    }

    @Override public void execute() {
        parent = node.parent();
        if (parent == null) throw new IllegalStateException("Cannot delete the model root");
        index = parent.indexOfChild(node);
        if (index < 0) throw new IllegalStateException("Node is not attached to its parent");
        if (node.parent() != parent) throw new IllegalStateException("Node changed parent outside this command");
        parent.removeChild(node);
    }

    @Override public void undo() {
        if (parent == null) throw new IllegalStateException("Delete command has not been executed");
        if (node.parent() != null) throw new IllegalStateException("Deleted node is already attached");
        parent.addChild(Math.min(index, parent.children().size()), node);
    }

    @Override public String name() { return "Delete Node"; }

    @Override public UUID selectionAfterUndo() { return node.id(); }

    @Override public UUID selectionAfterRedo() { return null; }

    public ModelNode deletedNode() { return node; }
}
