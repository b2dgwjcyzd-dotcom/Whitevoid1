package whitevoid.create.core.history;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Objects;

/**
 * Owns undo/redo state for CREATE. GUI and tools submit commands here instead
 * of modifying project state while bypassing history.
 */
public final class CommandHistory {
    private final Deque<Command> undoStack = new ArrayDeque<>();
    private final Deque<Command> redoStack = new ArrayDeque<>();
    private final int maxSize;
    private final Runnable mutationListener;

    public CommandHistory() { this(256, () -> {}); }

    public CommandHistory(int maxSize) { this(maxSize, () -> {}); }

    public CommandHistory(Runnable mutationListener) { this(256, mutationListener); }

    public CommandHistory(int maxSize, Runnable mutationListener) {
        if (maxSize < 1) throw new IllegalArgumentException("maxSize must be positive");
        this.maxSize = maxSize;
        this.mutationListener = Objects.requireNonNull(mutationListener, "mutationListener");
    }

    public void execute(Command command) {
        Objects.requireNonNull(command, "command");
        command.execute();
        undoStack.push(command);
        redoStack.clear();
        trim();
        mutationListener.run();
    }

    /** Records a command whose state change has already been applied by an interactive editor operation. */
    public void recordExecuted(Command command) {
        Objects.requireNonNull(command, "command");
        undoStack.push(command);
        redoStack.clear();
        trim();
        mutationListener.run();
    }

    public boolean undo() {
        if (undoStack.isEmpty()) return false;
        Command command = undoStack.pop();
        command.undo();
        redoStack.push(command);
        mutationListener.run();
        return true;
    }

    public boolean redo() {
        if (redoStack.isEmpty()) return false;
        Command command = redoStack.pop();
        command.redo();
        undoStack.push(command);
        trim();
        mutationListener.run();
        return true;
    }

    public void clear() {
        undoStack.clear();
        redoStack.clear();
    }

    public boolean canUndo() { return !undoStack.isEmpty(); }
    public boolean canRedo() { return !redoStack.isEmpty(); }
    public int undoSize() { return undoStack.size(); }
    public int redoSize() { return redoStack.size(); }

    private void trim() {
        while (undoStack.size() > maxSize) undoStack.removeLast();
    }
}
