package whitevoid.create.project;

import java.nio.file.Files;
import java.nio.file.Path;
import java.io.IOException;
import java.util.Optional;

public final class ProjectManager {
    private final Path root;
    private final ProjectSerializer serializer;
    private CreateProject activeProject;

    public ProjectManager(Path root) {
        this.root = root;
        this.serializer = new ProjectSerializer();
    }

    public void initialize() throws IOException {
        Files.createDirectories(root);
    }

    public CreateProject create(String name, ProjectType type) throws IOException {
        CreateProject project = new CreateProject(ProjectMetadata.create(name, type));
        save(project);
        activeProject = project;
        return project;
    }

    public void save(CreateProject project) throws IOException {
        project.touch();
        ProjectMetadata metadata = project.metadata();
        Path directory = root.resolve(metadata.id().toString());
        Files.createDirectories(directory);
        Files.writeString(directory.resolve("project.json"), serializer.serializeMetadata(metadata));
        Files.writeString(directory.resolve("model.json"), serializer.serializeModel(project.model()));
    }

    public Optional<CreateProject> open(java.util.UUID id) throws IOException {
        Path file = root.resolve(id.toString()).resolve("project.json");
        if (!Files.exists(file)) return Optional.empty();
        ProjectMetadata metadata = serializer.deserializeMetadata(Files.readString(file));
        Path modelFile = file.getParent().resolve("model.json");
        CreateProject project;
        if (Files.exists(modelFile)) {
            project = new CreateProject(
                    metadata,
                    serializer.deserializeModel(Files.readString(modelFile)));
        } else {
            // Projects created before model persistence receive the current default model.
            project = new CreateProject(metadata);
        }
        activeProject = project;
        return Optional.of(project);
    }

    public void setActiveProject(CreateProject project) {
        activeProject = project;
    }

    public Optional<CreateProject> activeProject() {
        return Optional.ofNullable(activeProject);
    }

    public Path root() { return root; }
}
