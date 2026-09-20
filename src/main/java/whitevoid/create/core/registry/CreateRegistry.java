package whitevoid.create.core.registry;

/**
 * Registration point for CREATE project types, tools and future extensions.
 */
public final class CreateRegistry {
    private boolean initialized;

    public void initialize() {
        if (initialized) {
            return;
        }

        // Project types and editor tools will be registered in later phases.
        initialized = true;
    }

    public boolean isInitialized() {
        return initialized;
    }
}
