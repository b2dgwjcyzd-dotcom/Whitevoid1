package whitevoid.create.core.history;

/** A reversible CREATE editor operation. */
public interface Command {
    void execute();
    void undo();
    default void redo() { execute(); }
    String name();
}
