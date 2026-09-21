package whitevoid.create.project;

import java.util.Objects;
import whitevoid.create.model.Model;

public final class CreateProject {
    private ProjectMetadata metadata;
    private Model model;

    public CreateProject(ProjectMetadata metadata) {
        this(metadata, Model.withDefaultCube());
    }

    public CreateProject(ProjectMetadata metadata, Model model) {
        this.metadata = Objects.requireNonNull(metadata, "metadata");
        this.model = Objects.requireNonNull(model, "model");
    }

    public ProjectMetadata metadata() {
        return metadata;
    }

    public Model model() {
        return model;
    }

    public void setModel(Model model) {
        this.model = Objects.requireNonNull(model, "model");
        touch();
    }

    public void touch() {
        metadata = new ProjectMetadata(
                metadata.id(), metadata.name(), metadata.type(),
                metadata.createdAt(), java.time.Instant.now()
        );
    }
}
