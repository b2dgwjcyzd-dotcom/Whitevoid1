package whitevoid.create.core.history;

import java.util.ArrayList;
import java.util.List;

/** Groups several editor operations into one undoable action. */
public final class CommandGroup implements Command {
    private final String name;
    private final List<Command> commands = new ArrayList<>();

    public CommandGroup(String name) { this.name = name; }

    public CommandGroup add(Command command) {
        commands.add(command);
        return this;
    }

    @Override
    public void execute() {
        for (Command command : commands) command.execute();
    }

    @Override
    public void undo() {
        for (int i = commands.size() - 1; i >= 0; i--) commands.get(i).undo();
    }

    @Override
    public String name() { return name; }

    public boolean isEmpty() { return commands.isEmpty(); }
}
