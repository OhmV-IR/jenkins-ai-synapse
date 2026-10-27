package dev.ohmvir.plugins.jenkinsaisynapse.api.input;

import dev.ohmvir.plugins.jenkinsaisynapse.api.models.ModelCapability;
import dev.ohmvir.plugins.jenkinsaisynapse.api.models.ModelInputType;
import hudson.Extension;
import java.util.Set;
import lombok.Getter;
import org.jspecify.annotations.NonNull;
import org.kohsuke.stapler.DataBoundConstructor;

public class InfillSuffixContent extends ModelInput {
    private @Getter final String suffix;

    @DataBoundConstructor
    public InfillSuffixContent(String suffix) {
        this.suffix = suffix;
    }

    @Override
    public Set<ModelCapability> getRequiredCapabilities() {
        return Set.of(ModelCapability.INFILL);
    }

    @Override
    public Set<ModelInputType> getRequiredInputTypes() {
        return Set.of(ModelInputType.TEXT);
    }

    @Extension
    public static class DescriptorImpl extends ModelInputDescriptor {
        @Override
        public Set<ModelCapability> getRequiredCapabilities() {
            return Set.of(ModelCapability.INFILL);
        }

        @Override
        public Set<ModelInputType> getRequiredInputTypes() {
            return Set.of(ModelInputType.TEXT);
        }

        @Override
        public @NonNull String getDisplayName() {
            return "Infill Suffix Content";
        }
    }
}
