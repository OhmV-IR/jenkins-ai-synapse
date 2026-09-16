package io.ohmvir.plugins.jenkinsaisynapse.api.output;

import hudson.Extension;
import io.ohmvir.plugins.jenkinsaisynapse.api.models.ModelCapability;
import io.ohmvir.plugins.jenkinsaisynapse.api.models.ModelOutputType;
import java.util.Set;
import lombok.Getter;
import org.kohsuke.stapler.DataBoundConstructor;

public class OutputVideoContent extends ModelOutput {
    private @Getter final byte[] videoData;

    @DataBoundConstructor
    public OutputVideoContent(byte[] videoData) {
        this.videoData = videoData;
    }

    @Extension
    public static class DescriptorImpl extends ModelOutputDescriptor {
        @Override
        public Set<ModelCapability> getRequiredCapabilities() {
            return Set.of();
        }

        @Override
        public Set<ModelOutputType> getRequiredOutputTypes() {
            return Set.of(ModelOutputType.VIDEO);
        }
    }
}
