package whitevoid.create.project;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record ProjectMetadata(
        UUID id,
        String name,
        ProjectType type,
        Instant createdAt,
        Instant modifiedAt
) {
    public ProjectMetadata {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(createdAt, "createdAt");
        Objects.requireNonNull(modifiedAt, "modifiedAt");
        if (name.isBlank()) throw new IllegalArgumentException("Project name cannot be blank");
    }

    public static ProjectMetadata create(String name, ProjectType type) {
        Instant now = Instant.now();
        return new ProjectMetadata(UUID.randomUUID(), name, type, now, now);
    }
}
