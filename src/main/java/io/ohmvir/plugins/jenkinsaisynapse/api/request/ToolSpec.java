package io.ohmvir.plugins.jenkinsaisynapse.api.request;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.ohmvir.plugins.jenkinsaisynapse.api.tools.Tool;
import io.ohmvir.plugins.jenkinsaisynapse.api.tools.ToolArgumentDescription;
import java.util.Objects;

/**
 * Describes a tool the model may call: its name, what it does and the JSON schema of its arguments.
 * Instances are immutable.
 */
public final class ToolSpec {
    private final String name;
    private final String description;
    private final String inputSchemaJson;

    /**
     * @param name The tool's name, as the model will call it.
     * @param description What the tool does.
     * @param inputSchema A JSON schema of type {@code object} describing the tool's arguments.
     */
    public ToolSpec(String name, String description, JsonObject inputSchema) {
        this.name = Objects.requireNonNull(name, "name");
        this.description = Objects.requireNonNull(description, "description");
        this.inputSchemaJson =
                Objects.requireNonNull(inputSchema, "inputSchema").toString();
    }

    /**
     * Describes a registered {@link Tool}.
     */
    public static ToolSpec of(Tool tool) {
        JsonObject properties = new JsonObject();
        JsonArray required = new JsonArray();
        for (ToolArgumentDescription argument : tool.getArguments()) {
            JsonObject property = new JsonObject();
            property.addProperty("type", jsonType(argument.getType()));
            property.addProperty("description", argument.getDescription());
            properties.add(argument.getName(), property);
            if (argument.isRequired()) {
                required.add(argument.getName());
            }
        }
        JsonObject schema = new JsonObject();
        schema.addProperty("type", "object");
        schema.add("properties", properties);
        schema.add("required", required);
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
    public JsonObject getInputSchema() {
        return JsonParser.parseString(inputSchemaJson).getAsJsonObject();
    }

    public String getInputSchemaJson() {
        return inputSchemaJson;
    }
}
