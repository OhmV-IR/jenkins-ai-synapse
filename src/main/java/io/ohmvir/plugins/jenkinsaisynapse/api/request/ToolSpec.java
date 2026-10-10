package io.ohmvir.plugins.jenkinsaisynapse.api.request;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.ohmvir.plugins.jenkinsaisynapse.api.tools.Tool;
import io.ohmvir.plugins.jenkinsaisynapse.api.tools.ToolArgumentDescription;
import java.util.Objects;

/**
 * Describes a tool the model may call: its name, what it does and the JSON schema of its arguments.
 * Instances are immutable.
 */
public final class ToolSpec {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final String name;
    private final String description;
    private final String inputSchemaJson;

    /**
     * @param name The tool's name, as the model will call it.
     * @param description What the tool does.
     * @param inputSchema A JSON schema of type {@code object} describing the tool's arguments.
     */
    public ToolSpec(String name, String description, ObjectNode inputSchema) {
        this.name = Objects.requireNonNull(name, "name");
        this.description = Objects.requireNonNull(description, "description");
        this.inputSchemaJson =
                Objects.requireNonNull(inputSchema, "inputSchema").toString();
    }

    /**
     * Describes a registered {@link Tool}.
     */
    public static ToolSpec of(Tool tool) {
        ObjectNode schema = MAPPER.createObjectNode();
        schema.put("type", "object");
        ObjectNode properties = schema.putObject("properties");
        ArrayNode required = schema.putArray("required");
        for (ToolArgumentDescription argument : tool.getArguments()) {
            ObjectNode property = properties.putObject(argument.getName());
            property.put("type", jsonType(argument.getType()));
            property.put("description", argument.getDescription());
            if (argument.isRequired()) {
                required.add(argument.getName());
            }
        }
        return new ToolSpec(tool.getName(), tool.getDescription(), schema);
    }

    private static String jsonType(Class<?> type) {
        if (type == int.class || type == long.class || type == Integer.class || type == Long.class) {
            return "integer";
        }
        if (type == double.class || type == float.class || type == Double.class || type == Float.class) {
            return "number";
        }
        if (type == boolean.class || type == Boolean.class) {
            return "boolean";
        }
        return "string";
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    /**
     * @return A copy of the JSON schema of the tool's arguments.
     */
    public ObjectNode getInputSchema() {
        try {
            return (ObjectNode) MAPPER.readTree(inputSchemaJson);
        } catch (JsonProcessingException e) {
            // The JSON was written from an ObjectNode by the constructor.
            throw new IllegalStateException("Stored input schema is not valid JSON", e);
        }
    }

    public String getInputSchemaJson() {
        return inputSchemaJson;
    }
}
