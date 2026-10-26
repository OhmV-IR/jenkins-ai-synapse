package dev.ohmvir.plugins.jenkinsaisynapse.api.output;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.ohmvir.plugins.jenkinsaisynapse.api.models.ModelCapability;
import dev.ohmvir.plugins.jenkinsaisynapse.api.models.ModelOutputType;
import hudson.Extension;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import lombok.Getter;
import org.jspecify.annotations.NonNull;
import org.kohsuke.stapler.DataBoundConstructor;

public class ToolCallContent extends ModelOutput {
    private @Getter final String toolUseId;
    private @Getter final ObjectNode toolArguments;
    private @Getter final String name;
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @DataBoundConstructor
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

    @Extension
    public static class DescriptorImpl extends ModelOutputDescriptor {
        @Override
        public Set<ModelCapability> getRequiredCapabilities() {
            return Set.of(ModelCapability.TOOLS);
        }

        @Override
        public Set<ModelOutputType> getRequiredOutputTypes() {
            return Set.of();
        }

        @Override
        public @NonNull String getDisplayName() {
            return "Tool Call Content";
        }
    }
}
