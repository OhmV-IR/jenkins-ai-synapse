package io.ohmvir.plugins.jenkinsaisynapse.api.content;

import hudson.Extension;
import io.ohmvir.plugins.jenkinsaisynapse.api.input.ModelInput;
import io.ohmvir.plugins.jenkinsaisynapse.api.input.ModelInputDescriptor;
import io.ohmvir.plugins.jenkinsaisynapse.api.models.ModelCapability;
import io.ohmvir.plugins.jenkinsaisynapse.api.models.ModelInputType;
import lombok.Getter;

import java.util.Set;

public class ToolCallResponseContent extends ModelInput {
    private @Getter final String toolUseId;
    private @Getter final boolean successful;
    private @Getter final String responseContent;

    public ToolCallResponseContent(String toolUseId, boolean successful, String responseContent) {
        this.toolUseId = toolUseId;
        this.successful = successful;
        this.responseContent = responseContent;
    }

    @Extension
    public static class DescriptorImpl extends ModelInputDescriptor {
        @Override
        public Set<ModelCapability> getRequiredCapabilities() {
            return Set.of(ModelCapability.TOOLS);
        }

        @Override
        public Set<ModelInputType> getRequiredInputTypes() {
            return Set.of(ModelInputType.TEXT);
        }
    }
}
