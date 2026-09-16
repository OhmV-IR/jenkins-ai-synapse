package io.ohmvir.plugins.jenkinsaisynapse.api.output;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import hudson.Extension;
import io.ohmvir.plugins.jenkinsaisynapse.api.models.ModelCapability;
import io.ohmvir.plugins.jenkinsaisynapse.api.models.ModelOutputType;
import java.util.Set;
import lombok.Getter;
import org.jspecify.annotations.NonNull;
import org.kohsuke.stapler.DataBoundConstructor;

public class ToolCallContent extends ModelOutput {
    private @Getter final String toolUseId;
    private @Getter final JsonObject toolArguments;
    private @Getter final String name;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    @DataBoundConstructor
    public ToolCallContent(String toolUseId, JsonObject toolArguments, String name) {
        this.toolUseId = toolUseId;
        this.toolArguments = toolArguments;
        this.name = name;
    }

    public String getArgumentsPrettyJson(){
        if(toolArguments == null){
            return "{}";
        }
        return GSON.toJson(toolArguments);
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
