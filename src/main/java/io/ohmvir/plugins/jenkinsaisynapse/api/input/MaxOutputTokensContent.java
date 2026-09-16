package io.ohmvir.plugins.jenkinsaisynapse.api.input;

import hudson.Extension;
import io.ohmvir.plugins.jenkinsaisynapse.api.models.ModelCapability;
import io.ohmvir.plugins.jenkinsaisynapse.api.models.ModelInputType;
import java.util.Set;
import lombok.Getter;
import org.jspecify.annotations.NonNull;
import org.kohsuke.stapler.DataBoundConstructor;

public class MaxOutputTokensContent extends ModelInput {
    private @Getter final long maxOutputTokens;

    @DataBoundConstructor
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

        @Override
        public @NonNull String getDisplayName() {
            return "Max Output Tokens Content";
        }
    }
}
