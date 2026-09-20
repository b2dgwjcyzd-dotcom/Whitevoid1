package whitevoid.create.core;

import java.io.IOException;
import java.nio.file.Path;
import net.fabricmc.loader.api.FabricLoader;
import whitevoid.create.core.registry.CreateRegistry;
import whitevoid.create.project.ProjectManager;

/** Entry point for the CREATE subsystem. */
public final class CreateCore {
    private final CreateRegistry registry;
    private final EditorContext editorContext;
    private final ProjectManager projectManager;

    public CreateCore() {
        this.registry = new CreateRegistry();
        this.editorContext = new EditorContext();
        Path projects = FabricLoader.getInstance().getConfigDir().resolve("whitevoid").resolve("create").resolve("projects");
        this.projectManager = new ProjectManager(projects);
    }

    public void initialize() {
        registry.initialize();
        editorContext.reset();
        try {
            projectManager.initialize();
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to initialize CREATE project storage", exception);
        }
    }

    public CreateRegistry registry() { return registry; }
    public EditorContext editorContext() { return editorContext; }
    public ProjectManager projectManager() { return projectManager; }
}
