package whitevoid.create.core.history.commands;

import java.util.Objects;
import whitevoid.create.core.history.Command;
import whitevoid.create.model.ModelNode;

/**
 * Reparents an existing node while preserving its object identity and child tree.
 * The operation is intentionally a history command so hierarchy changes are atomic.
 */
public final class ReparentNodeCommand implements Command {
    private final ModelNode node;
    private final ModelNode newParent;
    private final int requestedIndex;

    private ModelNode oldParent;
    private int oldIndex = -1;
    private int appliedIndex = -1;

    public ReparentNodeCommand(ModelNode node, ModelNode newParent, int index) {
        this.node = Objects.requireNonNull(node, "node");
        this.newParent = Objects.requireNonNull(newParent, "newParent");
        if (index < 0) throw new IllegalArgumentException("index must be non-negative");
        this.requestedIndex = index;
    }

    @Override
    public void execute() {
        validateTarget();

        if (oldParent == null) {
            oldParent = node.parent();
            if (oldParent == null) {
                throw new IllegalStateException("Cannot reparent a detached or root node");
            }
            oldIndex = oldParent.indexOfChild(node);
            if (oldIndex < 0) {
                throw new IllegalStateException("Node is not attached to its parent");
            }
        }

        ModelNode currentParent = node.parent();
        if (currentParent != null) {
            currentParent.removeChild(node);
        }

        int targetIndex = requestedIndex;
        if (currentParent == newParent && oldIndex < requestedIndex) {
            targetIndex--;
        }

        appliedIndex = Math.max(0, Math.min(targetIndex, newParent.children().size()));
        newParent.addChild(appliedIndex, node);
    }

    @Override
    public void undo() {
        if (oldParent == null) {
            throw new IllegalStateException("Reparent command has not been executed");
        }

        if (node.parent() != null) {
            node.parent().removeChild(node);
        }

        int index = Math.max(0, Math.min(oldIndex, oldParent.children().size()));
        oldParent.addChild(index, node);
    }

    @Override
    public String name() {
        return "Reparent Node";
    }

    public ModelNode node() {
        return node;
    }

    public ModelNode newParent() {
        return newParent;
    }

    public int appliedIndex() {
        return appliedIndex;
    }

    private void validateTarget() {
        if (node == newParent) {
            throw new IllegalArgumentException("A node cannot parent itself");
        }

        for (ModelNode cursor = newParent; cursor != null; cursor = cursor.parent()) {
            if (cursor == node) {
                throw new IllegalArgumentException("Cannot reparent a node into its own subtree");
            }
        }
    }
}
