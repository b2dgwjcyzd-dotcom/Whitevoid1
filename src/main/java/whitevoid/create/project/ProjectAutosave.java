package whitevoid.create.project;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;

public final class ProjectAutosave {
    private final ProjectManager manager;
    private final Duration interval;
    private Instant lastSave = Instant.MIN;

    public ProjectAutosave(ProjectManager manager, Duration interval) {
        this.manager = manager;
        this.interval = interval;
    }

    public void tick() {
        manager.activeProject().ifPresent(project -> {
            if (Duration.between(lastSave, Instant.now()).compareTo(interval) < 0) return;
            try {
                manager.save(project);
                lastSave = Instant.now();
            } catch (IOException exception) {
                throw new IllegalStateException("CREATE autosave failed", exception);
            }
        });
    }
}
