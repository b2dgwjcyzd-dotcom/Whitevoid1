package whitevoid.create.core.history.commands;

import java.util.function.Consumer;
import whitevoid.create.core.history.Command;

/** Small generic command useful for editor toggles and state flags. */
public final class SetBooleanCommand implements Command {
    private final String name;
    private final Consumer<Boolean> setter;
    private final boolean before;
    private final boolean after;

    public SetBooleanCommand(String name, Consumer<Boolean> setter, boolean before, boolean after) {
        this.name = name;
        this.setter = setter;
        this.before = before;
        this.after = after;
    }

    @Override public void execute() { setter.accept(after); }
    @Override public void undo() { setter.accept(before); }
    @Override public String name() { return name; }
}
