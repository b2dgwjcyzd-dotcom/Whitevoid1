package whitevoid.create.project;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public final class ProjectSerializer {
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final ModelSerializer modelSerializer = new ModelSerializer();

    public String serializeMetadata(ProjectMetadata metadata) {
        return gson.toJson(metadata);
    }

    public ProjectMetadata deserializeMetadata(String json) {
        ProjectMetadata metadata = gson.fromJson(json, ProjectMetadata.class);
        if (metadata == null) throw new IllegalArgumentException("Invalid project metadata");
        return metadata;
    }

    public String serializeModel(whitevoid.create.model.Model model) {
        return modelSerializer.serialize(model);
    }

    public whitevoid.create.model.Model deserializeModel(String json) {
        return modelSerializer.deserialize(json);
    }
}
