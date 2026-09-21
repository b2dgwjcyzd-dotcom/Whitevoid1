package whitevoid.create.core;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;
import whitevoid.create.project.CreateProject;
import whitevoid.create.project.ProjectType;
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

    public CreateProject ensureActiveProject() {
        Optional<CreateProject> active = projectManager.activeProject();
        if (active.isPresent()) {
            CreateProject project = active.get();
            editorContext.setModel(project.model());
            return project;
        }

        try {
            CreateProject created = projectManager.create("Untitled", ProjectType.COSMETIC);
            editorContext.setModel(created.model());
            return created;
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to create default CREATE project", exception);
        }
    }

    public void saveActiveProject() {
        CreateProject project = projectManager.activeProject()
                .orElseThrow(() -> new IllegalStateException("No active CREATE project"));
        project.setModel(editorContext.model());
        try {
            projectManager.save(project);
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to save CREATE project", exception);
        }
    }

    public CreateRegistry registry() { return registry; }
    public EditorContext editorContext() { return editorContext; }
    public ProjectManager projectManager() { return projectManager; }

}
