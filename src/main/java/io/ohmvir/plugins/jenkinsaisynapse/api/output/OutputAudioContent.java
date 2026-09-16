package io.ohmvir.plugins.jenkinsaisynapse.api.output;

import hudson.Extension;
import io.ohmvir.plugins.jenkinsaisynapse.api.models.ModelCapability;
import io.ohmvir.plugins.jenkinsaisynapse.api.models.ModelOutputType;
import java.util.Set;
import lombok.Getter;
import org.kohsuke.stapler.DataBoundConstructor;

public class OutputAudioContent extends ModelOutput {
    private @Getter final byte[] audioData;

    @DataBoundConstructor
    public OutputAudioContent(byte[] audioData) {
        this.audioData = audioData;
    }

    @Extension
    public static final class DescriptorImpl extends ModelOutputDescriptor {
        @Override
        public Set<ModelCapability> getRequiredCapabilities() {
            return Set.of();
        }

        @Override
        public Set<ModelOutputType> getRequiredOutputTypes() {
            return Set.of(ModelOutputType.AUDIO);
        }
    }
}
