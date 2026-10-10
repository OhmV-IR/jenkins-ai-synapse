package io.ohmvir.plugins.jenkinsaisynapse.api.message;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Objects;

/**
 * A request from the model to call a tool. It is answered by a {@link ToolResultPart} with the same id in the
 * {@link Role#TOOL} message that follows.
 */
public final class ToolCallPart implements ContentPart {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final String id;
    private final String name;
    // Stored as JSON text so the part is immutable and serializes cleanly.
    private final String argumentsJson;

    public ToolCallPart(String id, String name, ObjectNode arguments) {
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
    public ObjectNode getArguments() {
        try {
            return (ObjectNode) MAPPER.readTree(argumentsJson);
        } catch (JsonProcessingException e) {
            // The JSON was written from an ObjectNode by the constructor.
            throw new IllegalStateException("Stored tool arguments are not valid JSON", e);
        }
    }

    public String getArgumentsJson() {
        return argumentsJson;
    }

    @Override
    public String toString() {
        return "ToolCallPart[" + id + ", " + name + ", " + argumentsJson + "]";
    }
}
