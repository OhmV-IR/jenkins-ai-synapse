package io.ohmvir.plugins.jenkinsaisynapse.api.output;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.ohmvir.plugins.jenkinsaisynapse.api.models.ModelCapability;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import lombok.Getter;

public class ToolCallContent extends ModelOutput {
    public static final ModelOutputKind<ToolCallContent> KIND =
            new ModelOutputKind<>(ToolCallContent.class, Set.of(ModelCapability.TOOLS), Set.of());

    private @Getter final String toolUseId;
    private @Getter final ObjectNode toolArguments;
    private @Getter final String name;
    private static final ObjectMapper MAPPER = new ObjectMapper();

    public ToolCallContent(String toolUseId, ObjectNode toolArguments, String name) {
        this.toolUseId = toolUseId;
        this.toolArguments = toolArguments;
        this.name = name;
    }

    public String getArgumentsPrettyJson() {
        if (toolArguments == null) {
            return "{}";
        }
        try {
            return MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(toolArguments);
        } catch (JsonProcessingException e) {
            Logger.getLogger(ToolCallContent.class.getName())
                    .log(Level.WARNING, "Failed to serialize tool arguments to JSON", e);
            return "{}";
        }
    }

    @Override
    public ModelOutputKind<ToolCallContent> getKind() {
        return KIND;
    }
}
