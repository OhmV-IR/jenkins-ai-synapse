package io.ohmvir.plugins.jenkinsaisynapse.api.input;

import hudson.Extension;
import io.ohmvir.plugins.jenkinsaisynapse.api.models.ModelCapability;
import io.ohmvir.plugins.jenkinsaisynapse.api.models.ModelInputType;
import lombok.Getter;

import java.util.Set;

public class MaxOutputTokensContent extends ModelInput {
    private @Getter final long maxOutputTokens;

    public MaxOutputTokensContent(long maxOutputTokens) {
        this.maxOutputTokens = maxOutputTokens;
    }

    @Extension
    public static class DescriptorImpl extends ModelInputDescriptor {
        @Override
        public Set<ModelCapability> getRequiredCapabilities() {
            return Set.of(ModelCapability.OUTPUT_TOKEN_LIMITING);
        }

        @Override
        public Set<ModelInputType> getRequiredInputTypes() {
            return Set.of();
        }
    }
}
