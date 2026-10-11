package dev.ohmvir.plugins.jenkinsaisynapse.api.output;

import dev.ohmvir.plugins.jenkinsaisynapse.api.models.ModelCapability;
import dev.ohmvir.plugins.jenkinsaisynapse.api.models.ModelOutputType;
import hudson.Extension;
import java.util.Set;
import lombok.Getter;
import org.jspecify.annotations.NonNull;
import org.kohsuke.stapler.DataBoundConstructor;

public class ThinkingContent extends ModelOutput {
    private @Getter final String thinking;

    @DataBoundConstructor
    public ThinkingContent(String thinking) {
        this.thinking = thinking;
    }

    @Extension
    public static class DescriptorImpl extends ModelOutputDescriptor {
        @Override
        public Set<ModelCapability> getRequiredCapabilities() {
            return Set.of(ModelCapability.THINKING);
        }

        @Override
        public Set<ModelOutputType> getRequiredOutputTypes() {
            return Set.of();
        }

        @Override
        public @NonNull String getDisplayName() {
            return "Thinking Content";
        }
    }
}
