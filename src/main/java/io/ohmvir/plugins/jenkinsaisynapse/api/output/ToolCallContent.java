package io.ohmvir.plugins.jenkinsaisynapse.api.output;

import com.google.gson.JsonObject;
import hudson.Extension;
import io.ohmvir.plugins.jenkinsaisynapse.api.models.ModelCapability;
import io.ohmvir.plugins.jenkinsaisynapse.api.models.ModelOutputType;
import java.util.Map;
import java.util.Set;
import lombok.Getter;
import org.kohsuke.stapler.DataBoundConstructor;

public class ToolCallContent extends ModelOutput {
    private @Getter final String toolUseId;
    private @Getter final JsonObject toolArguments;
    private @Getter final String name;

    @DataBoundConstructor
    public ToolCallContent(String toolUseId, JsonObject toolArguments, String name) {
        this.toolUseId = toolUseId;
        this.toolArguments = toolArguments;
        this.name = name;
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
    }
}
