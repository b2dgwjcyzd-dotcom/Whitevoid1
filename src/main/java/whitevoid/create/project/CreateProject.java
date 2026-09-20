package whitevoid.create.project;

import java.util.Objects;

public final class CreateProject {
    private ProjectMetadata metadata;

    public CreateProject(ProjectMetadata metadata) {
        this.metadata = Objects.requireNonNull(metadata, "metadata");
    }

    public ProjectMetadata metadata() {
        return metadata;
    }

    public void touch() {
        metadata = new ProjectMetadata(
                metadata.id(), metadata.name(), metadata.type(),
                metadata.createdAt(), java.time.Instant.now()
        );
    }
}
