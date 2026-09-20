package whitevoid.create.core;

import whitevoid.create.core.registry.CreateRegistry;

/**
 * Entry point for the CREATE subsystem.
 *
 * CREATE is deliberately kept independent from screens and rendering. Those
 * layers will consume this core instead of owning project/editor state.
 */
public final class CreateCore {
    private final CreateRegistry registry;
    private final EditorContext editorContext;

    public CreateCore() {
        this.registry = new CreateRegistry();
        this.editorContext = new EditorContext();
    }

    public void initialize() {
        registry.initialize();
        editorContext.reset();
    }

    public CreateRegistry registry() {
        return registry;
    }

    public EditorContext editorContext() {
        return editorContext;
    }
}
