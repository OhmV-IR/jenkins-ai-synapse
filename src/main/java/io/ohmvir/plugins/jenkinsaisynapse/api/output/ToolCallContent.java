package io.ohmvir.plugins.jenkinsaisynapse.api.output;

import hudson.Extension;
import io.ohmvir.plugins.jenkinsaisynapse.api.models.ModelCapability;
import io.ohmvir.plugins.jenkinsaisynapse.api.models.ModelOutputType;

import java.util.Map;
import java.util.Set;

import lombok.Getter;

public class ToolCallContent extends ModelOutput {
    private @Getter final String toolUseId;
    private @Getter final Map<String, String> toolArguments;
    private @Getter final String name;

    public ToolCallContent(String toolUseId, Map<String, String> toolArguments, String name) {
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
