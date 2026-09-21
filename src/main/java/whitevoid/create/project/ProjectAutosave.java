package whitevoid.create.project;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;

public final class ProjectAutosave {
    private final ProjectManager manager;
    private final Duration interval;
    private final Runnable afterSave;
    private Instant lastSave = Instant.MIN;

    public ProjectAutosave(ProjectManager manager, Duration interval) {
        this(manager, interval, () -> {});
    }

    public ProjectAutosave(ProjectManager manager, Duration interval, Runnable afterSave) {
        if (manager == null) throw new IllegalArgumentException("Project manager cannot be null");
        if (interval == null || interval.isZero() || interval.isNegative()) {
            throw new IllegalArgumentException("Autosave interval must be positive");
        }
        this.manager = manager;
        this.interval = interval;
        this.afterSave = java.util.Objects.requireNonNull(afterSave, "afterSave");
    }

    public void tick() {
        manager.activeProject().ifPresent(project -> {
            if (!project.dirty()) return;

            Instant now = Instant.now();
            if (Duration.between(lastSave, now).compareTo(interval) < 0) return;

            try {
                manager.save(project);
                lastSave = now;
                afterSave.run();
            } catch (IOException exception) {
                throw new IllegalStateException("CREATE autosave failed", exception);
            }
        });
    }

    public void reset() {
        lastSave = Instant.MIN;
    }
}
