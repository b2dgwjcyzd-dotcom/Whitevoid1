package whitevoid.create.project;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import whitevoid.create.model.Model;

/**
 * Persistence boundary for CREATE project metadata and model data.
 *
 * The format version is stored in each persisted JSON document so future
 * schema changes can be migrated without coupling runtime model classes
 * to the file format.
 */
public final class ProjectSerializer {
    public static final int CURRENT_FORMAT_VERSION = 1;

    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final ModelSerializer modelSerializer = new ModelSerializer();

    public String serializeMetadata(ProjectMetadata metadata) {
        JsonObject document = new JsonObject();
        document.addProperty("formatVersion", CURRENT_FORMAT_VERSION);
        document.add("metadata", gson.toJsonTree(metadata));
        return gson.toJson(document);
    }

    public ProjectMetadata deserializeMetadata(String json) {
        JsonObject document = parseDocument(json);
        int version = readVersion(document);
        if (version > CURRENT_FORMAT_VERSION) {
            throw new IllegalArgumentException(
                    "Unsupported CREATE project format version: " + version);
        }

        ProjectMetadata metadata;
        if (document.has("metadata")) {
            metadata = gson.fromJson(document.get("metadata"), ProjectMetadata.class);
        } else {
            // Compatibility with the original unversioned metadata format.
            metadata = gson.fromJson(document, ProjectMetadata.class);
        }

        if (metadata == null) {
            throw new IllegalArgumentException("Invalid project metadata");
        }
        return metadata;
    }

    public String serializeModel(Model model) {
        JsonObject document = new JsonObject();
        document.addProperty("formatVersion", CURRENT_FORMAT_VERSION);
        document.add("model", JsonParser.parseString(modelSerializer.serialize(model)));
        return gson.toJson(document);
    }

    public Model deserializeModel(String json) {
        JsonObject document = parseDocument(json);
        int version = readVersion(document);
        if (version > CURRENT_FORMAT_VERSION) {
            throw new IllegalArgumentException(
                    "Unsupported CREATE model format version: " + version);
        }

        if (document.has("model")) {
            return modelSerializer.deserialize(
                    gson.toJson(document.get("model")));
        }

        // Compatibility with the original unversioned model format.
        return modelSerializer.deserialize(json);
    }

    private JsonObject parseDocument(String json) {
        if (json == null || json.isBlank()) {
            throw new IllegalArgumentException("CREATE project document cannot be blank");
        }

        var element = JsonParser.parseString(json);
        if (!element.isJsonObject()) {
            throw new IllegalArgumentException("CREATE project document must be a JSON object");
        }
        return element.getAsJsonObject();
    }

    private int readVersion(JsonObject document) {
        if (!document.has("formatVersion")) {
            return 0;
        }

        try {
            return document.get("formatVersion").getAsInt();
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException("Invalid CREATE project format version", exception);
        }
    }
}
