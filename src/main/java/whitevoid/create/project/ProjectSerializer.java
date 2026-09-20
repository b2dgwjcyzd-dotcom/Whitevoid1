package whitevoid.create.project;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public final class ProjectSerializer {
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public String serializeMetadata(ProjectMetadata metadata) {
        return gson.toJson(metadata);
    }

    public ProjectMetadata deserializeMetadata(String json) {
        ProjectMetadata metadata = gson.fromJson(json, ProjectMetadata.class);
        if (metadata == null) throw new IllegalArgumentException("Invalid project metadata");
        return metadata;
    }
}
