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
    private final Deque<Long> undoStateIds = new ArrayDeque<>();
    private final Deque<Long> redoStateIds = new ArrayDeque<>();
    private long nextStateId = 1L;
    private long currentStateId = 0L;
    private long savedStateId = 0L;

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
        undoStateIds.push(nextStateId++);
        redoStack.clear();
        redoStateIds.clear();
        lastUndone = null;
        lastRedone = null;
        currentStateId = undoStateIds.peek();
        trim();
        mutationListener.run();
    }

    public void recordExecuted(Command command) {
        Objects.requireNonNull(command, "command");
        undoStack.push(command);
        undoStateIds.push(nextStateId++);
        redoStack.clear();
        redoStateIds.clear();
        lastUndone = null;
        lastRedone = null;
        currentStateId = undoStateIds.peek();
        trim();
        mutationListener.run();
    }

    public boolean undo() {
        if (undoStack.isEmpty()) return false;
        Command command = undoStack.peek();
        command.undo();
        undoStack.pop();
        long stateId = undoStateIds.pop();
        redoStack.push(command);
        redoStateIds.push(stateId);
        lastUndone = command;
        lastRedone = null;
        currentStateId = undoStateIds.isEmpty() ? 0L : undoStateIds.peek();
        mutationListener.run();
        return true;
    }

    public boolean redo() {
        if (redoStack.isEmpty()) return false;
        Command command = redoStack.peek();
        command.redo();
        redoStack.pop();
        long stateId = redoStateIds.pop();
        undoStack.push(command);
        undoStateIds.push(stateId);
        lastRedone = command;
        lastUndone = null;
        currentStateId = stateId;
        trim();
        mutationListener.run();
        return true;
    }

    public Command lastUndone() { return lastUndone; }
    public Command lastRedone() { return lastRedone; }

    public void clear() {
        undoStack.clear();
        redoStack.clear();
        undoStateIds.clear();
        redoStateIds.clear();
        lastUndone = null;
        lastRedone = null;
        currentStateId = 0L;
        savedStateId = 0L;
    }

    /** Captures the current history position as the persisted state. */
    public void markSaved() {
        savedStateId = currentStateId;
    }

    public boolean isAtSavedState() {
        return currentStateId == savedStateId;
    }

    public boolean canUndo() { return !undoStack.isEmpty(); }
    public boolean canRedo() { return !redoStack.isEmpty(); }
    public int undoSize() { return undoStack.size(); }
    public int redoSize() { return redoStack.size(); }

    private void trim() {
        while (undoStack.size() > maxSize) {
            undoStack.removeLast();
            undoStateIds.removeLast();
        }
    }
}
