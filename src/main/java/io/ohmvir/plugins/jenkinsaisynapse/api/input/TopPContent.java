package io.ohmvir.plugins.jenkinsaisynapse.api.input;

import hudson.Extension;
import io.ohmvir.plugins.jenkinsaisynapse.api.models.ModelCapability;
import io.ohmvir.plugins.jenkinsaisynapse.api.models.ModelInputType;
import java.util.Set;
import lombok.Getter;

public class TopPContent extends ModelInput {
    private @Getter final double topP;

    public TopPContent(double topP) {
        this.topP = topP;
    }

    @Extension
    public static class DescriptorImpl extends ModelInputDescriptor {
        @Override
        public Set<ModelCapability> getRequiredCapabilities() {
            return Set.of(ModelCapability.CUSTOM_TOP_P);
        }

        @Override
        public Set<ModelInputType> getRequiredInputTypes() {
            return Set.of();
        }
    }
}
