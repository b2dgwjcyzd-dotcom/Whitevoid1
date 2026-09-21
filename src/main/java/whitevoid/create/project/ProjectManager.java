package whitevoid.create.project;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

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
        if (project == null) {
            throw new IllegalArgumentException("Project cannot be null");
        }

        project.touch();
        ProjectMetadata metadata = project.metadata();
        Path directory = root.resolve(metadata.id().toString());
        Files.createDirectories(directory);
        Files.writeString(directory.resolve("project.json"), serializer.serializeMetadata(metadata));
        Files.writeString(directory.resolve("model.json"), serializer.serializeModel(project.model()));
    }

    public Optional<CreateProject> open(UUID id) throws IOException {
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

    public List<ProjectMetadata> listMetadata() throws IOException {
        if (!Files.exists(root)) {
            return List.of();
        }

        List<ProjectMetadata> projects = new ArrayList<>();
        try (var stream = Files.list(root)) {
            stream.filter(Files::isDirectory)
                    .map(directory -> directory.resolve("project.json"))
                    .filter(Files::exists)
                    .forEach(file -> {
                        try {
                            projects.add(serializer.deserializeMetadata(Files.readString(file)));
                        } catch (IOException | RuntimeException exception) {
                            throw new ProjectLoadException(file, exception);
                        }
                    });
        } catch (ProjectLoadException exception) {
            throw exception.causeAsIOException();
        }

        projects.sort(Comparator.comparing(ProjectMetadata::modifiedAt).reversed());
        return List.copyOf(projects);
    }

    public void setActiveProject(CreateProject project) {
        activeProject = project;
    }

    public Optional<CreateProject> activeProject() {
        return Optional.ofNullable(activeProject);
    }

    public Path root() { return root; }

    private static final class ProjectLoadException extends RuntimeException {
        private final Path file;

        private ProjectLoadException(Path file, Exception cause) {
            super("Unable to read CREATE project: " + file, cause);
            this.file = file;
        }

        private IOException causeAsIOException() {
            Throwable cause = getCause();
            if (cause instanceof IOException ioException) {
                return ioException;
            }
            IOException exception = new IOException("Unable to read CREATE project: " + file);
            exception.initCause(cause);
            return exception;
        }
    }
}
