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
    private Command lastUndone;
    private Command lastRedone;
    private java.util.List<Command> savedUndo = java.util.List.of();
    private java.util.List<Command> savedRedo = java.util.List.of();

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
        lastUndone = null;
        lastRedone = null;
        trim();
        mutationListener.run();
    }

    public void recordExecuted(Command command) {
        Objects.requireNonNull(command, "command");
        undoStack.push(command);
        redoStack.clear();
        lastUndone = null;
        lastRedone = null;
        trim();
        mutationListener.run();
    }

    public boolean undo() {
        if (undoStack.isEmpty()) return false;
        Command command = undoStack.pop();
        command.undo();
        redoStack.push(command);
        lastUndone = command;
        lastRedone = null;
        mutationListener.run();
        return true;
    }

    public boolean redo() {
        if (redoStack.isEmpty()) return false;
        Command command = redoStack.pop();
        command.redo();
        undoStack.push(command);
        lastRedone = command;
        lastUndone = null;
        trim();
        mutationListener.run();
        return true;
    }

    public Command lastUndone() { return lastUndone; }
    public Command lastRedone() { return lastRedone; }

    public void clear() {
        undoStack.clear();
        redoStack.clear();
        lastUndone = null;
        lastRedone = null;
        savedUndo = java.util.List.of();
        savedRedo = java.util.List.of();
    }

    /** Captures the current history position as the persisted state. */
    public void markSaved() {
        savedUndo = java.util.List.copyOf(undoStack);
        savedRedo = java.util.List.copyOf(redoStack);
    }

    public boolean isAtSavedState() {
        return savedUndo.equals(java.util.List.copyOf(undoStack))
                && savedRedo.equals(java.util.List.copyOf(redoStack));
    }

    public boolean canUndo() { return !undoStack.isEmpty(); }
    public boolean canRedo() { return !redoStack.isEmpty(); }
    public int undoSize() { return undoStack.size(); }
    public int redoSize() { return redoStack.size(); }

    private void trim() {
        while (undoStack.size() > maxSize) undoStack.removeLast();
    }
}
