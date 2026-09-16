package io.ohmvir.plugins.jenkinsaisynapse.api.input;

import hudson.Extension;
import io.ohmvir.plugins.jenkinsaisynapse.api.models.ModelCapability;
import io.ohmvir.plugins.jenkinsaisynapse.api.models.ModelInputType;
import java.util.Set;
import lombok.Getter;
import org.kohsuke.stapler.DataBoundConstructor;

public class SystemPromptContent extends ModelInput {
    private @Getter final String systemPrompt;

    @DataBoundConstructor
    public SystemPromptContent(String systemPrompt) {
        this.systemPrompt = systemPrompt;
    }

    @Extension
    public static class DescriptorImpl extends ModelInputDescriptor {
        @Override
        public Set<ModelCapability> getRequiredCapabilities() {
            return Set.of(ModelCapability.ADJUSTABLE_SYSTEM_PROMPT);
        }

        @Override
        public Set<ModelInputType> getRequiredInputTypes() {
            return Set.of();
        }
    }
}
