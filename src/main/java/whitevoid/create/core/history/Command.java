package whitevoid.create.core.history;

/**
 * A reversible CREATE editor operation.
 *
 * <p>Implementations must be atomic: if execute(), undo(), or redo() throws,
 * the edited model must remain in the state it had before that call.</p>
 */
public interface Command {
    void execute();
    void undo();
    default void redo() { execute(); }
    String name();
}
