package whitevoid.create.project;

import java.time.Instant;
import java.util.Objects;
import whitevoid.create.model.Model;

public final class CreateProject {
    private ProjectMetadata metadata;
    private Model model;
    private boolean dirty;

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

    public boolean dirty() {
        return dirty;
    }

    public void setModel(Model model) {
        this.model = Objects.requireNonNull(model, "model");
        markDirty();
    }

    public void markDirty() {
        dirty = true;
        touch();
    }

    public void markSaved() {
        dirty = false;
    }

    public void markClean() {
        dirty = false;
    }

    public void touch() {
        metadata = new ProjectMetadata(
                metadata.id(), metadata.name(), metadata.type(),
                metadata.createdAt(), Instant.now()
        );
    }
}
