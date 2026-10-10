package io.ohmvir.plugins.jenkinsaisynapse.api.message;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.Objects;

/**
 * A request from the model to call a tool. It is answered by a {@link ToolResultPart} with the same id in the
 * {@link Role#TOOL} message that follows.
 */
public final class ToolCallPart implements ContentPart {
    private final String id;
    private final String name;
    // Stored as JSON text so the part is immutable and serializes cleanly.
    private final String argumentsJson;

    public ToolCallPart(String id, String name, JsonObject arguments) {
        this.id = Objects.requireNonNull(id, "id");
        this.name = Objects.requireNonNull(name, "name");
        this.argumentsJson = Objects.requireNonNull(arguments, "arguments").toString();
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    /**
     * @return A copy of the arguments the model passed.
     */
    public JsonObject getArguments() {
        return JsonParser.parseString(argumentsJson).getAsJsonObject();
    }

    public String getArgumentsJson() {
        return argumentsJson;
    }

    @Override
    public String toString() {
        return "ToolCallPart[" + id + ", " + name + ", " + argumentsJson + "]";
    }
}
