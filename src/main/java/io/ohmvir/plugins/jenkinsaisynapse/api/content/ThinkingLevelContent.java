package io.ohmvir.plugins.jenkinsaisynapse.api.content;

import hudson.Extension;
import io.ohmvir.plugins.jenkinsaisynapse.api.input.ModelInput;
import io.ohmvir.plugins.jenkinsaisynapse.api.input.ModelInputDescriptor;
import io.ohmvir.plugins.jenkinsaisynapse.api.models.ModelCapability;
import io.ohmvir.plugins.jenkinsaisynapse.api.models.ModelInputType;
import io.ohmvir.plugins.jenkinsaisynapse.api.models.ModelThinkingLevel;
import lombok.Getter;

import java.util.Set;

public class ThinkingLevelContent extends ModelInput {
    private @Getter final ModelThinkingLevel thinkingLevel;

    public ThinkingLevelContent(ModelThinkingLevel thinkingLevel) {
        this.thinkingLevel = thinkingLevel;
    }

    @Extension
    public static class DescriptorImpl extends ModelInputDescriptor {
        @Override
        public Set<ModelCapability> getRequiredCapabilities() {
            return Set.of(ModelCapability.THINKING);
        }

        @Override
        public Set<ModelInputType> getRequiredInputTypes() {
            return Set.of();
        }
    }
}
