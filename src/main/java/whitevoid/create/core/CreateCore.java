package whitevoid.create.core;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;
import whitevoid.create.project.CreateProject;
import whitevoid.create.project.ProjectType;
import net.fabricmc.loader.api.FabricLoader;
import whitevoid.create.core.registry.CreateRegistry;
import whitevoid.create.project.ProjectManager;
import whitevoid.create.project.ProjectAutosave;
import java.time.Duration;

/** Entry point for the CREATE subsystem. */
public final class CreateCore {
    private final CreateRegistry registry;
    private final EditorContext editorContext;
    private final ProjectManager projectManager;
    private final ProjectAutosave autosave;

    public CreateCore() {
        this.registry = new CreateRegistry();
        this.editorContext = new EditorContext(this::markProjectDirty);
        Path projects = FabricLoader.getInstance().getConfigDir().resolve("whitevoid").resolve("create").resolve("projects");
        this.projectManager = new ProjectManager(projects);
        this.autosave = new ProjectAutosave(projectManager, Duration.ofSeconds(30), editorContext.history()::markSaved);
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
            editorContext.history().markSaved();
            project.markClean();
            return project;
        }

        try {
            CreateProject created = projectManager.create("Untitled", ProjectType.MODEL);
            editorContext.setModel(created.model());
            editorContext.history().markSaved();
            created.markClean();
            return created;
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to create default CREATE project", exception);
        }
    }

    public Optional<CreateProject> openProject(java.util.UUID id) {
        try {
            Optional<CreateProject> opened = projectManager.open(id);
            opened.ifPresent(project -> {
                editorContext.setModel(project.model());
                editorContext.history().markSaved();
                project.markClean();
            });
            return opened;
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to open CREATE project", exception);
        }
    }

    public void markProjectDirty() {
        projectManager.activeProject().ifPresent(project -> {
            if (editorContext.history().isAtSavedState()) project.markClean();
            else project.markDirty();
        });
    }

    public void tickAutosave() {
        autosave.tick();
    }

    public void saveActiveProject() {
        CreateProject project = projectManager.activeProject()
                .orElseThrow(() -> new IllegalStateException("No active CREATE project"));
        if (project.model() != editorContext.model()) {
            project.setModel(editorContext.model());
        }
        try {
            projectManager.save(project);
            editorContext.history().markSaved();
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to save CREATE project", exception);
        }
    }

    public CreateRegistry registry() { return registry; }
    public EditorContext editorContext() { return editorContext; }
    public ProjectManager projectManager() { return projectManager; }
    public ProjectAutosave autosave() { return autosave; }

}
